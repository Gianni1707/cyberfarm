package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.CombustionGeneratorBlockEntity;
import com.cyberfarm.menu.CombustionGeneratorMenu;

import com.cyberfarm.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CombustionGeneratorScreen extends MachineScreen<CombustionGeneratorMenu> {
    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("combustion_generator");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        boolean burning = menu.isBurning();
        int ax = x + 5;
        if (burning) {
            // The fire sinks with what is left of the fuel, each flame on its own beat
            long beat = Util.getMillis() / 110;
            int base = 4 + Math.round(10 * menu.getBurnProgress());
            for (int i = 0; i < 4; i++) {
                int fx = ax + 8 + i * 5;
                int height = base + (int) ((beat + i * 3L) % 4);
                graphics.fill(fx, y + 58 - height, fx + 4, y + 58, 0xFFFF8C1A);
                graphics.fill(fx + 1, y + 60 - height, fx + 3, y + 58, 0xFFFFD04D);
            }
        }
        // The fan on the chimney
        drawSpinner(graphics, ax + 17, y + 22, 5, burning ? Util.getMillis() / 90.0 : 0.0);
        drawGhost(graphics, 0, new ItemStack(Items.COAL));
        drawNotice(graphics, x + 122, y + 22, Component.translatable(burning ? "gui.cyberfarm.generator.burning" : "gui.cyberfarm.generator.idle"), burning ? WARM : TEXT_DIM);
        drawNotice(graphics, x + 122, y + 54, Component.translatable("gui.cyberfarm.fe_per_tick", burning ? CombustionGeneratorBlockEntity.GENERATION_PER_TICK : 0), TEXT);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index == 0) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.generator.fuel"), mouseX, mouseY);
        }
    }
}
