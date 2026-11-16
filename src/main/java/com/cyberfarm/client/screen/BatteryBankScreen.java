package com.cyberfarm.client.screen;

import com.cyberfarm.menu.BatteryBankMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The bank's charge as a wide gauge with its flows, the slot that charges an item, and a button per side for In/Out/Off. */
public class BatteryBankScreen extends MachineScreen<BatteryBankMenu> {
    private static final int IN = 0xFF00B894, OUT = 0xFFFFB347;

    public BatteryBankScreen(BatteryBankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("battery_bank");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.text(font, Component.translatable("gui.cyberfarm.battery_bank.charge"), x + 6, y + 16, TEXT_DIM, false);
        // How the sides are set: with the Cable Swapper, on the bank itself
        graphics.text(font, Component.translatable("gui.cyberfarm.battery_bank.sides_hint"), x + 36, y + 24, TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.battery_bank.sides_hint2"), x + 36, y + 34, TEXT_DIM, false);
        // The bank's charge on one line, the flows on the next
        graphics.text(font, Component.literal(String.format("%,d / %,d FE", menu.getBankEnergy(), menu.getBankCapacity())), x + 10, y + 51, TEXT, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.battery_bank.in", String.format("%,d", menu.getIn())), x + 10, y + 60, IN, false);
        // Right-aligned to stop short of the energy gauge
        Component out = Component.translatable("gui.cyberfarm.battery_bank.out", String.format("%,d", menu.getOut()));
        graphics.text(font, out, x + energyBarX - 6 - font.width(out), y + 60, OUT, false);
    }
}
