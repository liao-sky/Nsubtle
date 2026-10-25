package com.nsubtle.list;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 方块属性表。
 */
public final class BlockList {

    private BlockList() {
    }

    public static BlockBehaviour.Properties altarFrame(BlockBehaviour.Properties properties) {
        return properties
                .mapColor(MapColor.WOOD)
                .strength(1.0F, 2.0F)
                .sound(SoundType.WOOD)
                .requiresCorrectToolForDrops();
    }
}
