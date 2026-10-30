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

public final class ModEnchantmentProvider {

    private ModEnchantmentProvider() {
    }

    /** 生成 {@code data/nsubtle/enchantment/*.json}。 */
    public static class ModDatapackProvider extends DatapackBuiltinEntriesProvider {

        public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
                .add(Registries.ENCHANTMENT, EnchantmentList::bootstrap);

        public ModDatapackProvider(PackOutput output,
                                   CompletableFuture<HolderLookup.Provider> registries,
                                   Set<String> modIds) {
            super(output, registries, BUILDER, modIds);
        }
    }

    /** 生成附魔标签：坚韧属于非宝藏附魔，生机属于宝藏附魔。 */
    public static class ModEnchantmentTagProvider extends EnchantmentTagsProvider {

        public ModEnchantmentTagProvider(PackOutput output,
                                         CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider, Nsubtle.MOD_ID);
        }

        @Override
        protected void addTags(HolderLookup.@NotNull Provider provider) {
            tag(EnchantmentTags.NON_TREASURE).addOptional(EnchantmentList.TOUGH);

            // 生机：只从战利品/交易获得，不进附魔台。
            tag(EnchantmentTags.TREASURE).addOptional(EnchantmentList.VITALITY);
            tag(EnchantmentTags.ON_RANDOM_LOOT).addOptional(EnchantmentList.VITALITY);
            tag(EnchantmentTags.TRADEABLE).addOptional(EnchantmentList.VITALITY);
        }
    }
}
