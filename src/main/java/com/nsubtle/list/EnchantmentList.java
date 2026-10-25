package com.nsubtle.list;

import com.nsubtle.Nsubtle;
import com.nsubtle.events.RegistryEvents;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 数据驱动附魔的定义（由 datagen 输出到 {@code data/nsubtle/enchantment/}）。
 */
public final class EnchantmentList {

    /** 坚韧：降低“破防”的触发概率。 */
    public static final ResourceKey<Enchantment> TOUGH = key("tough");

    private EnchantmentList() {
    }

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);

        register(context, TOUGH, Enchantment.enchantment(
                Enchantment.definition(
                        items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                        5,
                        4,
                        Enchantment.dynamicCost(5, 7),
                        Enchantment.dynamicCost(12, 7),
                        2,
                        EquipmentSlotGroup.ARMOR
                )
        ).withEffect(RegistryEvents.EnchantmentRegistry.TOUGH.get()));
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(Nsubtle.MOD_ID, path));
    }

    private static void register(BootstrapContext<Enchantment> context,
                                 ResourceKey<Enchantment> key,
                                 Enchantment.Builder builder) {
        context.register(key, builder.build(key.location()));
    }
}
