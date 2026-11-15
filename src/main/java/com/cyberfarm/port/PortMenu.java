package com.cyberfarm.port;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

/** A 26.x menu on 1.20.1: brings addStandardInventorySlots, which 1.20.1 menus lay out by hand. */
public abstract class PortMenu extends AbstractContainerMenu {
    protected PortMenu(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    /** The main inventory, three rows from ({@code x}, {@code y}), and the hotbar 58 px below its top. */
    protected void addStandardInventorySlots(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, x + column * 18, y + 58));
        }
    }
}
