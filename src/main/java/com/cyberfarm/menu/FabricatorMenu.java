package com.cyberfarm.menu;

import com.cyberfarm.block.entity.FabricatorBlockEntity;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class FabricatorMenu extends MachineMenu {
    public FabricatorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(FabricatorBlockEntity.INPUTS + 1) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return index < FabricatorBlockEntity.INPUTS && FabricatorBlockEntity.isIngredient(resource.toStack());
            }
        }, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 4), ContainerLevelAccess.NULL);
    }

    public FabricatorMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.FABRICATOR.get(), containerId, data, access, ModBlocks.FABRICATOR.get());
        for (int i = 0; i < FabricatorBlockEntity.INPUTS; i++) {
            addMachineSlot(items, i, 44, 17 + i * 18);
        }
        addMachineSlot(items, FabricatorBlockEntity.OUTPUT, 124, 35);
        addPlayerInventory(inventory, 84);
    }

    public float getProgress() {
        int total = extra(1);
        return total <= 0 ? 0.0F : (float) extra(0) / total;
    }

    public int getAcidAmount() {
        return extra(2);
    }

    public int getAcidFluid() {
        return extra(3);
    }
}
