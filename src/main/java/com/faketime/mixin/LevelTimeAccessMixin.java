package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;

/** 光影（Iris/Oculus）兼容的关键修复：为 Level 提供 dayTime() 实现。
 *  vanilla 的 LevelTimeAccess.getTimeOfDay(F) = dimensionType().timeOfDay(this.dayTime())，
 *  其中 dayTime() 由 LevelAccessor 提供默认实现，直接读 getLevelData().getDayTime()
 *  （真实时间）——**绕过了 Level.getDayTime 的注入**。Iris 的 CelestialUniforms/
 *  ShadowRenderer 直接调用 ClientLevel.getTimeOfDay(F) 方法本体（而非 renderSky 等
 *  具体调用点），因此太阳贴图位置正确（走 renderSky 的调用点重定向）但光影的
 *  sunPosition/shadowAngle 等 uniform 仍按真实时间计算，阳光方向"停在原地"。
 *  本 mixin 通过接口实现注入（Interface Implementation Injection）：目标是 Level
 *  （类，绕开 interface-mixin 限制），mixin 类 implements LevelAccessor 并提供
 *  dayTime() 实现，Mixin 将其注入 Level 覆盖接口默认方法——getTimeOfDay/
 *  getMoonBrightness/moonPhase 等所有经 dayTime() 的渲染时间源自动生效。
 *  只对客户端 Level 返回假时间，服务端逻辑不受影响。 */
@Mixin(Level.class)
public abstract class LevelTimeAccessMixin implements LevelAccessor {

    @Override
    public long dayTime() {
        Level level = (Level) (Object) this;
        if (level.isClientSide) {
            return FakeTimeManager.getInstance().getFakeDayTime(level.getLevelData().getDayTime());
        }
        return level.getLevelData().getDayTime();
    }
}
