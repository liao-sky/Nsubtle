package com.nsubtle.datagen;

import com.nsubtle.Nsubtle;
import com.nsubtle.events.RegistryEvents;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

public class ModModelProvider extends ModelProvider {

    public ModModelProvider(PackOutput output) {
        super(output, Nsubtle.MOD_ID);
    }

    @Override
    protected void registerModels(@NotNull BlockModelGenerators blockModels, @NotNull ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(RegistryEvents.ItemRegistry.SOUP.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RegistryEvents.ItemRegistry.GOLD_HEAD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(RegistryEvents.ItemRegistry.EXPLOSIVE_POWDER.get(), ModelTemplates.FLAT_ITEM);
        blockModels.createTrivialCube(RegistryEvents.BlockRegistry.ALTAR_FRAME.get());
    }
}
