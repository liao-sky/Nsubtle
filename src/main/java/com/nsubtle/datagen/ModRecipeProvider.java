package com.nsubtle.datagen;

import com.nsubtle.events.RegistryEvents;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static com.nsubtle.Nsubtle.MOD_ID;

public class ModRecipeProvider extends RecipeProvider {
    private final HolderGetter<Item> items;

    public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput recipeOutput) {
        super(registries, recipeOutput);

        items = registries.lookupOrThrow(Registries.ITEM);
    }

    @Override
    protected void buildRecipes() {
        ShapedRecipeBuilder.shaped(items, RecipeCategory.FOOD, RegistryEvents.ItemRegistry.soup.asItem(),2)
                .pattern("LGL")
                .pattern("GBG")
                .pattern("LGL")
                .define('L', Items.LAPIS_LAZULI)
                .define('G', Items.GOLD_NUGGET)
                .define('B', Items.BOWL)
                .unlockedBy("has_lapis_lazuli", has(Items.LAPIS_LAZULI))
                .save(output);

        ShapedRecipeBuilder.shaped(items, RecipeCategory.FOOD, RegistryEvents.ItemRegistry.gold_head.asItem(),2)
                .pattern("LGL")
                .pattern("GBG")
                .pattern("LGL")
                .define('L', Items.LAPIS_LAZULI)
                .define('G', Items.GOLD_INGOT)
                .define('B', Items.GOLDEN_APPLE)
                .unlockedBy("has_lapis_lazuli", has(Items.LAPIS_LAZULI))
                .save(output);
    }


    public HolderGetter<Item> getItems() {
        return items;
    }

    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected @NotNull RecipeProvider createRecipeProvider(HolderLookup.@NotNull Provider provider, @NotNull RecipeOutput recipeOutput) {
            return new ModRecipeProvider(provider, recipeOutput);
        }

        @Override
        public @NotNull String getName() {
            return MOD_ID;
        }

    }
}
