package com.nsubtle.util;

import com.nsubtle.Nsubtle;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 骨粉树爆的实际逻辑与“本次催熟”上下文。
 * <p>
 * 放在普通工具类里而不是 Mixin 类里，避免注入后的代码访问 Mixin 类的私有成员
 * （注入代码运行在目标类中，跨类的私有访问会在运行时失败）。
 */
public final class TreeBoomHelper {

    private static final ThreadLocal<BonemealContext> PENDING_BONEMEAL = new ThreadLocal<>();

    /** 伤害半径（格）。 */
    private static final double DAMAGE_RADIUS = 1.6D;
    /** 击退半径（格）：比伤害范围更大，用来表现向外扩散的冲击波。 */
    private static final double KNOCKBACK_RADIUS = 2.4D;
    /** 边缘伤害保留比例：中心 100%，伤害半径边缘 50%。 */
    private static final double DAMAGE_MIN_RATIO = 0.5D;
    /** 边缘击退保留比例：中心 100%，击退半径边缘 35%。 */
    private static final double KNOCKBACK_MIN_RATIO = 0.35D;
    /** 中心水平冲量：负责“炸飞”的横向位移。 */
    private static final double MAX_KNOCKBACK = 3.2D;
    /** 中心纵向冲量：约 6 格抬升，足够明显，但不会失控到 15 格以上。 */
    private static final double Y_KNOCKBACK = 1.1D;
    /** 摔落减免：把坠落距离预置为负值，相当于前 2 格不计入摔落伤害。 */
    private static final double FALL_DAMAGE_DISCOUNT = 2.0D;

    private record BonemealContext(ServerLevel level, BlockPos pos) {
    }

    private TreeBoomHelper() {
    }

    /** 骨粉催熟开始，记录上下文。 */
    public static void beginBonemeal(ServerLevel level, BlockPos pos) {
        PENDING_BONEMEAL.set(new BonemealContext(level, pos));
    }

    /** 骨粉催熟结束，清理上下文。 */
    public static void endBonemeal() {
        PENDING_BONEMEAL.remove();
    }

    /**
     * 树长成后调用。只有当前线程存在催熟上下文、且确实生长成功时才爆炸。
     * 无论是否爆炸都会消费掉上下文，避免异常路径残留。
     */
    public static void onTreeGrown(ServerLevel level, BlockPos pos, BlockState state, boolean success) {
        BonemealContext context = PENDING_BONEMEAL.get();
        PENDING_BONEMEAL.remove();
        if (context == null || !success) {
            return;
        }
        explode(level, pos, state.getBlock());
    }

    private static void explode(ServerLevel level, BlockPos pos, Block sapling) {
        float damage = damageFor(sapling);
        DamageSource source = level.damageSources().explosion(null, null);

        AABB area = new AABB(pos).inflate(KNOCKBACK_RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            double dx = entity.getX() - (pos.getX() + 0.5D);
            double dy = entity.getY() + entity.getEyeHeight() - (pos.getY() + 0.5D);
            double dz = entity.getZ() - (pos.getZ() + 0.5D);
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // 伤害只在 1.6 格内结算：平方衰减，越靠近边缘掉得越快。
            if (distance <= DAMAGE_RADIUS) {
                double normalizedDamage = distance / DAMAGE_RADIUS;
                double damageFactor = 1.0D - (1.0D - DAMAGE_MIN_RATIO) * normalizedDamage * normalizedDamage;
                entity.hurtServer(level, source, (float) (damage * damageFactor));
                if (!entity.isAlive()) {
                    continue;
                }
            }

            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            double dirX;
            double dirZ;
            if (horizontalDistance > 0.001D) {
                dirX = dx / horizontalDistance;
                dirZ = dz / horizontalDistance;
            } else {
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                dirX = Math.cos(angle);
                dirZ = Math.sin(angle);
            }

            // 爆炸发生在树长成之后：实体可能正好被树干/树枝卡住，先挪出碰撞体，
            // 否则 setDeltaMovement 会被方块碰撞直接吃掉，表现为“没有击退”。
            moveOutOfBlocks(level, entity, dirX, dirZ);

            // 击退在 2.4 格内都生效：同样是平方衰减，边缘保留 35% 的推力。
            double normalizedKnockback = Math.min(distance, KNOCKBACK_RADIUS) / KNOCKBACK_RADIUS;
            double knockbackFactor = 1.0D - (1.0D - KNOCKBACK_MIN_RATIO)
                    * normalizedKnockback * normalizedKnockback;
            double strength = MAX_KNOCKBACK * knockbackFactor;

            // 预置负的坠落距离：既清掉爆炸前已经累积的下落，又给落地减免约 2 格伤害。
            entity.fallDistance = -FALL_DAMAGE_DISCOUNT;
            entity.setDeltaMovement(dirX * strength, Y_KNOCKBACK * knockbackFactor, dirZ * strength);
            entity.hurtMarked = true;
        }

        awardTreeBoomAdvancement(level, pos);
        spawnParticles(level, pos);
    }

    /**
     * 首次炸树时给最近的玩家授予成就。
     * <p>
     * 骨粉催熟入口（{@code performBonemeal}）拿不到玩家参数，这里用“爆心 8 格内最近的玩家”
     * 作为触发者；正常使用骨粉的距离（约 4.5 格）一定覆盖在内。
     */
    private static void awardTreeBoomAdvancement(ServerLevel level, BlockPos pos) {
        ServerPlayer nearest = null;
        double nearestDistance = 8.0D * 8.0D;
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D);
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        if (nearest == null) {
            return;
        }

        AdvancementHolder advancement = level.getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(Nsubtle.MOD_ID, "tree_boom"));
        if (advancement != null) {
            nearest.getAdvancements().award(advancement, "trigger");
        }
    }

    /**
     * 如果实体当前与方块碰撞（例如被长出的树干包住），把它移到最近的空位。
     * 优先沿击退方向水平挪动，其次向上抬升。
     */
    private static void moveOutOfBlocks(ServerLevel level, LivingEntity entity, double dirX, double dirZ) {
        if (level.noCollision(entity)) {
            return;
        }

        AABB box = entity.getBoundingBox();
        for (double distance = 1.0D; distance <= 2.0D; distance += 1.0D) {
            for (double up = 0.0D; up <= 1.0D; up += 1.0D) {
                double x = entity.getX() + dirX * distance;
                double y = entity.getY() + up;
                double z = entity.getZ() + dirZ * distance;
                if (level.noCollision(entity, box.move(x - entity.getX(), y - entity.getY(), z - entity.getZ()))) {
                    entity.teleportTo(x, y, z);
                    return;
                }
            }
        }

        // 兜底只抬高 1~2 格：既够脱离树干，又不会额外叠加太多坠落高度。
        for (double up = 1.0D; up <= 2.0D; up += 1.0D) {
            if (level.noCollision(entity, box.move(0.0D, up, 0.0D))) {
                entity.teleportTo(entity.getX(), entity.getY() + up, entity.getZ());
                return;
            }
        }
    }

    private static float damageFor(Block sapling) {
        if (sapling == Blocks.OAK_SAPLING) {
            return 12.0F;
        }
        if (sapling == Blocks.SPRUCE_SAPLING) {
            return 16.0F;
        }
        if (sapling == Blocks.BIRCH_SAPLING) {
            return 16.0F;
        }
        if (sapling == Blocks.JUNGLE_SAPLING) {
            return 16.0F;
        }
        if (sapling == Blocks.ACACIA_SAPLING) {
            return 10.0F;
        }
        if (sapling == Blocks.DARK_OAK_SAPLING) {
            return 14.0F;
        }
        if (sapling == Blocks.MANGROVE_PROPAGULE) {
            return 12.0F;
        }
        if (sapling == Blocks.CHERRY_SAPLING) {
            return 8.0F;
        }
        return 8.0F;
    }

    private static void spawnParticles(ServerLevel level, BlockPos pos) {
        final double spread = KNOCKBACK_RADIUS;
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        // 不使用原版的 EXPLOSION / EXPLOSION_EMITTER 贴图，只保留自定义的烟雾、火花与彩色尘埃。
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 20, spread, 1.0D, spread, 0.05D);
        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 60, spread, 1.2D, spread, 0.15D);

        for (int i = 0; i < 100; i++) {
            double dx = (level.random.nextDouble() - 0.5D) * 2.0D * spread;
            double dz = (level.random.nextDouble() - 0.5D) * 2.0D * spread;
            double dy = level.random.nextDouble() * spread;

            level.sendParticles(new DustParticleOptions(randomColor(level), randomSize(level)),
                    x + dx, y + dy, z + dz, 0, 0.0D, 0.0D, 0.0D, 1.0D);
        }

        // 少量"飞得很远"的彩色粒子：用 count=0 + 方向向量 + 速度，直接给粒子一个远距离初速度。
        for (int i = 0; i < 12; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);
            double dirY = 0.15D + level.random.nextDouble() * 0.35D;
            double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            double speed = 1.8D + level.random.nextDouble() * 1.4D;

            level.sendParticles(new DustParticleOptions(randomColor(level), randomSize(level)),
                    x, y + 0.2D, z, 0,
                    dirX / length, dirY / length, dirZ / length, speed);
        }
    }

    private static int randomColor(ServerLevel level) {
        return (level.random.nextInt(256) << 16)
                | (level.random.nextInt(256) << 8)
                | level.random.nextInt(256);
    }

    private static float randomSize(ServerLevel level) {
        return 0.6F + level.random.nextFloat();
    }
}
