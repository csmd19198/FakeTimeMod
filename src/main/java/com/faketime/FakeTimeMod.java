package com.faketime;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FakeTimeMod.MODID)
public class FakeTimeMod {
    public static final String MODID = "faketimemod";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public FakeTimeMod(FMLJavaModLoadingContext context) {
        FakeTimeConfig.register(context);
    }
}
