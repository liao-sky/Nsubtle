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

public class EnchantmentList {
    public static final ResourceKey<Enchantment> TOUGH = key("tough");


    public static void bootstrap(BootstrapContext<Enchantment>  context) {
        HolderGetter<Item> itemHolderGetter = context.lookup(Registries.ITEM);

        register(
                context,
                TOUGH,
                Enchantment.enchantment(
                        Enchantment.definition(
                                itemHolderGetter.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                                5,
                                4,
                                Enchantment.dynamicCost(5, 7),
                                Enchantment.dynamicCost(12, 7),
                                2,
                                EquipmentSlotGroup.ARMOR
                        )
                ).withEffect(RegistryEvents.EnchantmentRegistry.tough.get()));
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Nsubtle.MOD_ID, path));
    }

    private static void register(BootstrapContext<Enchantment> context, ResourceKey<Enchantment> resourceKey,
                                 Enchantment.Builder builder) {
        context.register(resourceKey, builder.build(resourceKey.location()));
    }
}
