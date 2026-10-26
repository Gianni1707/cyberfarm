package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.PyrolysisOvenBlockEntity;
import com.cyberfarm.menu.PyrolysisOvenMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The methane tank, the String slot, a progress bar and the Carbon Fiber out. */
public class PyrolysisOvenScreen extends MachineScreen<PyrolysisOvenMenu> {
    private static final int TANK_X = 12;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public PyrolysisOvenScreen(PyrolysisOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getMethaneAmount(), PyrolysisOvenBlockEntity.TANK_CAPACITY, menu.getMethaneFluid());
        drawProgressBar(graphics, x + 84, y + 40, 32, 6, menu.getProgress(), 0xFFFF8C1A);
        if (menu.getEnergyCapacity() > 0 && menu.getEnergy() < PyrolysisOvenBlockEntity.ENERGY_PER_TICK && menu.getProgress() == 0) {
            graphics.text(font, Component.translatable("gui.cyberfarm.no_power"), x + 64, y + 60, WARM, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getMethaneAmount(), PyrolysisOvenBlockEntity.TANK_CAPACITY, menu.getMethaneFluid());
    }
}
