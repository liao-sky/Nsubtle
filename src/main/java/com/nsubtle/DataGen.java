package com.nsubtle;


import com.nsubtle.datagen.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Nsubtle.MOD_ID)
public class DataGen {

    @SubscribeEvent
    public static void OnClientGatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        event.createProvider(ModModelProvider::new);
        event.createProvider(ZnChLangProvider::new);
        event.createProvider(EnUsLangProvider::new);
    }

    @SubscribeEvent
    public static void OnServerGatherData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        event.createProvider(ModRecipeProvider.Runner::new);
        event.createProvider(ModLootTableProvider::new);

        generator.addProvider(true, new ModEnchantmentProvider.ModDatapackProvider(
                packOutput,
                event.getLookupProvider(),
                Set.of(Nsubtle.MOD_ID)
        ));
        generator.addProvider(true, new ModEnchantmentProvider.ModEnchantmentTagProvider(packOutput,
                lookupProvider));
    }
}
