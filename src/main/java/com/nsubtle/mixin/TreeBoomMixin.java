package com.nsubtle.mixin;

import com.nsubtle.Nsubtle;
import com.nsubtle.util.MarkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
            MarkUtils.BoneMealMarkHelper.mark(pos);
        }

        @Inject(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SaplingBlock;" +
                "advanceTree(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;" +
                "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V"))
        private void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
            MarkUtils.BoneMealMarkHelper.clear(pos);
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
            if (MarkUtils.BoneMealMarkHelper.isMarked(pos)){
                Nsubtle.LOGGER.info("Growing tree boom...");
                if (level.isClientSide()) return;
                float damage = nsubtle$getDamageBySapling(state.getBlock());
                nsubtle$applyExplosionKnockback(level, pos, damage*2);
                MarkUtils.BoneMealMarkHelper.clear(pos);
            }
        }

        @Unique
        private float nsubtle$getDamageBySapling(Block block) {
            if (block == Blocks.OAK_SAPLING) {
                return 12.0F;
            } else if (block == Blocks.SPRUCE_SAPLING) {
                return 16.0F;
            } else if (block == Blocks.BIRCH_SAPLING) {
                return 16.0F;
            } else if (block == Blocks.JUNGLE_SAPLING) {
                return 16.0F;
            } else if (block == Blocks.ACACIA_SAPLING) {
                return 10.0F;
            } else if (block == Blocks.DARK_OAK_SAPLING) {
                return 14.0F;
            } else if (block == Blocks.MANGROVE_PROPAGULE) {
                return 12.0F;
            } else if (block == Blocks.CHERRY_SAPLING) {
                return 8.0F;
            } else {
                return 8.0F;
            }
        }

        @Unique
        private void nsubtle$applyExplosionKnockback(ServerLevel level, BlockPos pos, float damage) {
            final double RADIUS = 1.2D;
            final double DAMAGE_MIN_RATIO = 0.8D;
            final double KNOCKBACK_MIN_RATIO = 0.9D;
            final double MAX_KNOCKBACK = 2.5D;
            final double Y_KNOCKBACK = 1.5D;

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
            double centerX = pos.getX() + 0.5;
            double centerY = pos.getY() + 0.5;
            double centerZ = pos.getZ() + 0.5;

            // 1. 核心爆炸（大范围）
            level.sendParticles(ParticleTypes.EXPLOSION, centerX, centerY, centerZ, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centerX, centerY, centerZ, 1, 0.0, 0.0, 0.0, 0.0);

            // 2. 大范围烟雾（半径 1.5，数量 20）
            level.sendParticles(ParticleTypes.LARGE_SMOKE, centerX, centerY, centerZ, 20, 1.5, 1.0, 1.5, 0.05);

            // 3. 火花/火焰粒子（向四周飞溅，数量 60，速度较快）
            level.sendParticles(ParticleTypes.FIREWORK, centerX, centerY, centerZ, 60, 1.5, 1.2, 1.5, 0.15);

            // 4. 彩色粒子（使用 DustParticleOptions，需要先检查客户端是否支持，但服务端发送没问题）
            // 生成 100 个随机颜色的粒子，均匀分布在爆炸半径内
            int colorParticleCount = 100;
            double radius = RADIUS + 2;
            for (int i = 0; i < colorParticleCount; i++) {
                // 随机偏移
                double dx = (level.random.nextDouble() - 0.5) * 2 * radius;
                double dz = (level.random.nextDouble() - 0.5) * 2 * radius;
                double dy = level.random.nextDouble() * radius;
                // 随机鲜艳颜色
                float r = level.random.nextFloat();
                float g = level.random.nextFloat();
                float b = level.random.nextFloat();

                // 将浮点颜色值转换为 RGB 整型
                int red = (int) (r * 255);
                int green = (int) (g * 255);
                int blue = (int) (b * 255);
                int colorInt = (red << 16) | (green << 8) | blue;

                // 随机粒子大小
                float size = 0.6f + level.random.nextFloat();
                DustParticleOptions dustOptions = new DustParticleOptions(colorInt, size);
                level.sendParticles(dustOptions, centerX + dx, centerY + dy, centerZ + dz, 0, 0.0, 0.0, 0.0, 1.0);
            }
        }
    }
}
