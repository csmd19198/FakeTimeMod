package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    /** getSkyColor/getCloudColor/getStarBrightness/getSkyDarken 内部对 getTimeOfDay 的调用点
     *  重定向为假时间。必须用 vanilla 的平滑曲线（FakeTimeManager.getTimeOfDay，非线性），
     *  而非线性 skyTicks——线性近似在黎明/黄昏与 vanilla 相差最多 ~840 刻，
     *  正是太阳/天空/光照位置偏移的根因。getSkyDarken 走 vanilla 原公式（重定向后内部
     *  的 getTimeOfDay 即假时间），夜间暗度与白天亮度均与 vanilla 在假时间一致。 */
    @Redirect(method = {"getSkyColor", "getCloudColor", "getStarBrightness", "getSkyDarken"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private float faketime_getTimeOfDay(ClientLevel self, float partialTick) {
        long fake = FakeTimeManager.getInstance().getFakeDayTime(self.getLevelData().getDayTime());
        return FakeTimeManager.getTimeOfDay(fake);
    }
}
