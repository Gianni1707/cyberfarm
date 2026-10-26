package com.cyberfarm.menu;

import com.cyberfarm.block.entity.ElectrolyzerBlockEntity;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class ElectrolyzerMenu extends MachineMenu {
    public ElectrolyzerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(2) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return index == ElectrolyzerBlockEntity.INPUT && ElectrolyzerBlockEntity.isAlumina(resource);
            }
        }, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 2), ContainerLevelAccess.NULL);
    }

    public ElectrolyzerMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.ELECTROLYZER.get(), containerId, data, access, ModBlocks.ELECTROLYZER.get());
        addMachineSlot(items, ElectrolyzerBlockEntity.INPUT, 44, 35);
        addMachineSlot(items, ElectrolyzerBlockEntity.OUTPUT, 116, 35);
        addPlayerInventory(inventory, 84);
    }

    public float getProgress() {
        int total = extra(1);
        return total <= 0 ? 0.0F : (float) extra(0) / total;
    }
}
