package com.faketime.client.gui;

import com.faketime.FakeTimeFormatter;
import com.faketime.FakeTimeManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FakeTimeScreen extends Screen {
    private static final FakeTimeManager MANAGER = FakeTimeManager.getInstance();
    private static final int CLOCK_COLOR = 0xFFFFFFFF;
    private static final int TICK_RADIUS = 13;   // 12 个刻度点的圆周半径
    private static final int HAND_LENGTH = 11;   // 时针长度

    /** 面板背景纹理（可被资源包覆盖：assets/faketimemod/textures/gui/panel.png）。 */
    private static final ResourceLocation PANEL_TEXTURE =
            new ResourceLocation("faketimemod", "textures/gui/panel.png");
    private static final int PANEL_TEX_W = 440;   // 纹理宽（2× 面板尺寸）
    private static final int PANEL_TEX_H = 280;   // 纹理高

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

        drawPanelTexture(g, PANEL_TEXTURE, x, y, panelW, panelH,
                PANEL_TEX_W, PANEL_TEX_H);

        long fake = MANAGER.getDisplayTicks();
        long real = MANAGER.getRealDayTimeApprox(); // 外推近似：界面打开时也随现实时间流动

        this.drawAnalogClock(g, x + 26, y + 26, fake);

        g.drawString(this.font, Component.translatable("gui.faketimemod.current_time",
                FakeTimeFormatter.formatTicks(fake).append("  (").append(
                        Component.literal(FakeTimeFormatter.formatClock(fake))).append(")")),
                x + 50, y + 12, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("gui.faketimemod.server_time",
                FakeTimeFormatter.formatTicks(real).append("  (").append(
                        Component.literal(FakeTimeFormatter.formatClock(real))).append(")")),
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

    /** 绘制面板背景纹理：整图等比缩小到面板尺寸。
     *  用 11 参数 blit(tex, x, y, width, height, u, v, uW, vH, texW, texH)——屏幕
     *  区域 (x,y) 尺寸 width×height，从纹理 (u,v) 取 uW×vH，两者解耦实现真正缩放：
     *  屏幕 220×140 取纹理全部 440×280（2:1 缩小）。注意参数顺序是 x,y,width,height
     *  （字节码验证：x2=x+width, y2=y+height）——之前误传 x,width,y,height 导致
     *  绘制区域错位。9 参数版取图尺寸==屏幕尺寸不可用。资源包可覆盖同名纹理。 */
    private static void drawPanelTexture(GuiGraphics g, ResourceLocation tex,
                                         int x, int y, int width, int height,
                                         int texW, int texH) {
        g.blit(tex, x, y, width, height, 0.0F, 0.0F, texW, texH, texW, texH);
    }
}
