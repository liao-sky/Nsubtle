package com.nsubtle.list;

import net.minecraft.world.item.Item;

public class ItemList {
    public static Item.Properties explosive_powder(Item.Properties properties) {
        return properties.stacksTo(64);
    }

}
