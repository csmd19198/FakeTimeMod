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
    // Forge 47 的 TOML 读取器把小整数读成 Integer，而 define(..., 0L) 的校验器要求 Long 实例，
    // 且 ConfigValue<Long>.get() 的泛型返回类型会让编译器隐式插入 checkcast Long——值为 0 等
    // 小整数时加载配置即 ClassCastException，导致 m.load() 从不执行、时间基准锚点失效
    // （假时间按现实时钟外推，与服务器时间脱节——太阳位置漂移的根因）。与 1.21.1 版同款修复。
    private static final ForgeConfigSpec.ConfigValue<Long> BASE_TICKS =
            BUILDER.defineInRange("baseTicks", 0L, Long.MIN_VALUE, Long.MAX_VALUE, Long.class);
    private static final ForgeConfigSpec.ConfigValue<Long> ANCHOR_MS =
            BUILDER.defineInRange("anchorMs", 0L, Long.MIN_VALUE, Long.MAX_VALUE, Long.class);
    private static final ForgeConfigSpec.ConfigValue<Long> LOCKED_TICKS =
            BUILDER.defineInRange("lockedTicks", 0L, Long.MIN_VALUE, Long.MAX_VALUE, Long.class);
    private static final ForgeConfigSpec.ConfigValue<Long> LAST_REAL_DAY_TIME =
            BUILDER.defineInRange("lastRealDayTime", 0L, Long.MIN_VALUE, Long.MAX_VALUE, Long.class);
    private static final ForgeConfigSpec.ConfigValue<Long> LAST_UPDATE_MS =
            BUILDER.defineInRange("lastUpdateMs", 0L, Long.MIN_VALUE, Long.MAX_VALUE, Long.class);

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

    /** 读 Long 配置并归一化类型。
     *  注意：直接写 ((Number) CONFIG_VALUE.get()).longValue() 无效——ConfigValue<Long>.get()
     *  的泛型返回类型会让编译器在字节码中隐式插入 checkcast Long，先于 Number 强转抛 CCE。
     *  必须用通配符类型 ConfigValue<?> 取到 Object（无隐式强转）再经 Number 归一化。 */
    private static long readLong(ForgeConfigSpec.ConfigValue<?> cv) {
        return ((Number) cv.get()).longValue();
    }

    public static void load(FakeTimeManager m) {
        m.load(STATE.get(),
                readLong(BASE_TICKS),
                readLong(ANCHOR_MS),
                readLong(LOCKED_TICKS),
                readLong(LAST_REAL_DAY_TIME),
                readLong(LAST_UPDATE_MS));
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
