package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.CoolantMixerBlockEntity;
import com.cyberfarm.menu.CoolantMixerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;

/** Input tank, the reagent slot, a progress bar and the output tank. */
public class CoolantMixerScreen extends MachineScreen<CoolantMixerMenu> {
    private static final int INPUT_X = 48;
    private static final int OUTPUT_X = 128;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public CoolantMixerScreen(CoolantMixerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("coolant_mixer");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        int cap = CoolantMixerBlockEntity.TANK_CAPACITY;
        drawTank(graphics, x + INPUT_X, y + TANK_Y, TANK_HEIGHT, menu.getInputAmount(), cap, menu.getInputFluid());
        drawTank(graphics, x + OUTPUT_X, y + TANK_Y, TANK_HEIGHT, menu.getOutputAmount(), cap, menu.getOutputFluid());
        float progress = menu.getProgress();
        int ax = x + 5;
        // The vat turning from what comes in to what goes out, stirred by the paddle
        int from = tankColor(menu.getInputFluid());
        if (from != 0) {
            // Code 5 is water, which makes coolant; coolant makes cryo
            int color = mixColor(from, tankColor(menu.getInputFluid() == 5 ? 6 : 7), progress);
            graphics.fill(ax + 5, y + 37, ax + 31, y + 57, color);
            graphics.fill(ax + 5, y + 37, ax + 31, y + 38, mixColor(color, 0xFFFFFFFF, 0.6F));
        }
        drawSteelRod(graphics, ax + 17, y + 18, ax + 19, y + 52);
        int reach = progress > 0 ? (int) (Math.abs(Math.cos(Util.getMillis() / 160.0)) * 10) + 2 : 12;
        drawSteelBar(graphics, ax + 18 - reach, y + 47, ax + 18 + reach, y + 50);
        drawGhost(graphics, 0, new ItemStack(menu.getInputFluid() == 5 || menu.getInputFluid() == 0 ? Items.LAPIS_LAZULI : Items.BLUE_ICE));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int cap = CoolantMixerBlockEntity.TANK_CAPACITY;
        tankTooltip(graphics, mouseX, mouseY, INPUT_X, TANK_Y, TANK_HEIGHT, menu.getInputAmount(), cap, menu.getInputFluid());
        tankTooltip(graphics, mouseX, mouseY, OUTPUT_X, TANK_Y, TANK_HEIGHT, menu.getOutputAmount(), cap, menu.getOutputFluid());
    }
}
