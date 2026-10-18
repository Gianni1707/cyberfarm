package com.cyberfarm.client.screen;

import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.menu.DroneDockMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DroneDockScreen extends MachineScreen<DroneDockMenu> {
    private static final String[] STATUS_KEYS = {"none", "idle", "charging", "working", "returning"};

    public DroneDockScreen(DroneDockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, DroneDockMenu.INVENTORY_Y + 82);
        energyBarHeight = 70;
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
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
