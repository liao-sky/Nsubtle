package com.nsubtle.list;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class BlockList {
    public static BlockBehaviour.Properties altar_frame(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.WOOD)
            .strength(2.0f,2.0f)
            .sound(SoundType.WOOD)
            .requiresCorrectToolForDrops();}
}
