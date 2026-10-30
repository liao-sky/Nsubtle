package com.nsubtle.entity;

import com.nsubtle.events.RegistryEvents;
import com.nsubtle.mixin.SaplingBlockAccessor;
import com.nsubtle.util.TreeBoomHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * 树苗投掷物：飞行时拖一条白色粒子尾迹，落地后在命中点附近种下对应树苗并立刻催熟。
 * <p>
 * 催熟走的是原版 {@link SaplingBlock#performBonemeal} 路径，因此 {@code TreeBoomMixin}
 * 记录到“本次是骨粉催熟”并照常触发树爆——不需要在这里重复写一遍爆炸逻辑。
 * 如果落地位置不适合种树（水里、半空、没有泥土支撑），则退化为一次空爆。
 */
public class SaplingProjectile extends ThrowableItemProjectile {

    /** 命中实体时的直接伤害；会按弓的力量附魔在生成时上调。 */
    private float impactDamage = 4.0F;

    /** 长树失败时是否把树苗掉回地上。多重射击时只有第一颗负责退还，避免刷树苗。 */
    private boolean refundOnFail = true;

    /** 发射它的武器（用来结算冲击附魔）。 */
    private ItemStack weapon = ItemStack.EMPTY;

    /** 穿透等级；>0 时可以继续穿过实体。 */
    private int pierceLevel;

    /** 已经穿过的实体，避免同一只被反复命中。 */
    private final Set<Integer> pierced = new HashSet<>();

    /** 是否已经退还过树苗（穿透时会多次结算，只退一次）。 */
    private boolean refunded;

    public void setImpactDamage(float damage) {
        this.impactDamage = damage;
    }

    public void setRefundOnFail(boolean refundOnFail) {
        this.refundOnFail = refundOnFail;
    }

    public void setWeapon(ItemStack weapon) {
        this.weapon = weapon;
    }

    public void setPierceLevel(int pierceLevel) {
        this.pierceLevel = pierceLevel;
    }

    /** 白色尾迹的尘埃颜色。 */
    private static final int TRAIL_COLOR = 0xFFFFFF;

    public SaplingProjectile(EntityType<? extends SaplingProjectile> type, Level level) {
        super(type, level);
    }

    public SaplingProjectile(Level level, LivingEntity owner, ItemStack sapling) {
        super(RegistryEvents.EntityRegistry.SAPLING_PROJECTILE.get(), owner, level, sapling);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.OAK_SAPLING;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.spawnTrail();
        }
    }

    /**
     * 白色尾迹：白色烟、末地烛光点、以及少量白色尘埃。
     * 只在客户端生成，服务端不会因为粒子而发包。
     */
    private void spawnTrail() {
        Level level = this.level();
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.0D, 0.0D);

        if (this.tickCount % 2 == 0) {
            level.addParticle(new DustParticleOptions(TRAIL_COLOR, 0.6F), x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel serverLevel) || !(result.getEntity() instanceof LivingEntity target)) {
            return;
        }
        DamageSource source = this.damageSources().thrown(this, this.getOwner());
        // 伤害走原版结算：本实体在 #minecraft:arrows 标签里，所以力量等附魔的
        // direct_attacker 条件能通过，数值也完全由附魔数据决定。
        float damage = EnchantmentHelper.modifyDamage(serverLevel, this.weapon, target, source, this.impactDamage);
        target.hurtServer(serverLevel, source, damage);
        this.applyKnockback(serverLevel, target, source);
    }

    /**
     * 冲击附魔：和原版箭一样沿水平飞行方向推开目标，并考虑目标的击退抗性。
     */
    private void applyKnockback(ServerLevel level, LivingEntity target, DamageSource source) {
        if (this.weapon.isEmpty()) {
            return;
        }
        float knockback = EnchantmentHelper.modifyKnockback(level, this.weapon, target, source, 0.0F);
        if (knockback <= 0.0F) {
            return;
        }
        double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        Vec3 push = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * 0.6 * resistance);
        if (push.lengthSqr() > 0.0) {
            target.push(push.x, 0.1, push.z);
        }
    }

    @Override
    public boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !this.pierced.contains(target.getId());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 location = result.getLocation();
        burst(serverLevel, location.x, location.y, location.z);

        if (result instanceof EntityHitResult entityHit) {
            // 打中实体时在实体**脚下**长树，而不是在命中点（可能是身体中段）生成半空的树。
            this.impact(serverLevel, entityHit.getEntity().blockPosition());

            // 穿透：还能继续穿就记下这只实体，让投掷物接着飞。
            this.pierced.add(entityHit.getEntity().getId());
            if (this.pierceLevel > 0 && this.pierced.size() <= this.pierceLevel) {
                return;
            }
            this.discard();
            return;
        }

        this.impact(serverLevel, BlockPos.containing(location));
        this.discard();
    }

    /**
     * 落点处理：直接在落点上方长树；长不成就把树苗掉出去（一颗炸弹只退一颗），不爆炸。
     */
    private void impact(ServerLevel level, BlockPos hitPos) {
        ItemStack payload = this.getItem();
        Block block = Block.byItem(payload.getItem());
        BlockPos spot = findPlantSpot(level, hitPos);

        if (block instanceof SaplingBlock sapling && spot != null) {
            TreeGrower grower = ((SaplingBlockAccessor) sapling).nsubtle$treeGrower();

            // 先按单棵长。growTree 内部会先把目标位置清成空气再放树。
            if (grow(level, spot, sapling, grower)) {
                return;
            }

            // 单棵失败时 growTree 会把树苗还原在原地，正好用来凑 2×2：
            // 深色橡木这类只认 2×2 的树种也能长。
            if (growTwoByTwo(level, spot, sapling, grower)) {
                return;
            }

            clearSaplings(level, spot, sapling);
        }

        // 长不出树就不炸，把树苗还回去（多重射击/穿透时也只退一颗）。
        if (this.refundOnFail && !this.refunded) {
            this.refunded = true;
            dropPayload(level, hitPos, payload);
        }
    }

    /**
     * 找一个可以长树的空位：命中点本身可替换就用它，否则用它正上方那一格。
     * <p>
     * 只认“完整、非含水”的支撑方块：水面、含水方块、台阶/楼梯/栅栏这类不完整方块都会被拒绝。
     */
    private static BlockPos findPlantSpot(ServerLevel level, BlockPos hitPos) {
        BlockPos[] candidates = {hitPos, hitPos.above()};
        for (BlockPos pos : candidates) {
            if (!canGrowInto(level, pos)) {
                continue;
            }
            if (!canGrowOn(level, pos)) {
                continue;
            }
            return pos;
        }
        return null;
    }

    /** 目标格必须可替换、且本身不含液体（排除水面、含水方块）。 */
    private static boolean canGrowInto(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    /** 下方的支撑方块必须是完整方块、且不含液体（排除含水方块、台阶、楼梯、栅栏等）。 */
    private static boolean canGrowOn(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState support = level.getBlockState(below);
        if (support.isAir() || !support.getFluidState().isEmpty()) {
            return false;
        }
        return support.isCollisionShapeFullBlock(level, below);
    }

    /**
     * 在指定位置长一次树。前后手动建立“本次是骨粉催熟”的上下文，
     * 成功时 {@code TreeBoomMixin} 会照常触发树爆。
     */
    private static boolean grow(ServerLevel level, BlockPos pos, SaplingBlock sapling, TreeGrower grower) {
        TreeBoomHelper.beginBonemeal(level, pos);
        boolean grown = grower.growTree(level, level.getChunkSource().getGenerator(), pos,
                sapling.defaultBlockState(), level.getRandom());
        TreeBoomHelper.endBonemeal();
        return grown;
    }

    /**
     * 临时补齐 2×2 的同种树苗再长一次，照顾深色橡木这种只认 2×2 的树种。
     * 长成时树会把 4 个树苗一起替换掉；失败则把临时方块清干净。
     */
    private static boolean growTwoByTwo(ServerLevel level, BlockPos spot, SaplingBlock sapling, TreeGrower grower) {
        BlockPos east = spot.east();
        BlockPos south = spot.south();
        BlockPos southEast = east.south();
        if (!canReplace(level, east) || !canReplace(level, south) || !canReplace(level, southEast)) {
            return false;
        }

        BlockState state = sapling.defaultBlockState();
        level.setBlock(east, state, Block.UPDATE_CLIENTS);
        level.setBlock(south, state, Block.UPDATE_CLIENTS);
        level.setBlock(southEast, state, Block.UPDATE_CLIENTS);

        if (grow(level, spot, sapling, grower)) {
            return true;
        }

        clearSaplings(level, spot, sapling);
        return false;
    }

    private static boolean canReplace(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced();
    }

    /** 清掉长树失败后残留的树苗方块（包括 growTree 还原的那一株）。 */
    private static void clearSaplings(ServerLevel level, BlockPos spot, SaplingBlock sapling) {
        BlockPos[] positions = {spot, spot.east(), spot.south(), spot.east().south()};
        for (BlockPos pos : positions) {
            if (level.getBlockState(pos).is(sapling)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static void dropPayload(ServerLevel level, BlockPos pos, ItemStack payload) {
        Block.popResource(level, pos, payload.copyWithCount(1));
    }

    private static void burst(ServerLevel level, double x, double y, double z) {
        ParticleOptions smoke = ParticleTypes.WHITE_SMOKE;
        level.sendParticles(smoke, x, y, z, 16, 0.3D, 0.3D, 0.3D, 0.02D);
    }
}
