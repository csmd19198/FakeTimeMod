package com.faketime;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** 配置定义与 MOD 总线订阅者：ModConfigEvent 只在 MOD 总线触发（IModBusEvent）。 */
@Mod.EventBusSubscriber(modid = FakeTimeMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FakeTimeConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.EnumValue<FakeTimeManager.TimeState> STATE =
            BUILDER.comment("Time state: FOLLOW (follow server), INDEPENDENT (free-running), LOCKED")
                    .defineEnum("state", FakeTimeManager.TimeState.FOLLOW);
    private static final ForgeConfigSpec.ConfigValue<Long> BASE_TICKS = BUILDER.define("baseTicks", 0L);
    private static final ForgeConfigSpec.ConfigValue<Long> ANCHOR_MS = BUILDER.define("anchorMs", 0L);
    private static final ForgeConfigSpec.ConfigValue<Long> LOCKED_TICKS = BUILDER.define("lockedTicks", 0L);
    private static final ForgeConfigSpec.ConfigValue<Long> LAST_REAL_DAY_TIME = BUILDER.define("lastRealDayTime", 0L);
    private static final ForgeConfigSpec.ConfigValue<Long> LAST_UPDATE_MS = BUILDER.define("lastUpdateMs", 0L);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private FakeTimeConfig() {}

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC);
    }

    public static void save(FakeTimeManager m) {
        try {
            STATE.set(m.getState());
            BASE_TICKS.set(m.getBaseTicks());
            ANCHOR_MS.set(m.getAnchorMs());
            LOCKED_TICKS.set(m.getLockedTicks());
            LAST_REAL_DAY_TIME.set(m.getLastRealDayTime());
            LAST_UPDATE_MS.set(m.getLastUpdateMs());
            // ConfigValue.set 内部已触发 autosave 写盘；SPEC.save() 冗余且增加写盘次数，不再调用
        } catch (Exception e) {
            // 配置写盘失败（瞬时 I/O 问题）不应崩溃游戏，仅记录警告；下次变化时重试
            FakeTimeMod.LOGGER.warn("Failed to save faketimemod config: {}", e.toString());
        }
    }

    public static void load(FakeTimeManager m) {
        m.load(STATE.get(), BASE_TICKS.get(), ANCHOR_MS.get(), LOCKED_TICKS.get(),
                LAST_REAL_DAY_TIME.get(), LAST_UPDATE_MS.get());
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            load(FakeTimeManager.getInstance());
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            load(FakeTimeManager.getInstance());
        }
    }
}
