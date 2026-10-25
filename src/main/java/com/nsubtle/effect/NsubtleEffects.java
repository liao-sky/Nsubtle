package com.nsubtle.effect;

import com.nsubtle.events.RegistryEvents;
import com.nsubtle.list.EnchantmentList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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

import java.util.Set;

/**
 * 本模组的战斗机制集合。
 * <p>
 * 目前包含“破防（Broken Defense）”：受到近战/三叉戟/箭矢伤害时，有概率放大本次伤害、
 * 叠加破防层数，并在层数到达上限后直接摧毁护甲。幸运值与“坚韧”附魔可以降低触发概率。
 */
public final class NsubtleEffects {

    /** 破防层数（amplifier）上限，避免伤害倍率无限增长。 */
    private static final int MAX_AMPLIFIER = 10;
    /** 伤害放大 / 叠层 / 首次破防的基础触发率，实际概率为 {@code BASE_CHANCE × P}。 */
    private static final float BASE_CHANCE = 0.25F;
    /** 触发概率修正的下限，保证再高的幸运/坚韧也保留极小概率。 */
    private static final float MIN_CHANCE = 0.01F;

    /** 每次受击触发“高额耐久损耗”的概率倍率，实际为 {@code 0.25 × P}。 */
    private static final float WEAR_TRIGGER_CHANCE = 0.25F;
    /** 触发后，每件护甲独立损耗耐久的概率。 */
    private static final float WEAR_PIECE_CHANCE = 0.5F;
    /** 0 层破防时单次损耗的耐久。 */
    private static final int WEAR_BASE = 4;
    /** 每层破防额外增加的耐久损耗。 */
    private static final int WEAR_PER_LEVEL = 4;

    public static final Style DARK_RED = Style.EMPTY.withColor(TextColor.fromRgb(0xAA0000));

    private static final Set<ResourceKey<DamageType>> ALLOWED_TYPES = Set.of(
            DamageTypes.MOB_ATTACK,
            DamageTypes.PLAYER_ATTACK,
            DamageTypes.TRIDENT,
            DamageTypes.ARROW
    );

    private NsubtleEffects() {
    }

    /**
     * 破防效果本体。
     * <p>
     * 耐久损耗已经改为受击时的概率结算（见 {@link #wearArmor(LivingEntity, int)}），
     * 效果本身不再逐 tick 生效，因此直接告诉原版“本 tick 不结算”。
     */
    public static final class BROKEN_DEFENSE extends NsubtleEffect {

        public BROKEN_DEFENSE() {
            super(MobEffectCategory.HARMFUL, 0x660033);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int remainingTicks, int amplifier) {
            return false;
        }
    }

    /**
     * 受击结算入口。
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        boolean allowed = source.typeHolder().unwrapKey()
                .map(ALLOWED_TYPES::contains)
                .orElse(false);
        if (!allowed) {
            return;
        }

        LivingEntity entity = event.getEntity();
        int armorValue = entity.getArmorValue();
        if (armorValue <= 0) {
            return;
        }

        float p = chanceModifier(entity);
        float chance = BASE_CHANCE * p;
        float amount = event.getNewDamage();
        MobEffectInstance effect = entity.getEffect(RegistryEvents.EffectRegistry.BROKEN_DEFENSE);

        if (effect == null) {
            // 尚未破防：只有伤害超过护甲值 30% 才会被打出第一层。
            if (amount > armorValue * 0.3F && roll(entity, chance)) {
                entity.addEffect(new MobEffectInstance(
                        RegistryEvents.EffectRegistry.BROKEN_DEFENSE,
                        effectDuration(amount),
                        0));
                notify(entity, "effect.nsubtle.broken_defense.break");
            }
            return;
        }

        int amplifier = effect.getAmplifier();

        // 1. 高额耐久损耗：低频率触发，单次损耗由层数决定，概率受幸运/坚韧影响。
        if (roll(entity, WEAR_TRIGGER_CHANCE * p)) {
            wearArmor(entity, amplifier);
        }

        // 2. 后续伤害结算：伤害需要超过护甲值 70%。
        if (amount <= armorValue * 0.7F) {
            return;
        }

        // 3. 本次伤害被放大。
        if (roll(entity, chance)) {
            float multiplier = multiplier(amplifier);
            event.setNewDamage(amount * multiplier);
            notify(entity, "effect.nsubtle.broken_defense.hurt", multiplier);
        }

        // 4. 再叠一层破防。
        if (amplifier < MAX_AMPLIFIER && roll(entity, chance)) {
            amplifier++;
            entity.addEffect(new MobEffectInstance(
                    RegistryEvents.EffectRegistry.BROKEN_DEFENSE,
                    effectDuration(amount),
                    amplifier));
            notify(entity, "effect.nsubtle.broken_defense.break_again", multiplier(amplifier));
        }

        // 5. 层数警告。
        if (amplifier >= MAX_AMPLIFIER - 1) {
            notify(entity, "effect.nsubtle.broken_defense.warning");
        }

        // 6. 层数到达上限后，护甲会被直接摧毁。
        if (amplifier >= MAX_AMPLIFIER && roll(entity, 0.05F * Math.abs(amplifier - 3) * p)) {
            boolean destroyed = false;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!slot.isArmor()) {
                    continue;
                }
                if (entity.getRandom().nextFloat() >= 0.25F * p) {
                    continue;
                }
                ItemStack stack = entity.getItemBySlot(slot);
                if (stack.isEmpty() || !stack.isDamageableItem()) {
                    continue;
                }
                entity.setItemSlot(slot, ItemStack.EMPTY);
                entity.onEquippedItemBroken(stack.getItem(), slot);
                destroyed = true;
            }
            if (destroyed) {
                notify(entity, "effect.nsubtle.broken_defense.armor_break");
            }
        }
    }

    /**
     * 返回概率倍率 P：基础 1.0，幸运值每点 -1%，每级坚韧附魔 -2%，下限 {@link #MIN_CHANCE}。
     * <p>
     * 实际触发率为 {@code 0.25 × P}（护甲摧毁为 {@code 0.05 × |层数 - 3| × P}）。
     */
    private static float chanceModifier(LivingEntity entity) {
        float chance = 1.0F;
        if (entity instanceof Player player) {
            chance -= player.getLuck() / 100.0F;
        }
        chance -= getEnchantmentLevel(EnchantmentList.TOUGH, entity) * 0.02F;
        return Math.max(chance, MIN_CHANCE);
    }

    private static float multiplier(int amplifier) {
        return (amplifier + 11) / 10.0F;
    }

    private static int effectDuration(float damage) {
        return Math.max(1, (int) (20 * damage));
    }

    private static boolean roll(LivingEntity entity, float chance) {
        return entity.getRandom().nextFloat() < Mth.clamp(chance, 0.0F, 1.0F);
    }

    private static void notify(LivingEntity entity, String translationKey, Object... args) {
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(translationKey, args).setStyle(DARK_RED), true);
        }
    }

    /**
     * 高额耐久损耗：每件护甲独立判定，单次损耗量 = {@code WEAR_BASE + 层数 × WEAR_PER_LEVEL}。
     */
    private static void wearArmor(LivingEntity entity, int amplifier) {
        int wear = WEAR_BASE + amplifier * WEAR_PER_LEVEL;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            if (entity.getRandom().nextFloat() >= WEAR_PIECE_CHANCE) {
                continue;
            }
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isDamageableItem()) {
                stack.hurtAndBreak(wear, entity, slot);
            }
        }
    }

    /**
     * 统计护甲槽位上某附魔的总等级（忽略主手/副手）。
     */
    public static int getEnchantmentLevel(Holder<Enchantment> enchantment, LivingEntity entity) {
        int level = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            level += entity.getItemBySlot(slot).getEnchantmentLevel(enchantment);
        }
        return level;
    }

    /**
     * 按资源键统计护甲附魔等级，注册表不可用时返回 0。
     */
    public static int getEnchantmentLevel(ResourceKey<Enchantment> enchantmentKey, LivingEntity entity) {
        var enchantmentLookup = entity.registryAccess().lookup(Registries.ENCHANTMENT);
        if (enchantmentLookup.isEmpty()) {
            return 0;
        }
        return enchantmentLookup.get()
                .get(enchantmentKey)
                .map(holder -> getEnchantmentLevel(holder, entity))
                .orElse(0);
    }
}
