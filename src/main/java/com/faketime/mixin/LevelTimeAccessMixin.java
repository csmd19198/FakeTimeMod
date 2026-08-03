package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 光影（Iris/Oculus）兼容的关键修复：dayTime() 接口默认方法注入。
 *  vanilla 的 LevelTimeAccess.getTimeOfDay(F) = dimensionType().timeOfDay(this.dayTime())，
 *  其中 dayTime() 由 LevelAccessor 提供默认实现，直接读 getLevelData().getDayTime()
 *  （真实时间）——**绕过了 Level.getDayTime 的注入**。Iris 的 CelestialUniforms/
 *  ShadowRenderer 直接调用 ClientLevel.getTimeOfDay(F) 方法本体（而非 renderSky 等
 *  具体调用点），因此太阳贴图位置正确（走 renderSky 的调用点重定向）但光影的
 *  sunPosition/shadowAngle 等 uniform 仍按真实时间计算，阳光方向"停在原地"。
 *  本注入在 dayTime() 默认方法本体返回假时间，getTimeOfDay/getMoonBrightness/
 *  moonPhase 等所有经 dayTime() 的渲染时间源自动生效（ClientLevel/ServerLevel
 *  均未 override dayTime()）。只对客户端 Level 生效，服务端逻辑不受影响。 */
@Mixin(LevelAccessor.class)
public abstract class LevelTimeAccessMixin {

    @Inject(method = "dayTime", at = @At("HEAD"), cancellable = true)
    private void faketime_dayTime(CallbackInfoReturnable<Long> cir) {
        if (((Level) (Object) this).isClientSide) {
            Level level = (Level) (Object) this;
            long fake = FakeTimeManager.getInstance().getFakeDayTime(level.getLevelData().getDayTime());
            cir.setReturnValue(fake);
        }
    }
}
