package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    /** 仅客户端：getDayTime 返回假时间（F3/isNight 等 Level 方法调用者）。 */
    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void faketime_getDayTime(CallbackInfoReturnable<Long> cir) {
        if (((Level) (Object) this).isClientSide) {
            cir.setReturnValue(FakeTimeManager.getInstance().getFakeDayTime(((Level) (Object) this).getLevelData().getDayTime()));
        }
    }

    /** 仅客户端：getSunAngle 返回假时间计算的太阳/月亮角度（Oculus 光影兼容）。
     *  getSunAngle(F) = getTimeOfDay(F) * 2*PI，其中 getTimeOfDay = frac(dayTime/24000 - 0.25)
     *  （忽略 partialTick）。Mixin 不支持接口方法注入，因此直接在此计算方法内用假时间替换，
     *  并复刻 vanilla 的 -6000 刻偏移（skyTicks）。 */
    @Inject(method = "getSunAngle", at = @At("HEAD"), cancellable = true)
    private void faketime_getSunAngle(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (((Level) (Object) this).isClientSide) {
            long fake = FakeTimeManager.getInstance().getFakeDayTime(((Level) (Object) this).getLevelData().getDayTime());
            float timeOfDay = FakeTimeManager.skyTicks(fake) / 24000.0F;
            cir.setReturnValue(timeOfDay * ((float) Math.PI * 2.0F));
        }
    }
}
