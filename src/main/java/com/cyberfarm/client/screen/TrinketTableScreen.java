package com.cyberfarm.client.screen;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.menu.TrinketTableMenu;

import com.cyberfarm.port.client.GuiGraphicsExtractor;
import com.cyberfarm.port.client.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Trinket Table's grid in the machines' enamel (tools/guigen.py, trinket_table_gui()): it works like a crafting
 * table but looks like CyberFarm. The chip on the left blinks its LEDs in turn; when the grid makes something, a pulse
 * of light runs down the pipe on the right towards the result.
 */
public class TrinketTableScreen extends AbstractContainerScreen<TrinketTableMenu> {
    private static final ResourceLocation BACKGROUND = CyberFarm.id("textures/gui/trinket_table.png");
    private static final int MINT = 0xFF5EC2B8, TEXT = 0xFFD8F8FF, GLOW = 0xFFD1FEE7, OFF = 0xFF055B57;
    /** The chip's LEDs, and the pipe the pulse runs along: as guigen.py draws them (TABLE_CHIP, TABLE_PIPE). */
    private static final int[][] LEDS = {{5, 39}, {8, 39}, {11, 39}};
    private static final int PIPE_X0 = 155, PIPE_X1 = 159, PIPE_TOP = 14, PIPE_BRANCH = 42;

    public TrinketTableScreen(TrinketTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        long ms = Util.getMillis();
        // The chip's LEDs, one after another
        int lit = (int) (ms / 300 % LEDS.length);
        for (int i = 0; i < LEDS.length; i++) {
            int x = leftPos + LEDS[i][0], y = topPos + LEDS[i][1];
            graphics.fill(x, y, x + 2, y + 1, i == lit ? GLOW : OFF);
        }
        // Something to take: a pulse down the pipe and into the result
        if (menu.getSlot(0).hasItem()) {
            int y = topPos + PIPE_TOP + (int) (ms / 40 % (PIPE_BRANCH - PIPE_TOP));
            graphics.fill(leftPos + PIPE_X0, y, leftPos + PIPE_X1, y + 2, GLOW);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, MINT, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }
}
