package com.faketime;

import net.minecraftforge.fml.common.Mod;

@Mod(FakeTimeMod.MODID)
public class FakeTimeMod {
    public static final String MODID = "faketimemod";

    public FakeTimeMod() {
        FakeTimeConfig.register();
    }
}
