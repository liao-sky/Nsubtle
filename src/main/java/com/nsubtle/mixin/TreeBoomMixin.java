package com.nsubtle.mixin;

import com.nsubtle.util.TreeBoomHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 骨粉催熟树苗时引发一次小型爆炸。
 * <p>
 * 实现方式：{@link SaplingBlock#performBonemeal} 执行期间记录“正在催熟”的上下文，
 * 等 {@link TreeGrower#growTree} 真正返回 {@code true} 时再爆炸。这样：
 * <ul>
 *   <li>自然生长的树不会爆炸（没有催熟上下文）；</li>
 *   <li>催熟失败（空间不足、2x2 未凑齐等）不会爆炸；</li>
 *   <li>不再需要全局 BlockPos 标记表，也就没有跨维度串味和长期残留的问题。</li>
 * </ul>
 */
public final class TreeBoomMixin {

    private TreeBoomMixin() {
    }

    @Mixin(SaplingBlock.class)
    public static class SaplingBlockMixin {

        @Inject(method = "performBonemeal", at = @At("HEAD"))
        private void nsubtle$beginBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state,
                                           CallbackInfo ci) {
            TreeBoomHelper.beginBonemeal(level, pos);
        }

        @Inject(method = "performBonemeal", at = @At("RETURN"))
        private void nsubtle$endBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state,
                                         CallbackInfo ci) {
            TreeBoomHelper.endBonemeal();
        }
    }

    @Mixin(TreeGrower.class)
    public static class TreeGrowerMixin {

        @Inject(method = "growTree", at = @At("RETURN"))
        private void nsubtle$explodeOnSuccessfulGrowth(ServerLevel level, ChunkGenerator generator, BlockPos pos,
                                                       BlockState state, RandomSource random,
                                                       CallbackInfoReturnable<Boolean> cir) {
            TreeBoomHelper.onTreeGrown(level, pos, state, cir.getReturnValueZ());
        }
    }
}
