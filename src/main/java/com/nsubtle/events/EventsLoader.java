package com.nsubtle.events;

import com.nsubtle.effect.NsubtleEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 注册与事件总线的统一挂载点。
 */
public final class EventsLoader {

    private EventsLoader() {
    }

    public static void loadEvents(IEventBus modEventBus) {
        RegistryEvents.CreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        RegistryEvents.ItemRegistry.ITEMS.register(modEventBus);
        RegistryEvents.BlockRegistry.BLOCKS.register(modEventBus);
        RegistryEvents.EffectRegistry.MOB_EFFECTS.register(modEventBus);
        RegistryEvents.PotionRegistry.POTIONS.register(modEventBus);
        RegistryEvents.EnchantmentRegistry.ENCHANTMENT_EFFECT_COMPONENTS.register(modEventBus);

        modEventBus.addListener(ModEvents::addCreativeTab);
        NeoForge.EVENT_BUS.addListener(ModEvents::bucketRightClick);
        NeoForge.EVENT_BUS.register(NsubtleEffects.class);
    }
}
