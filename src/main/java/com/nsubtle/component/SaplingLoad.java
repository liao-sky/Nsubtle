package com.nsubtle.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 弩上的装填进度。骨粉在前、树苗在后，所以组件存在本身就代表“骨粉已装入”：
 * <ul>
 *   <li>{@code sapling} 为空 → 只装了骨粉，还差树苗；</li>
 *   <li>{@code sapling} 有值 → 装填完成，可以发射。</li>
 * </ul>
 * 组件挂在武器物品上，会随物品栏同步与存档。
 */
public record SaplingLoad(ItemStack sapling) {

    public static final Codec<SaplingLoad> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("sapling").forGetter(SaplingLoad::sapling)
    ).apply(instance, SaplingLoad::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaplingLoad> STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.map(SaplingLoad::new, SaplingLoad::sapling);

    /** 只装入骨粉的初始状态。 */
    public static SaplingLoad powderOnly() {
        return new SaplingLoad(ItemStack.EMPTY);
    }

    public boolean hasSapling() {
        return !this.sapling.isEmpty();
    }
}
