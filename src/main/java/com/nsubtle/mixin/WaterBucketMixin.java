package com.nsubtle.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 允许玩家在非水中/非雨天时，只要副手拿着水桶就能使用三叉戟。
 */
public class WaterBucketMixin {

    @Mixin(TridentItem.class)
    public abstract static class TridentItemMixin {

        @ModifyExpressionValue(
                method = "use",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"
                )
        )
        private boolean nsubtle$allowUseWithBucket(boolean original, Level level, Player player, InteractionHand hand) {
            if (!original && player.getOffhandItem().is(Items.WATER_BUCKET)) {
                return true;
            }
            return original;
        }

        @ModifyExpressionValue(
                method = "releaseUsing",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"
                )
        )
        private boolean nsubtle$allowReleaseWithBucket(boolean original,
                                                       ItemStack stack, Level level,
                                                       LivingEntity entity, int useDuration) {
            if (!original && entity instanceof Player player
                    && player.getOffhandItem().is(Items.WATER_BUCKET)) {
                return true;
            }
            return original;
        }
    }
}
