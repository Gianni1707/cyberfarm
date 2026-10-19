package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.CoolantMixerBlockEntity;
import com.cyberfarm.menu.CoolantMixerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Input tank, the reagent slot, a progress bar and the output tank. */
public class CoolantMixerScreen extends MachineScreen<CoolantMixerMenu> {
    private static final int INPUT_X = 12;
    private static final int OUTPUT_X = 126;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public CoolantMixerScreen(CoolantMixerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        int cap = CoolantMixerBlockEntity.TANK_CAPACITY;
        drawTank(graphics, x + INPUT_X, y + TANK_Y, TANK_HEIGHT, menu.getInputAmount(), cap, menu.getInputFluid());
        drawTank(graphics, x + OUTPUT_X, y + TANK_Y, TANK_HEIGHT, menu.getOutputAmount(), cap, menu.getOutputFluid());
        drawProgressBar(graphics, x + 88, y + 40, 28, 6, menu.getProgress(), ACCENT);
        graphics.text(font, Component.translatable("gui.cyberfarm.mixer.recipe1"), x + 34, y + 60, TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.mixer.recipe2"), x + 34, y + 70, TEXT_DIM, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int cap = CoolantMixerBlockEntity.TANK_CAPACITY;
        tankTooltip(graphics, mouseX, mouseY, INPUT_X, TANK_Y, TANK_HEIGHT, menu.getInputAmount(), cap, menu.getInputFluid());
        tankTooltip(graphics, mouseX, mouseY, OUTPUT_X, TANK_Y, TANK_HEIGHT, menu.getOutputAmount(), cap, menu.getOutputFluid());
    }
}
