package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

public class GasTurbineMenu extends MachineMenu {
    public GasTurbineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 3), ContainerLevelAccess.NULL);
    }

    public GasTurbineMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.GAS_TURBINE.get(), containerId, data, access, ModBlocks.GAS_TURBINE.get());
        addPlayerInventory(inventory, 84);
    }

    public int getTankAmount() {
        return extra(0);
    }

    public int getTankFluid() {
        return extra(1);
    }

    public int getGenerating() {
        return extra(2);
    }
}
