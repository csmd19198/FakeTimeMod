package com.faketime.client.gui;

import com.faketime.FakeTimeManager;
import com.faketime.FakeTimeFormatter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class TimeSlider extends AbstractSliderButton {
    private final FakeTimeManager manager = FakeTimeManager.getInstance();
    private boolean dragging = false;

    public TimeSlider(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal(""), 0.0D);
        syncValue();
    }

    private void syncValue() {
        this.value = this.manager.getDisplayTicks() / (double) FakeTimeManager.DAY_LENGTH;
        this.updateMessage();
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.dragging) this.syncValue(); // slider follows time flow when not dragging
        super.renderWidget(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateMessage() {
        long ticks = this.manager.getDisplayTicks();
        this.setMessage(FakeTimeFormatter.formatTicks(ticks));
    }

    @Override
    protected void applyValue() {
        this.dragging = true;
        this.manager.dragTo((long) (this.value * FakeTimeManager.DAY_LENGTH));
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        this.dragging = false;
        this.syncValue();
        super.onRelease(mouseX, mouseY);
    }
}
