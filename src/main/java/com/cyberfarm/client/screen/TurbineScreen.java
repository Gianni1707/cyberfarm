package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.TurbineControllerBlockEntity;
import com.cyberfarm.menu.TurbineMenu;

import com.cyberfarm.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Left: steam tank. Middle: status, output, rotor speed bar and the hall's ratings. Right: energy. */
public class TurbineScreen extends MachineScreen<TurbineMenu> {
    private static final int TANK_X = 48;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;
    private static final int TEXT_X = 73;
    /** The rotor's angle in the drawing, turned every frame by the synced speed. */
    private double spin;
    private static final String[] SIZE_NAMES = {"", "small", "medium", "large"};

    public TurbineScreen(TurbineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("turbine");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getSteam(), TurbineControllerBlockEntity.TANK_CAPACITY, menu.getSteamFluid());
        // The rotor, turning as fast as it really does
        spin += menu.getSpeed() / 1000.0 * 0.5;
        drawSpinner(graphics, x + 22, y + 39, 11, spin);
        if (menu.getSize() == 0) {
            graphics.text(font, Component.translatable("gui.cyberfarm.turbine.incomplete"), x + TEXT_X, y + 22, WARM, false);
            return;
        }
        boolean running = menu.getGenerating() > 0;
        graphics.text(font, Component.translatable(running ? "gui.cyberfarm.generator.burning" : "gui.cyberfarm.generator.idle"),
                x + TEXT_X, y + 20, running ? ACCENT : TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.fe_per_tick", menu.getGenerating()), x + TEXT_X, y + 31, TEXT, false);

        // Rotor speed bar
        graphics.text(font, Component.translatable("gui.cyberfarm.turbine.rotor", menu.getSpeed() / 10), x + TEXT_X, y + 44, TEXT_DIM, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        if (menu.getSize() > 0) {
            Component size = Component.translatable("gui.cyberfarm.reactor.size." + SIZE_NAMES[menu.getSize()]);
            graphics.text(font, size, imageWidth - 8 - font.width(size), titleLabelY, TEXT_DIM, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (menu.getSize() > 0 && isHovering(70, 17, 82, 50, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.turbine.rating", menu.getMaxSteam(), menu.getFePerMb()), mouseX, mouseY);
        }
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getSteam(), TurbineControllerBlockEntity.TANK_CAPACITY, menu.getSteamFluid());
    }
}
