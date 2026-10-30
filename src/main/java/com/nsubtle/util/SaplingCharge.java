package com.nsubtle.util;

import com.nsubtle.Nsubtle;
import com.nsubtle.component.SaplingLoad;
import com.nsubtle.entity.SaplingProjectile;
import com.nsubtle.events.RegistryEvents;
import com.nsubtle.list.EnchantmentList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * 树苗炸弹的装填与发射。
 * <p>
 * 两把武器的规则不同：
 * <ul>
 *   <li><b>弩</b>：两步装填，<b>先骨粉、后树苗</b>；每一步都要像原版上弦那样按住右键蓄满力再松手，
 *       装好后右键发射；</li>
 *   <li><b>弓</b>：不装填。副手拿着树苗、背包里有骨粉时，拉弓松手直接消耗
 *       1 个骨粉 + 1 枚树苗发射；条件不满足时保持原版射箭。</li>
 * </ul>
 * 弩的进度存在武器自己的数据组件 {@code nsubtle:sapling_load} 上，换手/入箱/存档都不会丢。
 */
public final class SaplingCharge {

    /** 弓发射树苗炸弹需要的蓄力时间（tick），比原版弓的 20 tick 略长。 */
    private static final float BOW_CHARGE_TICKS = 30.0F;

    /** 树苗炸弹命中的基础伤害。 */
    private static final float BASE_DAMAGE = 4.0F;

    /** 有专属“弩 + 树苗”模型的树苗；其余（含模组树苗）统一用通用树苗模型。 */
    private static final Set<String> KNOWN_SAPLINGS = Set.of(
            "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling", "acacia_sapling",
            "dark_oak_sapling", "cherry_sapling", "pale_oak_sapling", "mangrove_propagule");

    private SaplingCharge() {
    }

    public static DataComponentType<SaplingLoad> type() {
        return RegistryEvents.DataComponentRegistry.SAPLING_LOAD.get();
    }

    // ------------------------------------------------------------------
    // 弩：先骨粉，后树苗，每步都要蓄力
    // ------------------------------------------------------------------

    /** 装填完成（骨粉 + 树苗都在）。 */
    public static boolean isCrossbowReady(ItemStack weapon) {
        SaplingLoad load = weapon.get(type());
        return load != null && load.hasSapling();
    }

    /** 弩是否处于“还能继续装填”的状态。 */
    public static boolean hasPowderOnly(ItemStack weapon) {
        SaplingLoad load = weapon.get(type());
        return load != null && !load.hasSapling();
    }

    /**
     * 当前这只手按下右键后，是否应该进入“装填蓄力”。
     * 也就是：另一只手上正好拿着这一步需要的材料。
     * <p>
     * 附有「生机」时，弩可以跳过骨粉直接装树苗。
     */
    public static boolean canLoad(Player player, ItemStack weapon, InteractionHand hand) {
        SaplingLoad load = weapon.get(type());
        ItemStack other = player.getItemInHand(opposite(hand));
        if (load == null) {
            if (other.is(Items.BONE_MEAL)) {
                return true;
            }
            return other.is(ItemTags.SAPLINGS) && hasVitality(player.level(), weapon);
        }
        return !load.hasSapling() && other.is(ItemTags.SAPLINGS);
    }

    /**
     * 蓄满力松手时真正把材料装进去。
     *
     * @return 是否装入成功
     */
    public static boolean loadFrom(ServerLevel level, Player player, ItemStack weapon, InteractionHand hand) {
        SaplingLoad load = weapon.get(type());
        ItemStack other = player.getItemInHand(opposite(hand));
        boolean vitality = hasVitality(level, weapon);

        if (load == null) {
            if (other.is(Items.BONE_MEAL)) {
                weapon.set(type(), SaplingLoad.powderOnly());
                takeArrows(player, weapon);
                applyCrossbowModel(weapon, ItemStack.EMPTY);
                // 生机：骨粉不消耗（但这里仍然只是“压药”这一步）
                if (!vitality) {
                    consume(player, other);
                }
                playSound(level, player, SoundEvents.BONE_MEAL_USE, 1.0F, 1.0F);
                return true;
            }
            if (!vitality || !other.is(ItemTags.SAPLINGS)) {
                return false;
            }
            // 生机：跳过骨粉，直接一步装好树苗
            return finishLoad(level, player, weapon, hand, other);
        }

        if (load.hasSapling() || !other.is(ItemTags.SAPLINGS)) {
            return false;
        }
        return finishLoad(level, player, weapon, hand, other);
    }

    /** 把树苗真正装进弩里（消耗树苗、换模型、扣耐久）。 */
    private static boolean finishLoad(ServerLevel level, Player player, ItemStack weapon,
                                      InteractionHand hand, ItemStack saplingStack) {
        ItemStack loaded = saplingStack.copyWithCount(1);
        weapon.set(type(), new SaplingLoad(loaded));
        takeArrows(player, weapon);
        applyCrossbowModel(weapon, loaded);
        consume(player, saplingStack);
        playSound(level, player, SoundEvents.BUNDLE_INSERT, 0.8F, 1.1F);
        refreshGlint(weapon, true);
        // 装填一次扣 1 点耐久（和原版弩上弦一致，附魔耐久照常生效）
        if (!player.hasInfiniteMaterials()) {
            weapon.hurtAndBreak(1, player, hand.asEquipmentSlot());
        }
        return true;
    }

    /**
     * 武器是否附有「生机」。
     * <p>
     * 效果：装填/发射都不再需要也不会消耗骨粉，弩可以直接装树苗。
     */
    public static boolean hasVitality(Level level, ItemStack weapon) {
        return level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(EnchantmentList.VITALITY)
                .map(holder -> weapon.getEnchantmentLevel(holder) > 0)
                .orElse(false);
    }

    /**
     * 装填期间要阻止原版 {@code tryLoadProjectiles} 把箭塞进弩里，
     * 否则“正在装树苗”的弩会先被上成普通箭。
     */
    public static boolean blocksVanillaLoad(LivingEntity shooter, ItemStack weapon) {
        if (weapon.has(type())) {
            return true;
        }
        if (!(shooter instanceof Player player)) {
            return false;
        }
        if (player.getMainHandItem().is(Items.BONE_MEAL) || player.getOffhandItem().is(Items.BONE_MEAL)) {
            return true;
        }
        // 「生机」让弩可以只拿树苗直接装填，此时同样不能让原版把箭塞进去。
        return hasVitality(player.level(), weapon)
                && (player.getMainHandItem().is(ItemTags.SAPLINGS) || player.getOffhandItem().is(ItemTags.SAPLINGS));
    }

    /**
     * 弩发射：取出装填的树苗并清空状态。只在服务端调用。
     * <p>
     * 走的是原版弩同一套「多重射击」处理：先用 {@link EnchantmentHelper#processProjectileCount}
     * 取连发数，再用 {@link EnchantmentHelper#processProjectileSpread} 取扩散角，
     * 因此多重射击（3 连发）在树苗炸弹上同样生效。
     */
    public static boolean fireCrossbow(ServerLevel level, Player player, ItemStack weapon, InteractionHand hand, float velocity) {
        SaplingLoad load = weapon.get(type());
        if (load == null || !load.hasSapling()) {
            return false;
        }

        ItemStack payload = load.sapling();
        int count = Math.max(1, EnchantmentHelper.processProjectileCount(level, weapon, player, 1));
        float spread = EnchantmentHelper.processProjectileSpread(level, weapon, player, 0.0F);
        float step = count == 1 ? 0.0F : 2.0F * spread / (count - 1);
        float middle = (count - 1) % 2 * step / 2.0F;
        float direction = 1.0F;

        for (int i = 0; i < count; i++) {
            float angle = middle + direction * ((i + 1) / 2) * step;
            direction = -direction;
            // 多重射击时只有第一颗负责在长树失败后退还树苗，避免一颗变三颗。
            shoot(level, player, weapon, payload, velocity, 0.5F, angle, BASE_DAMAGE).setRefundOnFail(i == 0);
        }

        weapon.remove(type());
        restoreModel(weapon);
        refreshGlint(weapon, false);
        return true;
    }

    // ------------------------------------------------------------------
    // 弓：副手树苗 + 背包骨粉，直接消耗发射
    // ------------------------------------------------------------------

    /** 是否满足发射条件：副手树苗，并且（背包有骨粉 或 弓附有「生机」）。 */
    public static boolean canUseBow(Level level, Player player, ItemStack bow) {
        return player.getOffhandItem().is(ItemTags.SAPLINGS)
                && (findBonemeal(player) >= 0 || hasVitality(level, bow));
    }

    /** 弓蓄力曲线：满力需要 30 tick，比原版略长。 */
    public static float bowPower(int charge) {
        float f = charge / BOW_CHARGE_TICKS;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }

    /**
     * 弓发射：消耗副手 1 枚树苗（有「生机」时不消耗骨粉，也不要求背包里有骨粉）。
     * 只在服务端调用。
     * <p>
     * 力量等伤害附魔在命中时由原版 {@code EnchantmentHelper} 结算，见 {@code SaplingProjectile#onHitEntity}。
     */
    public static boolean fireBow(ServerLevel level, Player player, ItemStack bow, float velocity) {
        ItemStack sapling = player.getOffhandItem();
        if (!sapling.is(ItemTags.SAPLINGS)) {
            return false;
        }
        boolean vitality = hasVitality(level, bow);
        int slot = findBonemeal(player);
        if (slot < 0 && !vitality) {
            return false;
        }

        ItemStack payload = sapling.copyWithCount(1);
        if (!player.hasInfiniteMaterials()) {
            sapling.shrink(1);
            if (!vitality && slot >= 0) {
                consumeSlot(player, slot);
            }
        }

        shoot(level, player, bow, payload, velocity, 1.0F, 0.0F, BASE_DAMAGE);
        return true;
    }

    // ------------------------------------------------------------------
    // 公共
    // ------------------------------------------------------------------

    private static SaplingProjectile shoot(ServerLevel level, Player player, ItemStack weapon, ItemStack sapling,
                                           float velocity, float inaccuracy, float angle, float damage) {
        SaplingProjectile projectile = new SaplingProjectile(level, player, sapling);
        projectile.setWeapon(weapon);
        projectile.setImpactDamage(damage);
        projectile.setPierceLevel(EnchantmentHelper.getPiercingCount(level, weapon, sapling));
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), angle, velocity, inaccuracy);
        level.addFreshEntity(projectile);
        return projectile;
    }

    private static InteractionHand opposite(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
    }

    private static int findBonemeal(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.BONE_MEAL)) {
                return i;
            }
        }
        return -1;
    }

    private static void consumeSlot(Player player, int slot) {
        Inventory inventory = player.getInventory();
        ItemStack stack = inventory.getItem(slot);
        stack.shrink(1);
        if (stack.isEmpty()) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }
    }

    /**
     * 装填完成后给武器加附魔光效，作为“可以发射了”的视觉提示。
     */
    private static void refreshGlint(ItemStack weapon, boolean ready) {
        if (ready) {
            weapon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            weapon.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }

    /**
     * 装填前把弩里已经上好的箭取下来还回背包。
     * <p>
     * 这样既不会出现“树苗 + 箭”同时挂在弩上，也不会白白吞掉玩家的一支箭。
     */
    private static void takeArrows(Player player, ItemStack weapon) {
        ChargedProjectiles charged = weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        if (charged == null || charged.isEmpty()) {
            return;
        }
        for (ItemStack arrow : charged.getItems()) {
            if (!player.getInventory().add(arrow)) {
                player.drop(arrow, false);
            }
        }
    }

    /**
     * 切换弩的物品模型：装骨粉阶段显示“弩 + 骨粉”，装好树苗后显示“弩 + 对应树苗”。
     */
    private static void applyCrossbowModel(ItemStack weapon, ItemStack sapling) {
        String path = sapling.isEmpty() ? "bone_meal" : species(sapling);
        weapon.set(DataComponents.ITEM_MODEL, id("crossbow_loaded_" + path));
    }

    /**
     * 拉弓时把弓的模型换成“弓 + 对应树苗”，松手时由 {@link #restoreModel} 还原。
     */
    public static void applyBowModel(Player player, ItemStack bow) {
        ItemStack sapling = player.getOffhandItem();
        String path = sapling.is(ItemTags.SAPLINGS) ? species(sapling) : "sapling";
        bow.set(DataComponents.ITEM_MODEL, id("bow_loaded_" + path));
    }

    /**
     * 还原物品的默认模型。
     * <p>
     * 注意：不能 remove(ITEM_MODEL)。原版每个物品默认就带 ITEM_MODEL 组件，
     * 删掉它会让客户端解析不到模型，物品直接变透明。
     */
    public static void restoreModel(ItemStack stack) {
        stack.set(DataComponents.ITEM_MODEL, defaultModel(stack));
    }

    /** 物品默认的模型 id（原版把它写在物品默认组件里）。 */
    private static ResourceLocation defaultModel(ItemStack stack) {
        ResourceLocation model = stack.getItem().getDefaultInstance().get(DataComponents.ITEM_MODEL);
        return model != null ? model : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private static String species(ItemStack sapling) {
        String item = BuiltInRegistries.ITEM.getKey(sapling.getItem()).getPath();
        return KNOWN_SAPLINGS.contains(item) ? item : "sapling";
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Nsubtle.MOD_ID, path);
    }

    private static void playSound(ServerLevel level, Player player, net.minecraft.sounds.SoundEvent sound,
                                  float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
