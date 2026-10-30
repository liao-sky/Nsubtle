package com.nsubtle.client;

import com.nsubtle.Nsubtle;
import com.nsubtle.events.RegistryEvents;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端专用注册。
 * <p>
 * 树苗投掷物沿用原版 {@link ThrownItemRenderer}：它会把实体同步的 ItemStack 按
 * {@code ItemDisplayContext.GROUND} 渲染出来，所以不管装的是哪种树苗，飞在空中的
 * 都是对应树苗的物品模型。
 */
@EventBusSubscriber(modid = Nsubtle.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RegistryEvents.EntityRegistry.SAPLING_PROJECTILE.get(),
                ThrownItemRenderer::new);
    }
}
