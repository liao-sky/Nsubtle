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

public class ModEvents {

    @SubscribeEvent
    public static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if(event.getTab() == RegistryEvents.CreativeTabs.NSUBTLE.get()){
            event.acceptAll(RegistryEvents.ItemRegistry.ITEMS.getEntries().stream()
                    .map(holder->holder.get().getDefaultInstance())
                    .toList());
        }

    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void bucketRightClick(PlayerInteractEvent.RightClickBlock event) {
            if (event.isCanceled()) {
                return;
            }
            Level level = event.getLevel();
            if (level.isClientSide()) {
                return;
            }
            ItemStack mainHandItem = event.getItemStack();
            if (!mainHandItem.is(ItemTags.HOES)) {
                return;
            }
            ItemStack offhandItem = event.getEntity().getOffhandItem();
            if (!offhandItem.is(Items.WATER_BUCKET)) {
                return;
            }
            BlockPos pos = event.getPos();
            BlockState targetBlock = level.getBlockState(pos);
            Block farmlandBlock = Blocks.FARMLAND;

            if (!targetBlock.is(Blocks.DIRT) && !targetBlock.is(Blocks.GRASS_BLOCK) && !targetBlock.is(Blocks.DIRT_PATH)) {
                return;
            }
            BlockState wetFarmland = farmlandBlock.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
            level.setBlock(pos, wetFarmland, 3); // Block.UPDATE_ALL

            event.setCanceled(true);
        }
}
