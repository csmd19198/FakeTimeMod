package com.faketime.client.gui;

import com.faketime.FakeTimeFormatter;
import com.faketime.FakeTimeManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FakeTimeScreen extends Screen {
    private static final FakeTimeManager MANAGER = FakeTimeManager.getInstance();
    private static final int CLOCK_COLOR = 0xFFFFFFFF;
    private static final int TICK_RADIUS = 13;   // 12 个刻度点的圆周半径
    private static final int HAND_LENGTH = 11;   // 时针长度

    public FakeTimeScreen() {
        super(Component.literal("FakeTime"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelW = 220;
        int panelH = 140;
        int x = cx - panelW / 2;
        int y = cy - panelH / 2;

        this.addRenderableWidget(new TimeSlider(x + 10, y + 46, panelW - 20, 20));
        this.addRenderableWidget(new FakeTimeCheckbox(x + 10, y + 72, 100, 20));
        this.addRenderableWidget(new FakeTimeSyncButton(x + 128, y + 72, 82, 20));
        this.addRenderableWidget(Button.builder(Component.translatable("gui.faketimemod.close"),
                b -> this.onClose()).bounds(cx - 40, y + panelH - 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelW = 220;
        int panelH = 140;
        int x = cx - panelW / 2;
        int y = cy - panelH / 2;

        g.fill(x, y, x + panelW, y + panelH, 0xC0202020);
        g.fill(x + 1, y + 1, x + panelW - 1, y + panelH - 1, 0xC0282828);

        long fake = MANAGER.getDisplayTicks();
        long real = MANAGER.getRealDayTimeApprox(); // 外推近似：界面打开时也随现实时间流动

        this.drawAnalogClock(g, x + 26, y + 26, fake);

        g.drawString(this.font, Component.translatable("gui.faketimemod.current_time",
                FakeTimeFormatter.formatTicks(fake) + "  (" + FakeTimeFormatter.formatClock(fake) + ")"),
                x + 50, y + 12, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("gui.faketimemod.server_time",
                FakeTimeFormatter.formatTicks(real) + "  (" + FakeTimeFormatter.formatClock(real) + ")"),
                x + 50, y + 24, 0x808080);

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** 简约钟表：12 个刻度点 + 一根时针。fakeTicks 按 1000 刻 = 1 小时换算（0 刻 = 6:00）。 */
    private void drawAnalogClock(GuiGraphics g, int cx, int cy, long fakeTicks) {
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0;
            int px = cx + (int) Math.round(Math.sin(a) * TICK_RADIUS);
            int py = cy - (int) Math.round(Math.cos(a) * TICK_RADIUS);
            g.fill(px - 1, py - 1, px + 2, py + 2, CLOCK_COLOR); // 3x3 刻度点
        }

        long hours24 = Math.floorMod(6 + fakeTicks / 1000L, 24L);
        float minutes = (fakeTicks % 1000L) * 60.0F / 1000.0F;
        float angle = ((hours24 % 12) + minutes / 60.0F) / 12.0F * 360.0F - 90.0F;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        g.fill(0, -1, HAND_LENGTH, 1, CLOCK_COLOR); // 时针（沿旋转后 +X 方向的细线）
        pose.popPose();

        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, CLOCK_COLOR); // 中心点
    }
}
