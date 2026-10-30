package com.nsubtle.mixin;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 取 {@link SaplingBlock} 的 {@code treeGrower}。
 * <p>
 * 树苗投掷物不摆树苗、直接调用 {@link TreeGrower#growTree} 长树，
 * 这样落地是石头/沙子也能长（原版 {@code growTree} 会先把目标位置清成空气再放树），
 * 不再受“树苗必须种在泥土上”的限制。
 */
@Mixin(SaplingBlock.class)
public interface SaplingBlockAccessor {

    @Accessor("treeGrower")
    TreeGrower nsubtle$treeGrower();
}
