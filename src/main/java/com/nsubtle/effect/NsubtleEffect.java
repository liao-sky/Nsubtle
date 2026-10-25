package com.nsubtle.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 本模组效果的基类：默认每个 tick 都允许结算，具体逻辑由子类覆盖
 * {@link #applyEffectTick(ServerLevel, LivingEntity, int)}。
 */
public abstract class NsubtleEffect extends MobEffect {

    protected NsubtleEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }
}
