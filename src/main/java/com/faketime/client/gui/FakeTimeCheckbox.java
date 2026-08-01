package com.faketime.client.gui;

import com.faketime.FakeTimeManager;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;

public class FakeTimeCheckbox extends Checkbox {
    private static final FakeTimeManager MANAGER = FakeTimeManager.getInstance();

    public FakeTimeCheckbox(int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("gui.faketimemod.lock"), MANAGER.isLocked(), false);
    }

    @Override
    public void onPress() {
        super.onPress();
        MANAGER.setLocked(this.selected());
    }
}
