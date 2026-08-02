package com.faketime;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FakeTimeMod.MODID)
public class FakeTimeMod {
    public static final String MODID = "faketimemod";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public FakeTimeMod(IEventBus bus, ModContainer container) {
        FakeTimeConfig.register(container);
    }
}
