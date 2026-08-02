package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    /** getSkyColor/getCloudColor/getStarBrightness 内部对 getTimeOfDay 的调用点重定向为假时间。
     *  原 LevelTimeAccess.getTimeOfDay = frac(dayTime/24000 - 0.25)（忽略 partialTick），
     *  必须复刻其 -6000 刻偏移（skyTicks），否则天空昼夜比 vanilla 超前 6000 刻
     *  （表现为滑块 0-6000 与 18000-24000 是白天、6000-18000 是黑夜）。 */
    @Redirect(method = {"getSkyColor", "getCloudColor", "getStarBrightness"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"))
    private float faketime_getTimeOfDay(ClientLevel self, float partialTick) {
        long fake = FakeTimeManager.getInstance().getFakeDayTime(self.getLevelData().getDayTime());
        return FakeTimeManager.skyTicks(fake) / 24000.0F;
    }

    /** 自定义昼夜暗度曲线：解决假时间晚上时地面太亮的问题。
     *  原版 getSkyDarken 只取决于 skyAngle（时间），但方块亮度 = skyLight(0-15) × darken，
     *  假晚上时 skyLight 仍是白天服务端值（如 15），导致 15×0.36 >> 4×0.36（真实晚上）。
     *  新曲线：夜间暗度固定 0.2，白天 0.2~1.0 平滑过渡，消除 skyLight 差异带来的亮度差。 */
    @Inject(method = "getSkyDarken", at = @At("HEAD"), cancellable = true)
    private void faketime_getSkyDarken(float partialTick, CallbackInfoReturnable<Float> cir) {
        ClientLevel self = (ClientLevel) (Object) this;
        long fake = FakeTimeManager.getInstance().getFakeDayTime(self.getLevelData().getDayTime());
        float t = (float) (fake % 24000L) / 24000.0F;              // 0=6AM, 0.25=正午, 0.5=18:00, 0.75=午夜
        float sun = (float) Math.cos((t - 0.25F) * 2.0F * (float) Math.PI);  // 正午=1, 午夜=-1
        float darken = 0.2F + 0.8F * Math.max(0.0F, sun);          // 白天 0.2~1.0, 夜间 0.2
        darken *= 1.0F - self.getRainLevel(partialTick) * 5.0F / 16.0F;
        darken *= 1.0F - self.getThunderLevel(partialTick) * 5.0F / 16.0F;
        cir.setReturnValue(darken);
    }
}
