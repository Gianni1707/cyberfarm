package com.cyberfarm.port.client;

import org.lwjgl.glfw.GLFW;

/** The 26.x key press, from the 1.20.1 key, scan code and modifiers. */
public record KeyEvent(int key, int scancode, int modifiers) {
    public boolean isConfirmation() {
        return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
    }

    public boolean isEscape() {
        return key == GLFW.GLFW_KEY_ESCAPE;
    }
}
