package com.nsubtle.datagen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class DataToolKit {

    /**
     * 添加附魔名称到语言文件
     * @param provider LanguageProvider 实例（通常是 this）
     * @param key 附魔的 ResourceKey
     * @param name 附魔的显示名称
     */
    public static void addEnchantment(LanguageProvider provider, ResourceKey<Enchantment> key, String name) {
        ResourceLocation location = key.location();
        provider.add("enchantment." + location.getNamespace() + "." + location.getPath(), name);
    }

    /**
     * 添加附魔描述到语言文件
     * @param provider LanguageProvider 实例（通常是 this）
     * @param key 附魔的 ResourceKey
     * @param description 附魔描述文本
     */
    public static void addEnchantmentDescription(LanguageProvider provider, ResourceKey<Enchantment> key, String description) {
        ResourceLocation location = key.location();
        provider.add("enchantment." + location.getNamespace() + "." + location.getPath() + ".desc", description);
    }
}
