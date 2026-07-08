package com.nsubtle.datagen;

import com.nsubtle.Nsubtle;
import com.nsubtle.list.EnchantmentList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Nsubtle.MOD_ID)
public class ModEnchantmentProvider {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(true, new ModDatapackProvider(
                packOutput,
                event.getLookupProvider(),
                Set.of(Nsubtle.MOD_ID)
        ));
        generator.addProvider(true, new ModEnchantmentTagProvider(packOutput,
                lookupProvider));
    }

    public static class ModDatapackProvider extends DatapackBuiltinEntriesProvider {
        public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
                .add(Registries.ENCHANTMENT, EnchantmentList::bootstrap);

        public ModDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, Set<String> modIds) {
            super(output, registries, BUILDER, modIds);
        }
    }


    private static class ModEnchantmentTagProvider extends EnchantmentTagsProvider {
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