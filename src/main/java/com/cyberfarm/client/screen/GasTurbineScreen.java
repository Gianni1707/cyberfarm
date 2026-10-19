package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.GasTurbineBlockEntity;
import com.cyberfarm.menu.GasTurbineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GasTurbineScreen extends MachineScreen<GasTurbineMenu> {
    private static final int TANK_X = 12;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public GasTurbineScreen(GasTurbineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getTankAmount(), GasTurbineBlockEntity.TANK_CAPACITY, menu.getTankFluid());
        boolean running = menu.getGenerating() > 0;
        graphics.text(font, Component.translatable(running ? "gui.cyberfarm.generator.burning" : "gui.cyberfarm.generator.idle"),
                x + 38, y + 22, running ? WARM : TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.fe_per_tick", menu.getGenerating()), x + 38, y + 34, TEXT, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.turbine.biogas", GasTurbineBlockEntity.FE_PER_MB_BIOGAS), x + 38, y + 50, TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.turbine.methane", GasTurbineBlockEntity.FE_PER_MB_METHANE), x + 38, y + 60, TEXT_DIM, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getTankAmount(), GasTurbineBlockEntity.TANK_CAPACITY, menu.getTankFluid());
    }
}
