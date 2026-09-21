package com.nsubtle.datagen;

import com.nsubtle.events.RegistryEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

import static com.nsubtle.Nsubtle.MOD_ID;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(BlockTags.MINEABLE_WITH_AXE)
                .add(RegistryEvents.BlockRegistry.altar_frame.get());
        this.tag(BlockTags.NEEDS_IRON_TOOL)
                .add(RegistryEvents.BlockRegistry.altar_frame.get());
    }
}
