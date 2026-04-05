package com.nsubtle.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;


public class ModEvents {

    @SubscribeEvent
    public static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if(event.getTab() == RegistryEvents.CreativeTabs.NSUBTLE.get()){
            event.acceptAll(RegistryEvents.ItemRegistry.ITEMS.getEntries().stream()
                    .map(holder->holder.get().getDefaultInstance())
                    .toList());
        }

    }
}
