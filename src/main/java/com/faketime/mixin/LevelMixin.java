package com.faketime.mixin;

import com.faketime.FakeTimeManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    /** 仅客户端：getDayTime 返回假时间（渲染/天空/光影统一入口）。 */
    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void faketime_getDayTime(CallbackInfoReturnable<Long> cir) {
        if (((Level) (Object) this).isClientSide) {
            cir.setReturnValue(FakeTimeManager.getInstance().getFakeDayTime(((Level) (Object) this).getLevelData().getDayTime()));
        }
    }
}
