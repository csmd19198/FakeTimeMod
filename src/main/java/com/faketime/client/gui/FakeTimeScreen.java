package com.faketime.client.gui;

import com.faketime.FakeTimeFormatter;
import com.faketime.FakeTimeManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Renderable;
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
            ResourceLocation.fromNamespaceAndPath("faketimemod", "textures/gui/panel.png");
    private static final int PANEL_TEX_W = 440;   // 纹理宽（2× 面板尺寸）
    private static final int PANEL_TEX_H = 280;   // 纹理高

    public FakeTimeScreen() {
        super(Component.literal("FakeTime"));
    }

    /**
     * PauseScreenMixin 入口按钮的打开回调。
     *
     * 必须是本类的静态方法而非注入处 lambda 直接 new：Mixin 会把注入代码内的
     * lambda 搬迁进目标类（Screen），若搬迁后的 lambda 直接引用本类（Screen 子类），
     * 在 Forge 模块化类加载器下会因 Screen 类身份分裂导致 VerifyError。经由本静态
     * 方法中转后，搬迁进 Screen 的 lambda 只引用普通 mod 类（不涉继承层级），可正常验证。
     */
    public static void open(Button button) {
        Minecraft.getInstance().setScreen(new FakeTimeScreen());
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
        // 1.21.1: Checkbox 不可被继承（构造器为包私有），改为 builder + onValueChange 回调
        this.addRenderableWidget(Checkbox.builder(Component.translatable("gui.faketimemod.lock"), this.font)
                .pos(x + 10, y + 72)
                .selected(MANAGER.isLocked())
                .onValueChange((checkbox, selected) -> MANAGER.setLocked(selected))
                .build());
        this.addRenderableWidget(new FakeTimeSyncButton(x + 128, y + 72, 82, 20));
        this.addRenderableWidget(Button.builder(Component.translatable("gui.faketimemod.close"),
                b -> this.onClose()).bounds(cx - 40, y + panelH - 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
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
                FakeTimeFormatter.formatTicks(fake) + "  (" + FakeTimeFormatter.formatClock(fake) + ")"),
                x + 50, y + 12, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("gui.faketimemod.server_time",
                FakeTimeFormatter.formatTicks(real) + "  (" + FakeTimeFormatter.formatClock(real) + ")"),
                x + 50, y + 24, 0x808080);

        // 1.21.1 的原版 Screen.render 自身会调用 renderBackground（含 processBlurEffect 模糊 pass）。
        // 若走 super.render，第二次模糊会把上方已画好的面板/钟表/文字再次模糊（按钮因在模糊
        // 之后绘制而保持清晰——正是实测所见）。故手动遍历 renderables，与原版 render 的
        // 组件循环等价，避免二次模糊。
        for (Renderable renderable : this.renderables) {
            renderable.render(g, mouseX, mouseY, partialTick);
        }
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
     *  （width/height 在 y 之后）——之前误传为 x,width,y,height 导致纹理画到屏幕下方
     *  且宽度错乱。9 参数版取图尺寸==屏幕尺寸不可用。资源包可覆盖同名纹理。 */
    private static void drawPanelTexture(GuiGraphics g, ResourceLocation tex,
                                         int x, int y, int width, int height,
                                         int texW, int texH) {
        g.blit(tex, x, y, width, height, 0.0F, 0.0F, texW, texH, texW, texH);
    }
}
