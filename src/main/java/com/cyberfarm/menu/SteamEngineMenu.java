package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

public class SteamEngineMenu extends MachineMenu {
    public SteamEngineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 6), ContainerLevelAccess.NULL);
    }

    public SteamEngineMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.STEAM_ENGINE.get(), containerId, data, access, ModBlocks.STEAM_ENGINE.get());
        addPlayerInventory(inventory, 114);
    }

    /** Cylinders in the row, 0 while it is not complete. */
    public int getCylinders() {
        return extra(0);
    }

    public int getSpeedPercent() {
        return extra(1);
    }

    public int getFuelAmount() {
        return extra(2);
    }

    public int getFuelFluid() {
        return extra(3);
    }

    public int getOutput() {
        return EnergyUtil.combine(extra(4), extra(5));
    }
}
