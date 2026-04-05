package com.nsubtle.mixin;

import com.nsubtle.Nsubtle;
import com.nsubtle.util.MarkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;


public class TreeBoomMixin {

    @Mixin(SaplingBlock.class)
    public static class SaplingBlockMixin {

        @Inject(method = "performBonemeal", at = @At("HEAD"))
        private void onTreeGrowthFromBoneMeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
            MarkUtils.GrowthContext.markAsBonemeal();
        }

        @Inject(
                method = "advanceTree",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/world/level/block/grower/TreeGrower;growTree(Lnet/minecraft/server/level/ServerLevel;" +
                                "Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/core/BlockPos;" +
                                "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Z"
                )
        )
        private void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, CallbackInfo ci){
            if (MarkUtils.GrowthContext.isBonemealGrowth()){
                Nsubtle.LOGGER.info("Growing tree boom...");
                if (level.isClientSide()) return;
                float damage = nsubtle$getDamageBySapling(state.getBlock());
                nsubtle$applyExplosionKnockback(level, pos, damage*2);
                MarkUtils.GrowthContext.clear();
            }
        }

        @Unique
        private float nsubtle$getDamageBySapling(Block block) {
            //this is not true,refer to "nsubtle$applyExplosionKnockback(level, pos, damage*2);" The "damage*2"
            if (block == Blocks.OAK_SAPLING) {
                return 6.0F;
            } else if (block == Blocks.SPRUCE_SAPLING) {
                return 4.0F;
            } else if (block == Blocks.BIRCH_SAPLING) {
                return 3.0F;
            } else if (block == Blocks.JUNGLE_SAPLING) {
                return 8.0F;
            } else if (block == Blocks.ACACIA_SAPLING) {
                return 5.0F;
            } else if (block == Blocks.DARK_OAK_SAPLING) {
                return 7.0F;
            } else if (block == Blocks.MANGROVE_PROPAGULE) {
                return 6.0F;
            } else if (block == Blocks.CHERRY_SAPLING) {
                return 2.0F;
            } else {
                return 4.0F;
            }
        }

        @Unique
        private void nsubtle$applyExplosionKnockback(ServerLevel level, BlockPos pos, float damage) {
            final double RADIUS = 1.2D;
            final double DAMAGE_MIN_RATIO = 0.8D;
            final double KNOCKBACK_MIN_RATIO = 0.25D;
            final double MAX_KNOCKBACK = 1.5D;
            final double Y_KNOCKBACK = 1.0D;

            AABB area = new AABB(pos).inflate(RADIUS);
            List<Entity> entities = level.getEntitiesOfClass(Entity.class, area, Entity::isAlive);
            DamageSource explosionDamage = level.damageSources().explosion(null,null);

            for (Entity entity : entities) {
                double dx = entity.getX() - (pos.getX() + 0.5);
                double dy = entity.getY() + entity.getEyeHeight() - (pos.getY() + 0.5);
                double dz = entity.getZ() - (pos.getZ() + 0.5);
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                // 限制最大距离为 RADIUS
                distance = Math.min(distance, RADIUS);

                // 1. 伤害计算：线性衰减，最远处保留 DAMAGE_MIN_RATIO
                double damageFactor = 1.0 - (1.0 - DAMAGE_MIN_RATIO) * (distance / RADIUS);
                float finalDamage = (float) (damage * damageFactor);
                entity.hurtServer(level, explosionDamage, finalDamage);

                // 2. 击退计算：水平方向衰减，最远处保留 KNOCKBACK_MIN_RATIO
                if (entity instanceof LivingEntity) {
                    double hdx = entity.getX() - (pos.getX() + 0.5);
                    double hdz = entity.getZ() - (pos.getZ() + 0.5);
                    double horizDistance = Math.sqrt(hdx * hdx + hdz * hdz);
                    if (horizDistance > 0.001) {
                        double knockbackFactor = 1.0 - (1.0 - KNOCKBACK_MIN_RATIO) * (horizDistance / RADIUS);
                        double strength = MAX_KNOCKBACK * knockbackFactor;
                        double motionX = hdx / horizDistance * strength;
                        double motionZ = hdz / horizDistance * strength;
                        entity.setDeltaMovement(motionX, Y_KNOCKBACK, motionZ);
                    } else {
                        // 完全重合时随机方向，强度取中心值
                        entity.setDeltaMovement((Math.random() - 0.5) * 2 * MAX_KNOCKBACK, Y_KNOCKBACK, (Math.random() - 0.5) * 2 * MAX_KNOCKBACK);
                    }
                    entity.hurtMarked = true;
                }
            }

            // 视觉效果
            level.explode(null, pos.getX(), pos.getY(), pos.getZ(), 0.0F, Level.ExplosionInteraction.NONE);
        }
    }
}
