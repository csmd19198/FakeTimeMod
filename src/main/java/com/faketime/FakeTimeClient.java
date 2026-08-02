package com.faketime;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = FakeTimeMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class FakeTimeClient {
    private FakeTimeClient() {}

    private static FakeTimeManager.TimeState lastSavedState = null;
    private static long lastSavedTicks = Long.MIN_VALUE;
    private static long lastSavedBase = Long.MIN_VALUE;

    // NeoForge 21.1 无 phase 字段，改用 ClientTickEvent.Post（等价于原 END 阶段）
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        FakeTimeManager manager = FakeTimeManager.getInstance();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            // LevelData 不被注入，永远存服务器真实时间
            manager.updateRealDayTime(mc.level.getLevelData().getDayTime());
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
