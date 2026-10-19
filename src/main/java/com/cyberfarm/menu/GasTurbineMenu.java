package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;

/** Shared by the Gas Turbine and the Steam Turbine. */
public class GasTurbineMenu extends MachineMenu {
    public GasTurbineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 3), ContainerLevelAccess.NULL);
    }

    public GasTurbineMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        this(ModMenus.GAS_TURBINE.get(), containerId, inventory, data, access, ModBlocks.GAS_TURBINE.get());
    }

    public static GasTurbineMenu steam(int containerId, Inventory inventory) {
        return steam(containerId, inventory, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 3), ContainerLevelAccess.NULL);
    }

    public static GasTurbineMenu steam(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        return new GasTurbineMenu(ModMenus.STEAM_TURBINE.get(), containerId, inventory, data, access, ModBlocks.STEAM_TURBINE.get());
    }

    private GasTurbineMenu(MenuType<?> type, int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access, Block block) {
        super(type, containerId, data, access, block);
        addPlayerInventory(inventory, 84);
    }

    public boolean isSteam() {
        return getType() == ModMenus.STEAM_TURBINE.get();
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
