package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

/** Steam Turbine multiblock: steam tank, rotor speed and output. */
public class TurbineMenu extends MachineMenu {
    public TurbineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 7), ContainerLevelAccess.NULL);
    }

    public TurbineMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.TURBINE.get(), containerId, data, access, ModBlocks.TURBINE_CONTROLLER.get());
        addPlayerInventory(inventory, 84);
    }

    /** 0 while the hall is incomplete, else 1 to 3 for small to large. */
    public int getSize() {
        return extra(0);
    }

    public int getSteam() {
        return extra(1);
    }

    public int getSteamFluid() {
        return extra(2);
    }

    /** Rotor speed in permille. */
    public int getSpeed() {
        return extra(3);
    }

    public int getGenerating() {
        return extra(4);
    }

    public int getMaxSteam() {
        return extra(5);
    }

    public int getFePerMb() {
        return extra(6);
    }
}
