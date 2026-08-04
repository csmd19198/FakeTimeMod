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

    /** 面板背景纹理（九宫格，可被资源包覆盖：assets/faketimemod/textures/gui/panel.png）。 */
    private static final ResourceLocation PANEL_TEXTURE =
            new ResourceLocation("faketimemod", "textures/gui/panel.png");
    private static final int PANEL_TEX_W = 440;   // 纹理宽（2× 面板尺寸）
    private static final int PANEL_TEX_H = 280;   // 纹理高
    private static final int PANEL_BORDER = 6;    // 九宫格边框宽度（深色描边+高光）

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

        drawNineSliced(g, PANEL_TEXTURE, x, y, panelW, panelH,
                PANEL_BORDER, PANEL_BORDER, PANEL_BORDER, PANEL_BORDER,
                PANEL_TEX_W, PANEL_TEX_H);

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

    /** 九宫格绘制面板纹理：四角固定尺寸，四边+中心重复填充。
     *  四角用 9 参数 blit(tex, x, y, u, v, width, height, texW, texH)（不缩放），
     *  四边/中心用 10 参数 blitRepeating（平铺重复，与 vanilla blitNineSliced 同法）。
     *  vanilla blitNineSliced 内部硬编码 256×256 UV，对 440×280 纹理不适用，
     *  故手动实现并传真实纹理尺寸。 */
    private static void drawNineSliced(GuiGraphics g, ResourceLocation tex,
                                       int x, int y, int width, int height,
                                       int left, int top, int right, int bottom,
                                       int texW, int texH) {
        int rightEdge = x + width - right;
        int bottomEdge = y + height - bottom;
        int uRight = texW - right;
        int vBottom = texH - bottom;
        int midW = texW - left - right;   // 纹理中部块宽
        int midH = texH - top - bottom;   // 纹理中部块高
        int screenMidW = rightEdge - x - left;  // 屏幕中部区域宽
        int screenMidH = bottomEdge - y - top;  // 屏幕中部区域高

        // 四角（不缩放）
        g.blit(tex, x, y, 0, 0, left, top, texW, texH);
        g.blit(tex, rightEdge, y, uRight, 0, right, top, texW, texH);
        g.blit(tex, x, bottomEdge, 0, vBottom, left, bottom, texW, texH);
        g.blit(tex, rightEdge, bottomEdge, uRight, vBottom, right, bottom, texW, texH);

        // 上/下边（水平平铺）
        if (screenMidW > 0) {
            g.blitRepeating(tex, x + left, y, screenMidW, top, left, 0, midW, top, texW, texH);
            g.blitRepeating(tex, x + left, bottomEdge, screenMidW, bottom, left, vBottom, midW, bottom, texW, texH);
        }

        // 左/右边（垂直平铺）
        if (screenMidH > 0) {
            g.blitRepeating(tex, x, y + top, left, screenMidH, 0, top, left, midH, texW, texH);
            g.blitRepeating(tex, rightEdge, y + top, right, screenMidH, uRight, top, right, midH, texW, texH);
        }

        // 中心（双向平铺）
        if (screenMidW > 0 && screenMidH > 0) {
            g.blitRepeating(tex, x + left, y + top, screenMidW, screenMidH, left, top, midW, midH, texW, texH);
        }
    }
}
