package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.CrystallizerBlockEntity;
import com.cyberfarm.menu.CrystallizerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The acid tank, the isotope and amethyst slots, a progress bar and the crystal out. */
public class CrystallizerScreen extends MachineScreen<CrystallizerMenu> {
    private static final int TANK_X = 12;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public CrystallizerScreen(CrystallizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getAcidAmount(), CrystallizerBlockEntity.TANK_CAPACITY, menu.getAcidFluid());
        drawProgressBar(graphics, x + 70, y + 40, 36, 6, menu.getProgress(), 0xFFB070FF);
        if (!menu.isFormed()) {
            graphics.text(font, Component.translatable("gui.cyberfarm.crystallizer.incomplete"), x + 40, y + 60, WARM, false);
        } else if (menu.getEnergyCapacity() > 0 && menu.getEnergy() < CrystallizerBlockEntity.ENERGY_PER_TICK && menu.getProgress() == 0) {
            graphics.text(font, Component.translatable("gui.cyberfarm.no_power"), x + 64, y + 60, WARM, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getAcidAmount(), CrystallizerBlockEntity.TANK_CAPACITY, menu.getAcidFluid());
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index <= CrystallizerBlockEntity.OUTPUT) {
            String key = switch (hoveredSlot.index) {
                case CrystallizerBlockEntity.ISOTOPE -> "isotope";
                case CrystallizerBlockEntity.AMETHYST -> "amethyst";
                default -> "crystal";
            };
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.crystallizer.slot." + key), mouseX, mouseY);
        }
    }
}
