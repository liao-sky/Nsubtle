package com.nsubtle.events;

import com.nsubtle.effect.NsubtleEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;


public class EventsLoader {

    public static void LoadEvents(IEventBus bus) {
        RegistryEvents.CreativeTabs.CREATIVE_MODE_TABS.register(bus);
        RegistryEvents.ItemRegistry.ITEMS.register(bus);
        RegistryEvents.EffectRegistry.MOB_EFFECTS.register(bus);

        bus.addListener(ModEvents::addCreativeTab);
        NeoForge.EVENT_BUS.addListener(ModEvents::bucketRightClick);
        NeoForge.EVENT_BUS.register(NsubtleEffects.class);
    }
}
