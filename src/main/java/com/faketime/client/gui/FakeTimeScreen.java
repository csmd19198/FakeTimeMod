package com.faketime.client.gui;

import com.faketime.FakeTimeFormatter;
import com.faketime.FakeTimeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FakeTimeScreen extends Screen {
    private static final FakeTimeManager MANAGER = FakeTimeManager.getInstance();
    private static final ResourceLocation[] CLOCK_FRAMES = new ResourceLocation[64];

    static {
        for (int i = 0; i < 64; i++) {
            CLOCK_FRAMES[i] = new ResourceLocation(String.format("textures/item/clock_%02d.png", i));
        }
    }

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
        long real = MANAGER.getLastRealDayTime();

        int frame = (int) (fake / (float) FakeTimeManager.DAY_LENGTH * 64.0F) % 64;
        g.blit(CLOCK_FRAMES[frame], x + 10, y + 10, 0, 0, 32, 32, 32, 32);

        g.drawString(this.font, Component.translatable("gui.faketimemod.current_time",
                FakeTimeFormatter.formatTicks(fake) + "  (" + FakeTimeFormatter.formatClock(fake) + ")"),
                x + 50, y + 12, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("gui.faketimemod.server_time",
                FakeTimeFormatter.formatTicks(real) + "  (" + FakeTimeFormatter.formatClock(real) + ")"),
                x + 50, y + 24, 0x808080);

        super.render(g, mouseX, mouseY, partialTick);
    }
}
