package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.BioDigesterBlockEntity;
import com.cyberfarm.menu.BioDigesterMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BioDigesterScreen extends MachineScreen<BioDigesterMenu> {
    private static final int TANK_X = 112;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public BioDigesterScreen(BioDigesterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.text(font, Component.translatable("gui.cyberfarm.digester.input"), x + 8, y + 22, TEXT_DIM, false);
        drawProgressBar(graphics, x + 68, y + 41, 34, 6, menu.getProgress(), 0xFF6FBF3F);
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getTankAmount(), BioDigesterBlockEntity.TANK_CAPACITY, menu.getTankFluid());
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getTankAmount(), BioDigesterBlockEntity.TANK_CAPACITY, menu.getTankFluid());
    }
}
