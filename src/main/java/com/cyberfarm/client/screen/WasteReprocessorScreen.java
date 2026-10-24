package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.WasteReprocessorBlockEntity;
import com.cyberfarm.menu.WasteReprocessorMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Waste in on the left, Vitrified Waste, recovered uranium and isotope shards out on the right. */
public class WasteReprocessorScreen extends MachineScreen<WasteReprocessorMenu> {
    public WasteReprocessorScreen(WasteReprocessorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawProgressBar(graphics, x + 68, y + 40, 28, 6, menu.getProgress(), 0xFF62D13A);
        // The menu loads with no energy capacity data for a moment, so only warn once it is known
        if (menu.getEnergyCapacity() > 0 && menu.getEnergy() < WasteReprocessorBlockEntity.ENERGY_PER_TICK && menu.getProgress() == 0) {
            graphics.text(font, Component.translatable("gui.cyberfarm.no_power"), x + 62, y + 60, WARM, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index <= WasteReprocessorBlockEntity.ISOTOPE_OUTPUT) {
            String key = switch (hoveredSlot.index) {
                case WasteReprocessorBlockEntity.INPUT -> "input";
                case WasteReprocessorBlockEntity.WASTE_OUTPUT -> "waste";
                case WasteReprocessorBlockEntity.URANIUM_OUTPUT -> "uranium";
                default -> "isotope";
            };
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.reprocessor.slot." + key), mouseX, mouseY);
        }
    }
}
