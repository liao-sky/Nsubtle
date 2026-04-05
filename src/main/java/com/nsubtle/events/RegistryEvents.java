package com.nsubtle.events;


import com.nsubtle.effect.NsubtleEffect;
import com.nsubtle.effect.NsubtleEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.nsubtle.Nsubtle;
import com.nsubtle.list.FoodList;

import java.util.function.Supplier;

@EventBusSubscriber
public class RegistryEvents {
    public static String MOD_ID = Nsubtle.MOD_ID;

    public static class CreativeTabs{
        public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
        public static final Supplier<CreativeModeTab> NSUBTLE =
        CREATIVE_MODE_TABS.register("nsubtle",
                ()->CreativeModeTab.builder().title(Component.translatable("itemGroup."+MOD_ID+".nsubtle"))
                        .icon(()->new ItemStack(ItemRegistry.gold_head.get())).build());
    }

    public static class EffectRegistry{
        public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT,MOD_ID);
        public static final Holder<MobEffect> BREAK_DEFENSE = MOB_EFFECTS.register("break_defense",()->new NsubtleEffects.BREAK_DEFENSE() {
        });
    }

    public static class ItemRegistry{
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
        public static final DeferredItem<Item> soup = ITEMS.registerItem("soup",
                properties -> new Item(properties.rarity(Rarity.RARE)
                        .stacksTo(1).food(FoodList.soup_fp,FoodList.soup)
                        ));

        public static final DeferredItem<Item> gold_head = ITEMS.registerItem("gold_head",
                properties ->  new Item(properties.rarity(Rarity.RARE)
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true)
                        .food(FoodList.gold_head_fp,FoodList.gold_head)
                        .stacksTo(16)));
    }
}
