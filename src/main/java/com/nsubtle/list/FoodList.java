package com.nsubtle.list;


import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.List;

public class FoodList {
    private static ApplyStatusEffectsConsumeEffect effect(MobEffectInstance effect,float chance) {
        return new ApplyStatusEffectsConsumeEffect(List.of(effect),chance);
    }
    public static final FoodProperties soup_fp = new FoodProperties.Builder()
            .nutrition(8)
            .saturationModifier(0.5f)
            .alwaysEdible()
            .build();
    public static final Consumable soup=(Consumables.defaultFood()
            .onConsume(effect(new MobEffectInstance(MobEffects.RESISTANCE,20*20,4),0.33f))
            .onConsume(effect(new MobEffectInstance(MobEffects.POISON,20*20,1),0.33f))
            .onConsume(effect(new MobEffectInstance(MobEffects.REGENERATION,20*20,1),0.33f))
            .onConsume(effect(new MobEffectInstance(MobEffects.GLOWING,20*20,0),0.33f))
            .onConsume(effect(new MobEffectInstance(MobEffects.HUNGER,20*20,0),0.33f))
            .onConsume(effect(new MobEffectInstance(MobEffects.SATURATION,20*20,0),0.33f))
            .build());

    public static final FoodProperties gold_head_fp = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(1f)
            .alwaysEdible()
            .build();
    public static final Consumable gold_head=(Consumables.defaultFood().consumeSeconds(0.8f)
            .onConsume(effect(new MobEffectInstance(MobEffects.REGENERATION,15*20,1),1f))
            .onConsume(effect(new MobEffectInstance(MobEffects.RESISTANCE,10*20,1),0.8f))
            .onConsume(effect(new MobEffectInstance(MobEffects.ABSORPTION,15*20,1),1f))
            .onConsume(effect(new MobEffectInstance(MobEffects.GLOWING,20*20,0),1f))
            .onConsume(effect(new MobEffectInstance(MobEffects.HEALTH_BOOST,15*20,0),1f))
            .onConsume(effect(new MobEffectInstance(MobEffects.SPEED,10*20,0),1f))
            .build()
    );
}
