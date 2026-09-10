package com.nsubtle.events;


import com.nsubtle.effect.NsubtleEffects;
import com.nsubtle.list.ItemList;
import com.nsubtle.list.BlockList;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.nsubtle.Nsubtle;
import com.nsubtle.list.FoodList;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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
        public static final Holder<MobEffect> BROKEN_DEFENSE = MOB_EFFECTS.register("broken_defense",()->new NsubtleEffects.BROKEN_DEFENSE() {
        });
    }

    public static class ItemRegistry{
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
        public static final DeferredItem<Item> soup = ITEMS.registerItem("soup",
                properties -> new Item(properties.rarity(Rarity.RARE)
                        .stacksTo(16).food(FoodList.soup_fp,FoodList.soup)));

        public static final DeferredItem<Item> gold_head = ITEMS.registerItem("gold_head",
                properties ->  new Item(properties.rarity(Rarity.RARE)
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true)
                        .food(FoodList.gold_head_fp,FoodList.gold_head)
                        .stacksTo(16)));

        public static final DeferredItem<Item> explosive_powder = ITEMS.registerItem("explosive_powder",
                Item::new,
                ItemList::explosive_powder
        );

        public static final DeferredItem<BlockItem> altar_frame = ITEMS.registerSimpleBlockItem(BlockRegistry.altar_frame);
    }

    public static class BlockRegistry{
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);

        public static final DeferredBlock<Block>  altar_frame = BLOCKS.registerBlock(
                "altar_frame",
                Block::new,
                BlockList::altar_frame
        );
    }

    public static class PotionRegistry{
        public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, MOD_ID);
        public static final DeferredHolder<Potion,Potion> POTION_BROKEN_DEFENSE = POTIONS.register("broken_defense",
                ()-> new Potion("broken_defense",new MobEffectInstance(EffectRegistry.BROKEN_DEFENSE.getDelegate(),20*30,0)));
    }

    public static class EnchantmentRegistry{
        public static final DeferredRegister<DataComponentType<?>> ENCHANTMENTS = DeferredRegister.create(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, MOD_ID);
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> tough = register("tough", builder -> builder.persistent(Unit.CODEC));

        private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String path, UnaryOperator<DataComponentType.Builder<T>> pOperator) {
            return ENCHANTMENTS.register(path, () -> pOperator.apply(DataComponentType.builder()).build());
        }
    }
}
