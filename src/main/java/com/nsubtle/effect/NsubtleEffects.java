package com.nsubtle.effect;

import com.nsubtle.list.EnchantmentList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import com.nsubtle.events.RegistryEvents.*;

import java.util.Set;
public class NsubtleEffects {

    public static class BROKEN_DEFENSE extends NsubtleEffect{

        float P=1.0f;
        public BROKEN_DEFENSE() {
            super(MobEffectCategory.HARMFUL,0x660033);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int remainingTicks, int level) {
            return remainingTicks % 11 == 0;
        }


        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            amplifier++;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if(entity.getRandom().nextFloat()<0.5*P && slot!=EquipmentSlot.OFFHAND && slot!=EquipmentSlot.MAINHAND){
                    DamageItemInSlot(slot,entity,amplifier);
                }
            }
            return true;
        }

        public void DamageItemInSlot(EquipmentSlot slot, LivingEntity livingBase, int amount) {
                ItemStack stack = livingBase.getItemBySlot(slot);
                stack.setDamageValue(stack.getDamageValue()+amount);
        }

    }

    public static Style DARK_RED = Style.EMPTY.withColor(TextColor.fromRgb(0xaa0000));
    private static final Set<ResourceKey<DamageType>> ALLOWED_TYPES = Set.of(
            DamageTypes.MOB_ATTACK,
            DamageTypes.PLAYER_ATTACK,
            DamageTypes.TRIDENT,
            DamageTypes.ARROW
    );
    @SubscribeEvent
    public static void BrokenDefense(LivingDamageEvent.Pre event) {
        //Nsubtle.LOGGER.warn("Broken Defense");
        DamageSource source = event.getSource();
        boolean notAllowed = !source.typeHolder().unwrapKey()
                .map(ALLOWED_TYPES::contains).orElse(false);
        if (notAllowed) return;
        float P=1.0f;
        LivingEntity entity = event.getEntity();
        Player player = null;
        if (entity instanceof Player) {
            player = (Player) entity;
            P = Math.max(P-player.getLuck()/100,0.01f);
        }
        int toughLevel = getEnchantmentLevel(EnchantmentList.TOUGH,entity);
        P = (float) Math.max(P-toughLevel*0.02,0.01f);
        //Nsubtle.LOGGER.info("{} tough level: {}", entity.getName(), toughLevel);

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
                && amount>ArmorValue*0.3f
                && entity.getArmorValue()!=0
        ) {
            entity.addEffect(new MobEffectInstance(EffectRegistry.BROKEN_DEFENSE.getDelegate(), (int) (20 * amount), 0));
            if (player != null) {
                player.displayClientMessage(Component.translatable("effect.nsubtle.broken_defense.break"),true);
            }
        }
    }

    public static int getEnchantmentLevel(Holder<Enchantment> enchantment, LivingEntity entity) {
        int level = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack armor = entity.getItemBySlot(slot);
            int l = armor.getEnchantmentLevel(enchantment);
            level += l;
        }
        return level;
    }

    public static int getEnchantmentLevel(ResourceKey<Enchantment> enchantmentKey, LivingEntity entity) {
        var registryAccess = entity.registryAccess();
        var enchantmentLookup = registryAccess.lookup(Registries.ENCHANTMENT);
        if (enchantmentLookup.isEmpty()) {
            return 0;
        }
        var holder = enchantmentLookup.get().get(enchantmentKey);
        return holder.map(enchantmentReference -> getEnchantmentLevel(enchantmentReference, entity)).orElse(0);
    }
}
