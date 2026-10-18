package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.CombustionGeneratorBlockEntity;
import com.cyberfarm.menu.CombustionGeneratorMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CombustionGeneratorScreen extends MachineScreen<CombustionGeneratorMenu> {
    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        // Fuel gauge above the fuel slot
        drawProgressBar(graphics, x + 80, y + 34, 16, 4, menu.getBurnProgress(), WARM);

        boolean burning = menu.isBurning();
        graphics.text(font, Component.translatable(burning ? "gui.cyberfarm.generator.burning" : "gui.cyberfarm.generator.idle"),
                x + 8, y + 22, burning ? WARM : TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.fe_per_tick", burning ? CombustionGeneratorBlockEntity.GENERATION_PER_TICK : 0),
                x + 8, y + 34, TEXT, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.generator.fuel"), x + 8, y + 48, TEXT_DIM, false);
    }
}
