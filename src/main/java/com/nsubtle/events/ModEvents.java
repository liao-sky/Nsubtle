package com.nsubtle.events;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class ModEvents {

    private ModEvents() {
    }

    /**
     * 把所有已注册物品放进本模组的创造模式标签页。
     */
    @SubscribeEvent
    public static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == RegistryEvents.CreativeTabs.NSUBTLE.get()) {
            event.acceptAll(RegistryEvents.ItemRegistry.ITEMS.getEntries().stream()
                    .map(holder -> holder.get().getDefaultInstance())
                    .toList());
        }
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
}
