package com.nsubtle.events;

import com.nsubtle.Nsubtle;
import com.nsubtle.effect.NsubtleEffects;
import com.nsubtle.list.BlockList;
import com.nsubtle.list.FoodList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 所有 DeferredRegister 的集中定义。
 * <p>
 * 物品/方块/效果/药水/附魔效果组件的具体数值放在 {@code com.nsubtle.list} 包中，
 * 这里只负责注册。
 */
public final class RegistryEvents {

    public static final String MOD_ID = Nsubtle.MOD_ID;

    private RegistryEvents() {
    }

    public static final class CreativeTabs {

        public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
                DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

        public static final Supplier<CreativeModeTab> NSUBTLE = CREATIVE_MODE_TABS.register("nsubtle",
                () -> CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup." + MOD_ID + ".nsubtle"))
                        .icon(() -> new ItemStack(ItemRegistry.GOLD_HEAD.get()))
                        .build());
    }

    public static final class EffectRegistry {

        public static final DeferredRegister<MobEffect> MOB_EFFECTS =
                DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID);

        public static final DeferredHolder<MobEffect, MobEffect> BROKEN_DEFENSE =
                MOB_EFFECTS.register("broken_defense", NsubtleEffects.BROKEN_DEFENSE::new);
    }

    public static final class ItemRegistry {

        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

        public static final DeferredItem<Item> SOUP = ITEMS.registerItem("soup",
                properties -> new Item(properties
                        .rarity(Rarity.RARE)
                        .stacksTo(16)
                        .food(FoodList.SOUP_FOOD, FoodList.SOUP_CONSUMABLE)));

        public static final DeferredItem<Item> GOLD_HEAD = ITEMS.registerItem("gold_head",
                properties -> new Item(properties
                        .rarity(Rarity.RARE)
                        .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                        .food(FoodList.GOLD_HEAD_FOOD, FoodList.GOLD_HEAD_CONSUMABLE)
                        .stacksTo(16)));

        public static final DeferredItem<Item> EXPLOSIVE_POWDER = ITEMS.registerSimpleItem("explosive_powder");

        public static final DeferredItem<BlockItem> ALTAR_FRAME = ITEMS.registerSimpleBlockItem(BlockRegistry.ALTAR_FRAME);
    }

    public static final class BlockRegistry {

        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);

        public static final DeferredBlock<Block> ALTAR_FRAME = BLOCKS.registerBlock(
                "altar_frame",
                Block::new,
                BlockList::altarFrame);
    }

    public static final class PotionRegistry {

        public static final DeferredRegister<Potion> POTIONS =
                DeferredRegister.create(Registries.POTION, MOD_ID);

        public static final DeferredHolder<Potion, Potion> POTION_BROKEN_DEFENSE = POTIONS.register("broken_defense",
                () -> new Potion("broken_defense",
                        new MobEffectInstance(EffectRegistry.BROKEN_DEFENSE, 20 * 30, 0)));
    }

    /**
     * 附魔效果组件（数据驱动附魔的 {@code effects} 字段）。
     */
    public static final class EnchantmentRegistry {

        public static final DeferredRegister<DataComponentType<?>> ENCHANTMENT_EFFECT_COMPONENTS =
                DeferredRegister.create(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, MOD_ID);

        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> TOUGH =
                ENCHANTMENT_EFFECT_COMPONENTS.register("tough",
                        () -> DataComponentType.<Unit>builder().persistent(Unit.CODEC).build());
    }
}
