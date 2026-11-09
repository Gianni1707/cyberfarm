package com.cyberfarm.menu;

import com.cyberfarm.block.entity.BiomassPressBlockEntity;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class BiomassPressMenu extends MachineMenu {
    public BiomassPressMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(2), new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 2), ContainerLevelAccess.NULL);
    }

    public BiomassPressMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.BIOMASS_PRESS.get(), containerId, data, access, ModBlocks.BIOMASS_PRESS.get());
        addMachineSlot(items, BiomassPressBlockEntity.INPUT, 58, 35);
        addMachineSlot(items, BiomassPressBlockEntity.OUTPUT, 120, 35);
        addPlayerInventory(inventory, 84);
    }

    public float getProgress() {
        int total = extra(1);
        return total <= 0 ? 0.0F : (float) extra(0) / total;
    }

    @Override
    public boolean hasAddons() {
        return true;
    }
}
