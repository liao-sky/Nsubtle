package com.nsubtle.mixin;

import com.nsubtle.events.RegistryEvents.*;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PotionBrewing.class)
public class PotionRecipeMixin {
    @Inject(
            method = "addVanillaMixes",
            at = @At("HEAD")
    )
    private static void nsubtle$addCustomBrewing(PotionBrewing.Builder builder, CallbackInfo ci) {
        builder.addMix(Potions.AWKWARD, ItemRegistry.explosive_powder.asItem(), PotionRegistry.POTION_BROKEN_DEFENSE);
    }
}
