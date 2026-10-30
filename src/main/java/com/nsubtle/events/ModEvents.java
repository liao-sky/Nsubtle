package com.nsubtle.events;

import com.nsubtle.component.SaplingLoad;
import com.nsubtle.list.EnchantmentList;
import com.nsubtle.util.SaplingCharge;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

public final class ModEvents {

    private ModEvents() {
    }

    /**
     * 把所有已注册物品放进本模组的创造模式标签页，并附一本「生机」附魔书方便测试。
     */
    @SubscribeEvent
    public static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == RegistryEvents.CreativeTabs.NSUBTLE.get()) {
            event.acceptAll(RegistryEvents.ItemRegistry.ITEMS.getEntries().stream()
                    .map(holder -> holder.get().getDefaultInstance())
                    .toList());
            event.accept(vitalityBook(event));
        }
    }

    /** 造一本 1 级「生机」附魔书（宝藏附魔无法从附魔台获得，放在创造标签页里方便取用/测试）。 */
    private static ItemStack vitalityBook(BuildCreativeModeTabContentsEvent event) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        event.getParameters().holders()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(EnchantmentList.VITALITY)
                .ifPresent(holder -> {
                    ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    enchantments.set(holder, 1);
                    book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
                });
        return book;
    }

    /**
     * 主手锄 + 副手水桶右键泥土类方块：直接变成湿润耕地。
     * <p>
     * 目前不消耗水、也不损耗锄，属于刻意保留的“取巧”机制。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void bucketRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) {
            return;
        }

        ItemStack mainHand = event.getItemStack();
        if (!mainHand.is(ItemTags.HOES)) {
            return;
        }
        if (!event.getEntity().getOffhandItem().is(Items.WATER_BUCKET)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState target = event.getLevel().getBlockState(pos);
        if (!target.is(Blocks.DIRT) && !target.is(Blocks.GRASS_BLOCK) && !target.is(Blocks.DIRT_PATH)) {
            return;
        }

        Level level = event.getLevel();
        BlockState wetFarmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
        level.setBlock(pos, wetFarmland, Block.UPDATE_ALL);
        event.setCanceled(true);
    }

    /**
     * 弓/弩的装填提示：弩显示装到哪一步，弓显示当前能不能打树苗炸弹。
     */
    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        SaplingLoad load = stack.get(RegistryEvents.DataComponentRegistry.SAPLING_LOAD.get());

        if (load != null) {
            List<Component> tooltip = event.getToolTip();
            tooltip.add(Component.empty());
            if (load.hasSapling()) {
                tooltip.add(Component.translatable("tooltip.nsubtle.sapling_loaded", load.sapling().getHoverName())
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.nsubtle.sapling_ready")
                        .withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(Component.translatable("tooltip.nsubtle.powder_loaded")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.nsubtle.powder_needs_sapling")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }

        Player player = event.getEntity();
        if (stack.is(Items.BOW) && player != null && SaplingCharge.canUseBow(player.level(), player, stack)) {
            event.getToolTip().add(Component.translatable("tooltip.nsubtle.bow_sapling")
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
