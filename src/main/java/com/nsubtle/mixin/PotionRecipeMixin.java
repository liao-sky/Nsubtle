package com.nsubtle.mixin;

import com.nsubtle.events.RegistryEvents;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把“爆裂粉末”接入原版酿造表：粗制的药水 + 爆裂粉末 → 破防药水。
 */
@Mixin(PotionBrewing.class)
public class PotionRecipeMixin {

    @Inject(method = "addVanillaMixes", at = @At("HEAD"))
    private static void nsubtle$addCustomBrewing(PotionBrewing.Builder builder, CallbackInfo ci) {
        builder.addMix(Potions.AWKWARD,
                RegistryEvents.ItemRegistry.EXPLOSIVE_POWDER.asItem(),
                RegistryEvents.PotionRegistry.POTION_BROKEN_DEFENSE);
    }
}
