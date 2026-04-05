package com.nsubtle;


import com.nsubtle.datagen.EnUsLangProvider;
import com.nsubtle.datagen.ModModelProvider;
import com.nsubtle.datagen.ZnChLangProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Nsubtle.MOD_ID)
public class DataGen {

    @SubscribeEvent
    public static void OnClientGatherData(GatherDataEvent.Client event) {
        event.createProvider(ModModelProvider::new);
        event.createProvider(ZnChLangProvider::new);
        event.createProvider(EnUsLangProvider::new);
    }
}
