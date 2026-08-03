package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 雾的时间源修复：FogRenderer.setupColor 直接调用 getTimeOfDay 计算雾亮度与
 *  日落光晕参数——若不走假时间，服务器晚上时雾被压暗，与假白天的亮天空
 *  形成地平线黑边/红边。 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    // 复刻 vanilla getTimeOfDay 的平滑曲线（FakeTimeManager.getTimeOfDay），保持雾与昼夜一致。
    @Redirect(method = "setupColor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private static float faketime_getTimeOfDay(ClientLevel level, float partialTick) {
        long fake = FakeTimeManager.getInstance().getFakeDayTime(level.getLevelData().getDayTime());
        return FakeTimeManager.getTimeOfDay(fake);
    }
}
