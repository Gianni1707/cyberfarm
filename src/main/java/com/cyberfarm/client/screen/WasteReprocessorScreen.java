package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.WasteReprocessorBlockEntity;
import com.cyberfarm.menu.WasteReprocessorMenu;
import com.cyberfarm.registry.ModItems;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

/** Waste in on the left, Vitrified Waste, recovered uranium and isotope shards out on the right. */
public class WasteReprocessorScreen extends MachineScreen<WasteReprocessorMenu> {
    public WasteReprocessorScreen(WasteReprocessorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("waste_reprocessor");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        float progress = menu.getProgress();
        int ax = x + 5;
        // The spent rod pushed against the disc, shortening; green dust falling onto the pile
        int rod = Math.round(18 * (1 - progress));
        if (rod > 1) {
            graphics.fill(ax + 4, y + 33, ax + 4 + rod, y + 40, WELL);
            graphics.fill(ax + 4, y + 34, ax + 4 + rod, y + 39, STEEL_EDGE);
            graphics.fill(ax + 4, y + 34, ax + 4 + rod, y + 35, STEEL_LOW);
            graphics.fill(ax + 4, y + 36, ax + 4 + rod, y + 37, 0xFF62D13A);
        }
        int disc = ax + 4 + rod;
        graphics.fill(disc, y + 28, disc + 3, y + 46, STEEL_LIGHT);
        graphics.fill(disc, y + 28, disc + 1, y + 46, STEEL_TOP);
        if (progress > 0) {
            long now = Util.getMillis();
            for (int i = 0; i < 5; i++) {
                int dust = y + 40 + (int) ((now + i * 210L) % 800 * 10 / 800);
                graphics.fill(disc + 4 + i * 2, dust, disc + 5 + i * 2, dust + 2, 0xFF8FE86A);
            }
        }
        int pile = 2 + Math.round(7 * progress);
        graphics.fill(ax + 4, y + 57 - pile, ax + 32, y + 57, 0xFF3F8A2A);
        graphics.fill(ax + 7, y + 56 - pile, ax + 29, y + 57 - pile, 0xFF62D13A);
        drawGhost(graphics, WasteReprocessorBlockEntity.INPUT, new ItemStack(ModItems.DEPLETED_FUEL_ROD.get()));
        drawGhost(graphics, WasteReprocessorBlockEntity.WASTE_OUTPUT, new ItemStack(ModItems.VITRIFIED_WASTE.get()));
        drawGhost(graphics, WasteReprocessorBlockEntity.URANIUM_OUTPUT, new ItemStack(ModItems.URANIUM_INGOT.get()));
        drawGhost(graphics, WasteReprocessorBlockEntity.ISOTOPE_OUTPUT, new ItemStack(ModItems.ISOTOPE_SHARD.get()));
        // The menu loads with no energy capacity data for a moment, so only warn once it is known
        if (menu.getEnergyCapacity() > 0 && menu.getEnergy() < WasteReprocessorBlockEntity.ENERGY_PER_TICK && progress == 0) {
            drawNotice(graphics, x + 80, y + 56, Component.translatable("gui.cyberfarm.no_power"), WARM);
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
