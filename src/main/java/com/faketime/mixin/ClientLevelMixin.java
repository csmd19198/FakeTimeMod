package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    /** 4 个渲染时间方法内部对 getTimeOfDay 的调用点重定向为假时间。
     *  原 LevelTimeAccess.getTimeOfDay 实现忽略 partialTick 返回 (dayTime%24000)/24000。 */
    @Redirect(method = {"getSkyDarken", "getSkyColor", "getCloudColor", "getStarBrightness"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private float faketime_getTimeOfDay(ClientLevel self, float partialTick) {
        long fake = FakeTimeManager.getInstance().getFakeDayTime(self.getLevelData().getDayTime());
        return (float) (fake % 24000L) / 24000.0F;
    }
}
