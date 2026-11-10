package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.AcidSynthesizerBlockEntity;
import com.cyberfarm.menu.AcidSynthesizerMenu;
import com.cyberfarm.registry.ModItems;

import com.cyberfarm.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

/** Water tank, the sulfur slot, a progress bar and the acid tank. */
public class AcidSynthesizerScreen extends MachineScreen<AcidSynthesizerMenu> {
    private static final int INPUT_X = 48;
    private static final int OUTPUT_X = 128;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public AcidSynthesizerScreen(AcidSynthesizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("acid_synthesizer");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        int cap = AcidSynthesizerBlockEntity.TANK_CAPACITY;
        drawTank(graphics, x + INPUT_X, y + TANK_Y, TANK_HEIGHT, menu.getInputAmount(), cap, menu.getInputFluid());
        drawTank(graphics, x + OUTPUT_X, y + TANK_Y, TANK_HEIGHT, menu.getOutputAmount(), cap, menu.getOutputFluid());
        float progress = menu.getProgress();
        int ax = x + 5;
        // Water turning to acid in the tower, sulfur falling into it
        if (menu.getInputAmount() > 0 || progress > 0) {
            int color = mixColor(0xFF3F6FE4, 0xFF9BE04A, progress);
            graphics.fill(ax + 5, y + 37, ax + 31, y + 57, color);
            graphics.fill(ax + 5, y + 37, ax + 31, y + 38, mixColor(color, 0xFFFFFFFF, 0.6F));
        }
        if (progress > 0) {
            drawBubbles(graphics, ax + 5, y + 38, 26, 18, 0xFFE4FFB0);
            int grain = y + 31 + (int) (Util.getMillis() / 60 % 6);
            graphics.fill(ax + 17, grain, ax + 19, grain + 2, 0xFFD8C83A);
        }
        drawGhost(graphics, 0, new ItemStack(ModItems.SULFUR.get()));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int cap = AcidSynthesizerBlockEntity.TANK_CAPACITY;
        // The recipe, which used to be written on the panel
        if (isHovering(96, 34, 24, 18, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.acid.recipe"), mouseX, mouseY);
        }
        tankTooltip(graphics, mouseX, mouseY, INPUT_X, TANK_Y, TANK_HEIGHT, menu.getInputAmount(), cap, menu.getInputFluid());
        tankTooltip(graphics, mouseX, mouseY, OUTPUT_X, TANK_Y, TANK_HEIGHT, menu.getOutputAmount(), cap, menu.getOutputFluid());
    }
}
