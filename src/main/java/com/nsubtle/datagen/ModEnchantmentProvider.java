package com.nsubtle.datagen;

import com.nsubtle.Nsubtle;
import com.nsubtle.list.EnchantmentList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModEnchantmentProvider {

    public static class ModDatapackProvider extends DatapackBuiltinEntriesProvider {
        public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
                .add(Registries.ENCHANTMENT, EnchantmentList::bootstrap);

        public ModDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, Set<String> modIds) {
            super(output, registries, BUILDER, modIds);
        }
    }


    public static class ModEnchantmentTagProvider extends EnchantmentTagsProvider {
        public ModEnchantmentTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider, Nsubtle.MOD_ID);
        }

        @Override
        protected void addTags(HolderLookup.@NotNull Provider pProvider) {
            //tag(EnchantmentTags.CURSE).addOptional(EnchantmentRegistry.AFFECTION)
            //tag(EnchantmentTags.TREASURE).addOptional(EnchantmentRegistry.AFFECTION)
            tag(EnchantmentTags.NON_TREASURE).addOptional(EnchantmentList.TOUGH);
        }
    }
}