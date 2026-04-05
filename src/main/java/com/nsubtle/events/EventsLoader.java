package com.nsubtle.events;

import net.neoforged.bus.api.IEventBus;


public class EventsLoader {

    public static void LoadEvents(IEventBus bus) {
        RegistryEvents.CreativeTabs.CREATIVE_MODE_TABS.register(bus);
        RegistryEvents.ItemRegistry.ITEMS.register(bus);

        bus.addListener(ModEvents::addCreativeTab);
    }
}
