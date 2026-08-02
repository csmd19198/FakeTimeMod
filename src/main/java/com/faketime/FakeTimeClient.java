package com.faketime;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FakeTimeMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FakeTimeClient {
    private FakeTimeClient() {}

    private static FakeTimeManager.TimeState lastSavedState = null;
    private static long lastSavedTicks = Long.MIN_VALUE;
    private static long lastSavedBase = Long.MIN_VALUE;
    private static int debugCounter = 0; // TEMP DEBUG: 局域网双账号对比测试用，定位后移除

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        FakeTimeManager manager = FakeTimeManager.getInstance();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            // LevelData 不被注入，永远存服务器真实时间
            long worldDayTime = mc.level.getLevelData().getDayTime();
            manager.updateRealDayTime(worldDayTime);
            // TEMP DEBUG: 每 200 tick(约10s) 打印 世界时间 vs 假时间 的跟踪情况
            if (++debugCounter % 200 == 0) {
                FakeTimeMod.LOGGER.info("[FakeTimeDBG] world={} fake={} approx={} lastReal={} lastUpdateMs={} now={}",
                        worldDayTime, manager.getFakeDayTime(worldDayTime), manager.getRealDayTimeApprox(),
                        manager.getLastRealDayTime(), manager.getLastUpdateMs(), System.currentTimeMillis());
            }
        }
        if (manager.getState() != lastSavedState
                || manager.getLockedTicks() != lastSavedTicks
                || manager.getBaseTicks() != lastSavedBase) {
            lastSavedState = manager.getState();
            lastSavedTicks = manager.getLockedTicks();
            lastSavedBase = manager.getBaseTicks();
            FakeTimeConfig.save(manager);
        }
    }

}
