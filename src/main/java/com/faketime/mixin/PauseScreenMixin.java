package com.faketime.mixin;

import com.faketime.client.gui.FakeTimeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 暂停界面右上角添加时间调整入口按钮，点击打开 FakeTimeScreen 独立界面。
 *
 * 注入目标为 Screen 而非 PauseScreen：Mixin AP (0.8.5) 无法通过 PauseScreen
 * 类层级解析继承的 addRenderableWidget 泛型方法映射；以 instanceof PauseScreen
 * 守卫保证只对暂停界面生效。只添加单一入口按钮，旧的三列表大面板已整体迁入
 * FakeTimeScreen，解除了交互失效与遮挡问题。
 */
@Mixin(Screen.class)
public abstract class PauseScreenMixin {

    @Shadow
    private List<GuiEventListener> children;

    @Shadow
    private List<NarratableEntry> narratables;

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
    private void faketime_addEntryButton(CallbackInfo ci) {
        if (!((Object) this instanceof PauseScreen)) return;

        Screen screen = (Screen) (Object) this;
        Button entryButton = Button.builder(Component.translatable("gui.faketimemod.open"),
                b -> Minecraft.getInstance().setScreen(new FakeTimeScreen()))
                .bounds(screen.width - 80, 5, 75, 20).build();

        screen.renderables.add(entryButton);
        this.children.add(entryButton);
        this.narratables.add(entryButton);
    }
}
