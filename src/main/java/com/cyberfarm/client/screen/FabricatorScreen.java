package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.FabricatorBlockEntity;
import com.cyberfarm.menu.FabricatorMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The acid tank, three ingredient slots, a progress bar and the circuits out. */
public class FabricatorScreen extends MachineScreen<FabricatorMenu> {
    private static final int TANK_X = 12;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public FabricatorScreen(FabricatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getAcidAmount(), FabricatorBlockEntity.TANK_CAPACITY, menu.getAcidFluid());
        drawProgressBar(graphics, x + 70, y + 40, 46, 6, menu.getProgress(), 0xFF3FBF6F);
        if (menu.getEnergyCapacity() > 0 && menu.getEnergy() < FabricatorBlockEntity.ENERGY_PER_TICK && menu.getProgress() == 0) {
            graphics.text(font, Component.translatable("gui.cyberfarm.no_power"), x + 70, y + 60, WARM, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getAcidAmount(), FabricatorBlockEntity.TANK_CAPACITY, menu.getAcidFluid());
    }
}
