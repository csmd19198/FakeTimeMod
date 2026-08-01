package com.faketime;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FakeTimeMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FakeTimeClient {
    private FakeTimeClient() {}

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
    }
}
