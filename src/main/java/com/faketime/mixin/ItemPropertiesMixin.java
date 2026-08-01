package com.faketime.mixin;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemProperties.class)
public class ItemPropertiesMixin {

    /** 静态块末尾覆盖 CLOCK 的 time 属性函数：钟表指针改读服务器真实时间。
     *  原函数读 level.getTimeOfDay()（已被注入为假时间），此处覆盖为读 LevelData 真值。 */
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void faketime_overrideClock(CallbackInfo ci) {
        ItemProperties.register(Items.CLOCK, new ResourceLocation("time"),
                (stack, level, entity, seed) -> {
                    if (level == null) return 0.0F;
                    long realTicks = level.getLevelData().getDayTime();
                    return Math.floorMod(realTicks, 24000L) / 24000.0F;
                });
    }
}
