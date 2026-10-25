package com.nsubtle.list;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.List;

/**
 * 食物数值表。食物属性本身（nutrition/saturation）与“食用效果”分开定义。
 */
public final class FoodList {

    private FoodList() {
    }

    private static ApplyStatusEffectsConsumeEffect effect(MobEffectInstance instance, float chance) {
        return new ApplyStatusEffectsConsumeEffect(List.of(instance), chance);
    }

    /** 羹：8 点饱食度，6 种效果各自独立 33% 概率。 */
    public static final FoodProperties SOUP_FOOD = new FoodProperties.Builder()
            .nutrition(8)
            .saturationModifier(0.5F)
            .alwaysEdible()
            .build();

    public static final Consumable SOUP_CONSUMABLE = Consumables.defaultFood()
            .onConsume(effect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 20, 4), 0.33F))
            .onConsume(effect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 1), 0.33F))
            .onConsume(effect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1), 0.33F))
            .onConsume(effect(new MobEffectInstance(MobEffects.GLOWING, 20 * 20, 0), 0.33F))
            .onConsume(effect(new MobEffectInstance(MobEffects.HUNGER, 20 * 20, 0), 0.33F))
            .onConsume(effect(new MobEffectInstance(MobEffects.SATURATION, 20 * 20, 0), 0.33F))
            .build();

    /** 金头：4 点饱食度，0.8 秒快速食用，几乎必定获得多种正面效果。 */
    public static final FoodProperties GOLD_HEAD_FOOD = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(1.0F)
            .alwaysEdible()
            .build();

    public static final Consumable GOLD_HEAD_CONSUMABLE = Consumables.defaultFood()
            .consumeSeconds(0.8F)
            .onConsume(effect(new MobEffectInstance(MobEffects.REGENERATION, 15 * 20, 1), 1.0F))
            .onConsume(effect(new MobEffectInstance(MobEffects.RESISTANCE, 10 * 20, 1), 0.8F))
            .onConsume(effect(new MobEffectInstance(MobEffects.ABSORPTION, 15 * 20, 1), 1.0F))
            .onConsume(effect(new MobEffectInstance(MobEffects.GLOWING, 20 * 20, 0), 1.0F))
            .onConsume(effect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 15 * 20, 0), 1.0F))
            .onConsume(effect(new MobEffectInstance(MobEffects.SPEED, 10 * 20, 0), 1.0F))
            .build();
}
