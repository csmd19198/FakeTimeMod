package com.faketime;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = FakeTimeMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FakeTimeClient {
    private FakeTimeClient() {}

    private static FakeTimeManager.TimeState lastSavedState = null;
    private static long lastSavedTicks = Long.MIN_VALUE;
    private static long lastSavedBase = Long.MIN_VALUE;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        FakeTimeManager manager = FakeTimeManager.getInstance();
        manager.onTick();
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

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == FakeTimeConfig.SPEC) {
            FakeTimeConfig.load(FakeTimeManager.getInstance());
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == FakeTimeConfig.SPEC) {
            FakeTimeConfig.load(FakeTimeManager.getInstance());
        }
    }
}
