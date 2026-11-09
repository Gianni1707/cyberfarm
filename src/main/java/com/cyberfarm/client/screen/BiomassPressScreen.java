package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.BiomassPressBlockEntity;
import com.cyberfarm.menu.BiomassPressMenu;
import com.cyberfarm.registry.ModItems;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The press drawn at the side: its plate comes down on the heap of mash as the work goes on. */
public class BiomassPressScreen extends MachineScreen<BiomassPressMenu> {
    private static final int ART_X = 8;
    private static final int ART_Y = 16;
    private static final int RAM_TRAVEL = 22;
    private static final ItemStack WASTE = new ItemStack(Items.WHEAT);

    public BiomassPressScreen(BiomassPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("biomass_press");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        float progress = menu.getProgress();
        boolean powered = menu.getEnergy() >= BiomassPressBlockEntity.ENERGY_PER_TICK;
        int ax = x + ART_X;
        int ay = y + ART_Y;
        // The mash, flattened as the plate comes down
        int heap = (int) (14 - 7 * progress);
        for (int i = 0; i < 20; i++) {
            int height = Math.max(2, (int) (heap - Math.abs(i - 9.5F) * heap / 13));
            int foot = y + 62;
            graphics.fill(ax + 5 + i, foot - height, ax + 6 + i, foot, 0xFF54371F);
            graphics.fill(ax + 5 + i, foot - height, ax + 6 + i, foot - height * 2 / 3, 0xFF8A5F36);
            graphics.fill(ax + 5 + i, foot - height * 2 / 3, ax + 6 + i, foot - height / 3, 0xFF6F4A2A);
        }
        graphics.fill(ax + 10, y + 59, ax + 12, y + 60, 0xFF7FB24A);
        graphics.fill(ax + 15, y + 57, ax + 17, y + 58, 0xFF7FB24A);
        graphics.fill(ax + 19, y + 60, ax + 21, y + 61, 0xFF7FB24A);
        // The rod and the plate on it
        int plate = ay + 14 + Math.round(progress * RAM_TRAVEL);
        drawSteelRod(graphics, ax + 12, ay + 6, ax + 18, plate);
        drawSteelBar(graphics, ax + 5, plate, ax + 25, plate + 6);
        graphics.fill(ax + 12, ay + 2, ax + 18, ay + 3, progress > 0 && powered ? GLOW : ACCENT_DIM);

        drawGhost(graphics, BiomassPressBlockEntity.INPUT, WASTE);
        drawGhost(graphics, BiomassPressBlockEntity.OUTPUT, new ItemStack(ModItems.BIOMASS.get()));
        if (!powered && progress == 0) {
            drawNotice(graphics, x + 97, y + 56, Component.translatable("gui.cyberfarm.no_power"), WARM);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index == BiomassPressBlockEntity.INPUT) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.press.input"), mouseX, mouseY);
        }
    }
}
