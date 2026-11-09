package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.PyrolysisOvenBlockEntity;
import com.cyberfarm.menu.PyrolysisOvenMenu;
import com.cyberfarm.registry.ModItems;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;

/** The methane tank, the String slot, a progress bar and the Carbon Fiber out. */
public class PyrolysisOvenScreen extends MachineScreen<PyrolysisOvenMenu> {
    private static final int TANK_X = 48;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public PyrolysisOvenScreen(PyrolysisOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("pyrolysis_oven");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getMethaneAmount(), PyrolysisOvenBlockEntity.TANK_CAPACITY, menu.getMethaneFluid());
        float progress = menu.getProgress();
        int ax = x + 5;
        // The strand crossing the tube, black behind the glowing front
        int front = ax + 5 + Math.round(26 * progress);
        graphics.fill(ax + 5, y + 27, front, y + 30, 0xFF14100E);
        graphics.fill(front, y + 27, ax + 31, y + 30, 0xFFEDEBE2);
        if (progress > 0) {
            graphics.fill(Math.max(ax + 5, front - 2), y + 26, front + 1, y + 31, 0xFFFF8C1A);
            // The burners under it, each flame on its own beat
            long beat = Util.getMillis() / 110;
            for (int i = 0; i < 5; i++) {
                int fx = ax + 5 + i * 6;
                int height = 6 + (int) ((beat + i * 3L) % 5);
                graphics.fill(fx, y + 58 - height, fx + 4, y + 58, 0xFFFF8C1A);
                graphics.fill(fx + 1, y + 60 - height, fx + 3, y + 58, 0xFFFFD04D);
            }
        }
        drawGhost(graphics, PyrolysisOvenBlockEntity.STRING, new ItemStack(Items.STRING));
        drawGhost(graphics, PyrolysisOvenBlockEntity.OUTPUT, new ItemStack(ModItems.CARBON_FIBER.get()));
        if (menu.getEnergyCapacity() > 0 && menu.getEnergy() < PyrolysisOvenBlockEntity.ENERGY_PER_TICK && progress == 0) {
            drawNotice(graphics, x + 112, y + 56, Component.translatable("gui.cyberfarm.no_power"), WARM);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getMethaneAmount(), PyrolysisOvenBlockEntity.TANK_CAPACITY, menu.getMethaneFluid());
    }
}
