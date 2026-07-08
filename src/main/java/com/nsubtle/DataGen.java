package com.nsubtle;


import com.nsubtle.datagen.EnUsLangProvider;
import com.nsubtle.datagen.ModModelProvider;
import com.nsubtle.datagen.ModRecipeProvider;
import com.nsubtle.datagen.ZnChLangProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Nsubtle.MOD_ID)
public class DataGen {

    @SubscribeEvent
    public static void OnClientGatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        HolderLookup.Provider lookupProvider = event.getLookupProvider().join();

        event.createProvider(ModModelProvider::new);
        event.createProvider(ZnChLangProvider::new);
        event.createProvider(EnUsLangProvider::new);
        event.createProvider(ModRecipeProvider.Runner::new);
    }

    @SubscribeEvent
    public static void OnServerGatherData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        HolderLookup.Provider lookupProvider = event.getLookupProvider().join();
    }
}
