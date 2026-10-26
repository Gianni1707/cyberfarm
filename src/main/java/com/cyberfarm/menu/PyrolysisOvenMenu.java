package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.PyrolysisOvenBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class PyrolysisOvenMenu extends MachineMenu {
    public PyrolysisOvenMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(2) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return index == PyrolysisOvenBlockEntity.STRING && resource.is(Items.STRING);
            }
        }, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 4), ContainerLevelAccess.NULL);
    }

    public PyrolysisOvenMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.PYROLYSIS_OVEN.get(), containerId, data, access, ModBlocks.PYROLYSIS_OVEN.get());
        addMachineSlot(items, PyrolysisOvenBlockEntity.STRING, 62, 35);
        addMachineSlot(items, PyrolysisOvenBlockEntity.OUTPUT, 124, 35);
        addPlayerInventory(inventory, 84);
    }

    public float getProgress() {
        int total = extra(1);
        return total <= 0 ? 0.0F : (float) extra(0) / total;
    }

    public int getMethaneAmount() {
        return extra(2);
    }

    public int getMethaneFluid() {
        return extra(3);
    }
}
