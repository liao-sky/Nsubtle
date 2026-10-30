package com.nsubtle.mixin;

import com.nsubtle.util.SaplingCharge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让弓/弩能把“树苗 + 骨粉”当投掷物打出去。
 * <p>
 * <ul>
 *   <li><b>弓</b>：副手树苗 + 背包骨粉时接管拉弓与松手，直接消耗两者发射，蓄力 30 tick；</li>
 *   <li><b>弩</b>：右键蓄满力松手装入骨粉，再蓄满力松手装入树苗，装好后再右键发射。
 *       蓄力过程完全复用原版弩的上弦动画与音效，只是把“上箭”换成了“装材料”。</li>
 * </ul>
 * 条件不满足时一律原样交还原版，因此普通射箭/上弦不受影响。
 */
public final class SaplingWeaponMixin {

    /** 弩的发射初速，与原版弩一致。 */
    private static final float CROSSBOW_POWER = 3.15F;

    private SaplingWeaponMixin() {
    }

    @Mixin(BowItem.class)
    public abstract static class BowItemMixin {

        @Inject(method = "use", at = @At("HEAD"), cancellable = true)
        private void nsubtle$useSapling(Level level, Player player, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResult> cir) {
            ItemStack weapon = player.getItemInHand(hand);
            if (!SaplingCharge.canUseBow(level, player, weapon)) {
                // 副手没有树苗，或既没有骨粉也没有「生机」：保持原版拉弓。
                return;
            }

            // 有树苗炸弹可打：即使背包里没有箭也能拉弓，并把弓换成“弓 + 树苗”的模型。
            player.startUsingItem(hand);
            SaplingCharge.applyBowModel(player, weapon);
            cir.setReturnValue(InteractionResult.CONSUME);
        }

        @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
        private void nsubtle$releaseSapling(ItemStack weapon, Level level, LivingEntity entity, int timeLeft,
                                            CallbackInfoReturnable<Boolean> cir) {
            // 无论这一箭是不是树苗炸弹，松手时都把弓的模型还原。
            SaplingCharge.restoreModel(weapon);

            if (!(entity instanceof Player player) || !SaplingCharge.canUseBow(level, player, weapon)) {
                return;
            }

            int charge = weapon.getUseDuration(entity) - timeLeft;
            float power = SaplingCharge.bowPower(charge);
            if (power < 0.1F) {
                cir.setReturnValue(false);
                return;
            }

            if (level instanceof ServerLevel serverLevel) {
                SaplingCharge.fireBow(serverLevel, player, weapon, power * 3.0F);
                InteractionHand usedHand = player.getMainHandItem() == weapon
                        ? InteractionHand.MAIN_HAND
                        : InteractionHand.OFF_HAND;
                if (!player.hasInfiniteMaterials()) {
                    weapon.hurtAndBreak(1, player, usedHand);
                }
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS, 1.0F,
                    1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
            cir.setReturnValue(true);
        }
    }

    @Mixin(CrossbowItem.class)
    public abstract static class CrossbowItemMixin {

        @Inject(method = "use", at = @At("HEAD"), cancellable = true)
        private void nsubtle$useSapling(Level level, Player player, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResult> cir) {
            ItemStack weapon = player.getItemInHand(hand);

            // 装填完成：已上弦，右键发射。
            if (SaplingCharge.isCrossbowReady(weapon)) {
                if (level instanceof ServerLevel serverLevel) {
                    SaplingCharge.fireCrossbow(serverLevel, player, weapon, hand, CROSSBOW_POWER);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT,
                            SoundSource.PLAYERS, 1.0F,
                            1.0F / (level.getRandom().nextFloat() * 0.5F + 1.8F) + 0.43F);
                }
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }

            // 装填中：按住右键蓄力（复用原版弩的上弦动画），松手才真正装入。
            if (SaplingCharge.canLoad(player, weapon, hand)) {
                player.startUsingItem(hand);
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }

            // 只装了骨粉、手上又没有树苗：不让原版拿去上普通箭。
            if (SaplingCharge.hasPowderOnly(weapon)) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }

        @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
        private void nsubtle$releaseLoad(ItemStack weapon, Level level, LivingEntity entity, int timeLeft,
                                         CallbackInfoReturnable<Boolean> cir) {
            if (!(entity instanceof Player player)) {
                return;
            }

            InteractionHand hand = player.getMainHandItem() == weapon
                    ? InteractionHand.MAIN_HAND
                    : InteractionHand.OFF_HAND;

            if (!SaplingCharge.canLoad(player, weapon, hand)) {
                // 蓄力途中把材料换走了：这次装填作废。
                if (SaplingCharge.hasPowderOnly(weapon)) {
                    cir.setReturnValue(false);
                }
                return;
            }

            int charge = weapon.getUseDuration(entity) - timeLeft;
            if (charge < CrossbowItem.getChargeDuration(weapon, entity)) {
                cir.setReturnValue(false);
                return;
            }

            if (level instanceof ServerLevel serverLevel) {
                SaplingCharge.loadFrom(serverLevel, player, weapon, hand);
            }
            cir.setReturnValue(true);
        }

        /**
         * 装填期间阻止原版把箭塞进弩里，否则“正在装树苗”的弩会先被上成普通箭。
         */
        @Inject(method = "tryLoadProjectiles", at = @At("HEAD"), cancellable = true)
        private static void nsubtle$blockVanillaLoad(LivingEntity shooter, ItemStack crossbowStack,
                                                     CallbackInfoReturnable<Boolean> cir) {
            if (SaplingCharge.blocksVanillaLoad(shooter, crossbowStack)) {
                cir.setReturnValue(false);
            }
        }
    }
}
