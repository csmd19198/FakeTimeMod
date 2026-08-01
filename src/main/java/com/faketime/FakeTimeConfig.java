package com.faketime;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class FakeTimeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.EnumValue<FakeTimeManager.TimeState> STATE =
            BUILDER.comment("Time state: FOLLOW (follow server), INDEPENDENT (free-running), LOCKED")
                    .defineEnum("state", FakeTimeManager.TimeState.FOLLOW);
    private static final ForgeConfigSpec.ConfigValue<Long> BASE_TICKS = BUILDER.define("baseTicks", 0L);
    private static final ForgeConfigSpec.ConfigValue<Long> ANCHOR_TICKS = BUILDER.define("anchorTicks", 0L);
    private static final ForgeConfigSpec.ConfigValue<Long> LOCKED_TICKS = BUILDER.define("lockedTicks", 0L);
    private static final ForgeConfigSpec.ConfigValue<Long> CLIENT_TICKS = BUILDER.define("clientTicks", 0L);
    private static final ForgeConfigSpec.ConfigValue<Long> LAST_REAL_DAY_TIME = BUILDER.define("lastRealDayTime", 0L);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private FakeTimeConfig() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }

    public static void save(FakeTimeManager m) {
        STATE.set(m.getState());
        BASE_TICKS.set(m.getBaseTicks());
        ANCHOR_TICKS.set(m.getAnchorTicks());
        LOCKED_TICKS.set(m.getLockedTicks());
        CLIENT_TICKS.set(m.getClientTicks());
        LAST_REAL_DAY_TIME.set(m.getLastRealDayTime());
        SPEC.save();
    }

    public static void load(FakeTimeManager m) {
        m.load(STATE.get(), BASE_TICKS.get(), ANCHOR_TICKS.get(), LOCKED_TICKS.get(),
                CLIENT_TICKS.get(), LAST_REAL_DAY_TIME.get());
    }
}
