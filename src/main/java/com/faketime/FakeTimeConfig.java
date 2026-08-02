package com.faketime;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 配置定义与 MOD 总线订阅者：ModConfigEvent 只在 MOD 总线触发（IModBusEvent）。 */
@EventBusSubscriber(modid = FakeTimeMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FakeTimeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.EnumValue<FakeTimeManager.TimeState> STATE =
            BUILDER.comment("Time state: FOLLOW (follow server), INDEPENDENT (free-running), LOCKED")
                    .defineEnum("state", FakeTimeManager.TimeState.FOLLOW);
    // NeoForge 21.1 的 TOML 读取器（nightconfig）把小整数读成 Integer，而 define(..., 0L) 的校验器
    // 要求 Long 实例，导致值为 0 的键被永久判定"不正确"并陷入每秒一次的 Correcting 循环。
    // 改用 defineInRange：其 Range.test 走数值范围检查（doubleValue），Integer/Long 均接受。
    // 工作区按任务计划保留，待 Task 4 实测确认 NeoForge 读取行为后再决定是否简化。
    private static final ModConfigSpec.ConfigValue<Long> BASE_TICKS =
            BUILDER.defineInRange("baseTicks", 0L, Long.MIN_VALUE, Long.MAX_VALUE, Long.class);
    private static final ModConfigSpec.ConfigValue<Long> ANCHOR_MS =
            BUILDER.defineInRange("anchorMs", 0L, Long.MIN_VALUE, Long.MAX_VALUE, Long.class);
    private static final ModConfigSpec.ConfigValue<Long> LOCKED_TICKS =
            BUILDER.defineInRange("lockedTicks", 0L, Long.MIN_VALUE, Long.MAX_VALUE, Long.class);
    private static final ModConfigSpec.ConfigValue<Long> LAST_REAL_DAY_TIME =
            BUILDER.defineInRange("lastRealDayTime", 0L, Long.MIN_VALUE, Long.MAX_VALUE, Long.class);
    private static final ModConfigSpec.ConfigValue<Long> LAST_UPDATE_MS =
            BUILDER.defineInRange("lastUpdateMs", 0L, Long.MIN_VALUE, Long.MAX_VALUE, Long.class);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FakeTimeConfig() {}

    public static void register(ModContainer container) {
        // NeoForge 21.1：ModLoadingContext.get() 已移除，改为 ModContainer 构造器注入
        // （javap 核实 loader-4.0.41.jar: ModContainer.registerConfig(ModConfig$Type, IConfigSpec)）
        container.registerConfig(ModConfig.Type.CLIENT, SPEC);
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
     *  NeoForge 21.1 的 TOML 读取器把 int 范围内的小整数读成 Integer（超出才读成 Long）。
     *  注意：直接写 ((Number) CONFIG_VALUE.get()).longValue() 无效——ConfigValue<Long>.get()
     *  的泛型返回类型会让编译器在字节码中隐式插入 checkcast Long，先于 Number 强转抛 CCE。
     *  必须用通配符类型 ConfigValue<?> 取到 Object（无隐式强转）再经 Number 归一化。 */
    private static long readLong(ModConfigSpec.ConfigValue<?> cv) {
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
