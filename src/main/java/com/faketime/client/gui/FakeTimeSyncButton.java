package com.faketime.client.gui;

import com.faketime.FakeTimeManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class FakeTimeSyncButton extends Button {
    private static final FakeTimeManager MANAGER = FakeTimeManager.getInstance();

    public FakeTimeSyncButton(int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("gui.faketimemod.sync"),
                btn -> MANAGER.syncToServer(), Button.DEFAULT_NARRATION);
    }
}
