package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    // 复刻 vanilla getTimeOfDay 的 -6000 刻偏移（skyTicks），保持昼夜与滑块一致。
    @Redirect(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private float faketime_sky_getTimeOfDay(ClientLevel self, float partialTick) {
        long fake = FakeTimeManager.getInstance().getFakeDayTime(self.getLevelData().getDayTime());
        return FakeTimeManager.skyTicks(fake) / 24000.0F;
    }

    /** 月相（月亮贴图选择）用假时间计算。DimensionType.moonPhase = (int)(p/24000L%8L+8L)%8 */
    @Redirect(method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMoonPhase()I"))
    private int faketime_sky_getMoonPhase(ClientLevel self) {
        long fakeFull = FakeTimeManager.getInstance().getFakeFullDayTime(self.getLevelData().getDayTime());
        return (int) (fakeFull / 24000L % 8L + 8L) % 8;
    }
}
