package com.nsubtle;

import com.nsubtle.datagen.EnUsLangProvider;
import com.nsubtle.datagen.ModBlockTagProvider;
import com.nsubtle.datagen.ModEnchantmentProvider;
import com.nsubtle.datagen.ModLootTableProvider;
import com.nsubtle.datagen.ModModelProvider;
import com.nsubtle.datagen.ModRecipeProvider;
import com.nsubtle.datagen.ZnChLangProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 数据生成入口。
 * <p>
 * 客户端资源（模型、方块状态、语言）输出到 {@code src/generated/resources/client}，
 * 服务端数据（配方、战利品、附魔、标签）输出到 {@code src/generated/resources/server}。
 * 两个目录都会作为资源目录打进模组 jar。
 */
@EventBusSubscriber(modid = Nsubtle.MOD_ID)
public final class DataGen {

    private DataGen() {
    }

    @SubscribeEvent
    public static void onClientGatherData(GatherDataEvent.Client event) {
        event.createProvider(ModModelProvider::new);
        event.createProvider(EnUsLangProvider::new);
        event.createProvider(ZnChLangProvider::new);
    }

    @SubscribeEvent
    public static void onServerGatherData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        event.createProvider(ModRecipeProvider.Runner::new);
        event.createProvider(ModLootTableProvider::new);

        generator.addProvider(true, new ModEnchantmentProvider.ModDatapackProvider(
                output,
                lookupProvider,
                Set.of(Nsubtle.MOD_ID)
        ));
        generator.addProvider(true, new ModEnchantmentProvider.ModEnchantmentTagProvider(output, lookupProvider));
        generator.addProvider(true, new ModBlockTagProvider(output, lookupProvider));
    }
}
