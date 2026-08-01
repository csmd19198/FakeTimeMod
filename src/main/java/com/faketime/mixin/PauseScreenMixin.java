package com.faketime.mixin;

import com.faketime.client.gui.FakeTimeCheckbox;
import com.faketime.client.gui.FakeTimePanelBackground;
import com.faketime.client.gui.FakeTimeSyncButton;
import com.faketime.client.gui.TimeSlider;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 注入目标为 Screen 而非 PauseScreen：Mixin AP (0.8.5) 无法通过 PauseScreen
 * 类层级解析继承的 addRenderableWidget 泛型方法映射；以 instanceof PauseScreen
 * 守卫保证只对暂停界面生效。
 */
@Mixin(Screen.class)
public abstract class PauseScreenMixin {

    @Shadow
    private List<GuiEventListener> children;

    @Shadow
    private List<NarratableEntry> narratables;

    @Inject(method = "init", at = @At("TAIL"))
    private void faketime_addPanel(CallbackInfo ci) {
        if (!((Object) this instanceof PauseScreen)) return;

        Screen screen = (Screen) (Object) this;
        int panelWidth = 220;
        int panelHeight = 116;
        int x = (screen.width - panelWidth) / 2;
        int y = screen.height - panelHeight - 12;

        FakeTimePanelBackground bg = new FakeTimePanelBackground(x, y, panelWidth, panelHeight);
        screen.renderables.add(bg);
        this.children.add(bg);
        this.narratables.add(bg);

        TimeSlider slider = new TimeSlider(x + 10, y + 46, panelWidth - 20, 20);
        screen.renderables.add(slider);
        this.children.add(slider);
        this.narratables.add(slider);

        FakeTimeCheckbox checkbox = new FakeTimeCheckbox(x + 10, y + 72, 100, 20);
        screen.renderables.add(checkbox);
        this.children.add(checkbox);
        this.narratables.add(checkbox);

        FakeTimeSyncButton syncBtn = new FakeTimeSyncButton(x + 128, y + 72, 82, 20);
        screen.renderables.add(syncBtn);
        this.children.add(syncBtn);
        this.narratables.add(syncBtn);
    }
}
