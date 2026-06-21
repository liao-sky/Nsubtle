package com.nsubtle.effect;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import com.nsubtle.events.RegistryEvents.*;

public class NsubtleEffects {
    private static float P = 1.0f;

    public static class BROKEN_DEFENSE extends NsubtleEffect{


        public BROKEN_DEFENSE() {
            super(MobEffectCategory.HARMFUL,0x660033);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int remainingTicks, int level) {
            return remainingTicks % 7 == 0;
        }


        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            amplifier++;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if(entity.getRandom().nextFloat()<0.75*P){
                    DamageItemInSlot(slot,entity,amplifier);
                }
            }
            return true;
        }

        public void DamageItemInSlot(EquipmentSlot slot, LivingEntity livingBase, int amount) {
            if (slot!=EquipmentSlot.OFFHAND && slot!=EquipmentSlot.MAINHAND) {
                ItemStack stack = livingBase.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                stack.hurtAndBreak(stack.getDamageValue()+amount,livingBase,slot);}
            }
        }

    }

    public static Style DARK_RED = Style.EMPTY.withColor(TextColor.fromRgb(0xaa0000));
    @SubscribeEvent
    public static void BrokenDefense(LivingDamageEvent.Pre event) {
        //Nsubtle.LOGGER.warn("Broken Defense");
        LivingEntity entity = event.getEntity();
        Player player = null;
        if (entity instanceof Player) {
            player = (Player) entity;
            P-= player.getLuck()/100;
            if (P<0.01f) {
                P = 0.01f;
            }
        }
        float amount = event.getNewDamage();
        int ArmorValue = entity.getArmorValue();
        MobEffectInstance Effect = entity.getEffect(EffectRegistry.BROKEN_DEFENSE);
        if(Effect!=null
                && amount>ArmorValue*0.7f
                && entity.getArmorValue()!=0
        ){
            int amplified = Effect.getAmplifier();
            if (entity.getRandom().nextFloat()<0.25f * P){
                event.setNewDamage(amount*(amplified+11)/10);
                if (player != null) {
                    player.displayClientMessage(Component.translatable("effect.nsubtle.broken_defense.hurt", (float) (amplified + 11) / 10).setStyle(DARK_RED),true);
                }
            }
            if (entity.getRandom().nextFloat()<0.25f * P) {
                amplified++;
                entity.addEffect(new MobEffectInstance(EffectRegistry.BROKEN_DEFENSE.getDelegate(), (int) (20 * amount), amplified));
                if (player != null) {
                    player.displayClientMessage(Component.translatable("effect.nsubtle.broken_defense.break_again",(float) (amplified + 11) /10).setStyle(DARK_RED),true);
                }
            }
            if (amplified >= 9){
                if (player != null) {
                    player.displayClientMessage(Component.translatable("effect.nsubtle.broken_defense.warning"),true);
                }
            }
            if (entity.getRandom().nextFloat()<0.05f * Math.abs(amplified-3) * P && amplified >= 10){
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if(slot!= EquipmentSlot.MAINHAND
                            && entity.getRandom().nextFloat()<0.25f * P
                            && !entity.getItemBySlot(slot).isEmpty()){
                        entity.setItemSlot(slot, ItemStack.EMPTY);
                        entity.getItemBySlot(slot).shrink(1);
                        if (player != null) {
                            player.displayClientMessage(Component.translatable("effect.nsubtle.broken_defense.armor_break"),true);
                        }
                    }
                }
            }

        }
        else if (entity.getRandom().nextFloat()<0.25f * P
                && amount>ArmorValue*0.2f
                && entity.getArmorValue()!=0
        ) {
            entity.addEffect(new MobEffectInstance(EffectRegistry.BROKEN_DEFENSE.getDelegate(), (int) (20 * amount), 0));
            if (player != null) {
                player.displayClientMessage(Component.translatable("effect.nsubtle.broken_defense.break"),true);
            }
        }
    }






}
