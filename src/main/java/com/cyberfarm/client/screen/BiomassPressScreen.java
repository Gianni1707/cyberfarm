package com.cyberfarm.client.screen;

import com.cyberfarm.menu.BiomassPressMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Organic waste in on the left, Biomass out on the right. */
public class BiomassPressScreen extends MachineScreen<BiomassPressMenu> {
    public BiomassPressScreen(BiomassPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.text(font, Component.translatable("gui.cyberfarm.press.input"), x + 8, y + 22, TEXT_DIM, false);
        drawProgressBar(graphics, x + 70, y + 40, 36, 6, menu.getProgress(), 0xFF6FBF3F);
    }
}
