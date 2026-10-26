package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.ElectrolyzerBlockEntity;
import com.cyberfarm.menu.ElectrolyzerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Alumina in on the left, Aluminum Ingots out on the right. */
public class ElectrolyzerScreen extends MachineScreen<ElectrolyzerMenu> {
    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawProgressBar(graphics, x + 70, y + 40, 36, 6, menu.getProgress(), 0xFFC8D0D8);
        if (menu.getEnergyCapacity() > 0 && menu.getEnergy() < ElectrolyzerBlockEntity.ENERGY_PER_TICK && menu.getProgress() == 0) {
            graphics.text(font, Component.translatable("gui.cyberfarm.no_power"), x + 64, y + 60, WARM, false);
        }
    }
}
