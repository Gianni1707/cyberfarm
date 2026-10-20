package com.cyberfarm.client.screen;

import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.menu.DroneDockMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class DroneDockScreen extends MachineScreen<DroneDockMenu> {
    // Filter button, under the cargo grid
    private static final int BUTTON_X = DroneDockMenu.CARGO_X - 1;
    private static final int BUTTON_Y = DroneDockMenu.CARGO_Y + 3 * 18 + 3;
    private static final int BUTTON_WIDTH = 3 * 18;
    private static final int BUTTON_HEIGHT = 14;
    private static final String[] STATUS_KEYS = {"none", "idle", "charging", "working", "returning"};

    public DroneDockScreen(DroneDockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, DroneDockMenu.INVENTORY_Y + 82);
        energyBarHeight = 70;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isHovering(BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, event.x(), event.y())) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DroneDockMenu.BUTTON_FILTER);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (isHovering(BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.dock.filter.hint"), mouseX, mouseY);
        }
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawButton(graphics, x + BUTTON_X, y + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.cyberfarm.dock.filter"), PANEL_LIGHT, TEXT);

        int status = menu.getDroneStatus();
        String key = STATUS_KEYS[Math.max(0, Math.min(status, STATUS_KEYS.length - 1))];
        int statusColor = switch (status) {
            case DroneEntity.STATUS_WORKING -> ACCENT;
            case DroneEntity.STATUS_CHARGING, DroneEntity.STATUS_RETURNING -> WARM;
            case DroneEntity.STATUS_IDLE -> TEXT;
            default -> TEXT_DIM;
        };

        graphics.text(font, Component.translatable("gui.cyberfarm.dock.drone"), x + 8, y + 20, TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.dock.status." + key), x + 8, y + 30, statusColor, false);
        if (status != DroneEntity.STATUS_NONE) {
            graphics.text(font, Component.translatable("gui.cyberfarm.dock.battery", menu.getDroneEnergyPercent()), x + 8, y + 42, TEXT, false);
        }
        graphics.text(font, Component.translatable("gui.cyberfarm.dock.area", menu.getAreaWidth(), menu.getAreaDepth()), x + 8, y + 54, TEXT_DIM, false);
        graphics.text(font, Component.translatable(menu.hasTransportRoute() ? "gui.cyberfarm.dock.route" : "gui.cyberfarm.dock.no_route"),
                x + 8, y + 66, menu.hasTransportRoute() ? TEXT : TEXT_DIM, false);
        if (menu.isQuarryStarted()) {
            graphics.text(font, Component.translatable("gui.cyberfarm.dock.quarry", menu.getQuarryLayer()), x + 8, y + 78, TEXT_DIM, false);
        }
    }
}
