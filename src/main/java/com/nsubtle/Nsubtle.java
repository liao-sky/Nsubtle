package com.nsubtle;

import com.mojang.logging.LogUtils;
import com.nsubtle.events.EventsLoader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 模组入口。
 * <p>
 * 这里只保留最必要的初始化：把注册与事件挂到总线上，其余逻辑分散在
 * {@code events}、{@code effect}、{@code list} 等包中。
 */
@Mod(Nsubtle.MOD_ID)
public class Nsubtle {

    public static final String MOD_ID = "nsubtle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Nsubtle(IEventBus modEventBus) {
        EventsLoader.loadEvents(modEventBus);
    }
}
