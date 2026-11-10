package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.BioDigesterBlockEntity;
import com.cyberfarm.menu.BioDigesterMenu;
import com.cyberfarm.registry.ModItems;

import com.cyberfarm.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

public class BioDigesterScreen extends MachineScreen<BioDigesterMenu> {
    private static final int TANK_X = 120;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public BioDigesterScreen(BioDigesterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("bio_digester");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        float progress = menu.getProgress();
        int ax = x + 5;
        // The mash sinking in the drum's glass, the gas rising off it
        int level = (int) (12 * (1 - progress)) + 3;
        graphics.fill(ax + 8, y + 50 - level, ax + 28, y + 50, 0xFF6F4A2A);
        graphics.fill(ax + 8, y + 50 - level, ax + 28, y + 52 - level, 0xFF8A5F36);
        if (progress > 0) {
            drawBubbles(graphics, ax + 8, y + 31, 20, 19 - level, 0xFFC8F890);
        }
        drawGhost(graphics, 0, new ItemStack(ModItems.BIOMASS.get()));
        if (menu.getEnergy() < BioDigesterBlockEntity.ENERGY_PER_TICK && progress == 0) {
            drawNotice(graphics, x + 90, y + 56, Component.translatable("gui.cyberfarm.no_power"), WARM);
        }
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getTankAmount(), BioDigesterBlockEntity.TANK_CAPACITY, menu.getTankFluid());
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index == 0) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.digester.input"), mouseX, mouseY);
        }
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getTankAmount(), BioDigesterBlockEntity.TANK_CAPACITY, menu.getTankFluid());
    }
}
