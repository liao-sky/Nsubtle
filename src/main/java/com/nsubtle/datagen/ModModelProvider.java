package com.nsubtle.datagen;

import com.nsubtle.Nsubtle;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;
import com.nsubtle.events.RegistryEvents.*;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output,Nsubtle.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ItemRegistry.soup.get(),ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ItemRegistry.gold_head.get(),ModelTemplates.FLAT_ITEM);

    }

}
