package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.FabricatorBlockEntity;
import com.cyberfarm.menu.FabricatorMenu;

import com.cyberfarm.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The acid tank, three ingredient slots, a progress bar and the circuits out. */
public class FabricatorScreen extends MachineScreen<FabricatorMenu> {
    private static final int TANK_X = 48;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public FabricatorScreen(FabricatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("fabricator");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getAcidAmount(), FabricatorBlockEntity.TANK_CAPACITY, menu.getAcidFluid());
        float progress = menu.getProgress();
        int ax = x + 5;
        // The print head running over the card, the traces lighting up behind it
        int head = ax + 7 + Math.round(20 * progress);
        if (progress > 0) {
            for (int tx = ax + 6; tx < ax + 30 && tx <= head; tx += 4) {
                graphics.fill(tx, y + 44, tx + 3, y + 45, ACCENT);
                graphics.fill(tx + 1, y + 46, tx + 2, y + 48, 0xFFC9B458);
            }
        }
        drawSteelBar(graphics, head - 3, y + 23, head + 5, y + 33);
        drawSteelRod(graphics, head, y + 33, head + 2, y + 38);
        graphics.fill(head, y + 38, head + 2, y + 39, ACCENT);
        if (menu.getEnergyCapacity() > 0 && menu.getEnergy() < FabricatorBlockEntity.ENERGY_PER_TICK && progress == 0) {
            drawNotice(graphics, x + 124, y + 56, Component.translatable("gui.cyberfarm.no_power"), WARM);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getAcidAmount(), FabricatorBlockEntity.TANK_CAPACITY, menu.getAcidFluid());
    }
}
