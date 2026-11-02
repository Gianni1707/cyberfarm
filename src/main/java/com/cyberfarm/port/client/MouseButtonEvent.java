package com.cyberfarm.port.client;

import net.minecraft.client.gui.screens.Screen;

/** The 26.x mouse click, from the 1.20.1 position and button. */
public record MouseButtonEvent(double x, double y, int button) {
    public boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }
}
