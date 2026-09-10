package com.nsubtle.datagen;

import com.nsubtle.events.RegistryEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ModBlockLootProvider extends BlockLootSubProvider {
    protected ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(),registries);
    }

    @Override
    protected void generate() {
        dropSelf(RegistryEvents.BlockRegistry.altar_frame.get());
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return Set.of(RegistryEvents.BlockRegistry.altar_frame.get());
    }
}
