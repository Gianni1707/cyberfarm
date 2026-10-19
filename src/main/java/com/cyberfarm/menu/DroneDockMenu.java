package com.cyberfarm.menu;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class DroneDockMenu extends MachineMenu {
    public static final int CARGO_X = 98;
    public static final int CARGO_Y = 17;
    public static final int INVENTORY_Y = 104;
    public static final int BUTTON_FILTER = 0;

    private final ContainerLevelAccess access;

    public DroneDockMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(DroneDockBlockEntity.CARGO_SLOTS),
                new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 7), ContainerLevelAccess.NULL);
    }

    public DroneDockMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.DRONE_DOCK.get(), containerId, data, access, ModBlocks.DRONE_DOCK.get());
        this.access = access;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addMachineSlot(items, row * 3 + col, CARGO_X + col * 18, CARGO_Y + row * 18);
            }
        }
        addPlayerInventory(inventory, INVENTORY_Y);
    }

    /** The Filter button swaps this menu for the dock's filter. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_FILTER) {
            return false;
        }
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof DroneDockBlockEntity dock) {
                dock.openFilter(player);
            }
        });
        return true;
    }

    public int getAreaWidth() {
        return extra(0);
    }

    public int getAreaDepth() {
        return extra(1);
    }

    public int getDroneStatus() {
        return extra(2);
    }

    public int getDroneEnergyPercent() {
        return extra(3);
    }

    public boolean hasTransportRoute() {
        return extra(4) != 0;
    }

    public boolean isQuarryStarted() {
        return extra(5) != 0;
    }

    public int getQuarryLayer() {
        return extra(6);
    }
}
