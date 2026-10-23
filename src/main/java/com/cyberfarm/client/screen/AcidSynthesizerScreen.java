package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.AcidSynthesizerBlockEntity;
import com.cyberfarm.menu.AcidSynthesizerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Water tank, the sulfur slot, a progress bar and the acid tank. */
public class AcidSynthesizerScreen extends MachineScreen<AcidSynthesizerMenu> {
    private static final int INPUT_X = 12;
    private static final int OUTPUT_X = 126;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public AcidSynthesizerScreen(AcidSynthesizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        int cap = AcidSynthesizerBlockEntity.TANK_CAPACITY;
        drawTank(graphics, x + INPUT_X, y + TANK_Y, TANK_HEIGHT, menu.getInputAmount(), cap, menu.getInputFluid());
        drawTank(graphics, x + OUTPUT_X, y + TANK_Y, TANK_HEIGHT, menu.getOutputAmount(), cap, menu.getOutputFluid());
        drawProgressBar(graphics, x + 88, y + 40, 28, 6, menu.getProgress(), 0xFF9BE04A);
        graphics.text(font, Component.translatable("gui.cyberfarm.acid.recipe"), x + 34, y + 62, TEXT_DIM, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int cap = AcidSynthesizerBlockEntity.TANK_CAPACITY;
        tankTooltip(graphics, mouseX, mouseY, INPUT_X, TANK_Y, TANK_HEIGHT, menu.getInputAmount(), cap, menu.getInputFluid());
        tankTooltip(graphics, mouseX, mouseY, OUTPUT_X, TANK_Y, TANK_HEIGHT, menu.getOutputAmount(), cap, menu.getOutputFluid());
    }
}
