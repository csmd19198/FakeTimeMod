package com.faketime.client.gui;

import com.faketime.FakeTimeFormatter;
import com.faketime.FakeTimeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FakeTimePanelBackground extends AbstractWidget {

    private static final ResourceLocation[] CLOCK_FRAMES = new ResourceLocation[64];
    static {
        for (int i = 0; i < 64; i++) {
            CLOCK_FRAMES[i] = new ResourceLocation(String.format("textures/item/clock_%02d.png", i));
        }
    }

    private final FakeTimeManager manager;

    public FakeTimePanelBackground(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("FakeTimePanel"));
        this.manager = FakeTimeManager.getInstance();
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        // semi-transparent dark background with subtle border
        g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xC0202020);
        g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, 0xC0282828);

        long fake = this.manager.getDisplayTicks();
        long real = this.manager.getLastRealDayTime();

        // Clock: MC 64-frame animated texture, select frame by fake time
        // frame 0 = 6:00 AM, frame 32 = 6:00 PM
        int frame = (int) (fake / (float) FakeTimeManager.DAY_LENGTH * 64.0F) % 64;
        g.blit(CLOCK_FRAMES[frame],
                getX() + 10, getY() + 10, 0, 0, 32, 32, 32, 32);

        // Current time (fake time)
        g.drawString(font, Component.translatable("gui.faketimemod.current_time",
                FakeTimeFormatter.formatTicks(fake) + "  (" + FakeTimeFormatter.formatClock(fake) + ")"),
                getX() + 50, getY() + 12, 0xFFFFFF);

        // Server real time
        g.drawString(font, Component.translatable("gui.faketimemod.server_time",
                FakeTimeFormatter.formatTicks(real) + "  (" + FakeTimeFormatter.formatClock(real) + ")"),
                getX() + 50, getY() + 24, 0x808080);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
    }
}
