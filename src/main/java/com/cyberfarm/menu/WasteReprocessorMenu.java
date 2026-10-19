package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.WasteReprocessorBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class WasteReprocessorMenu extends MachineMenu {
    public WasteReprocessorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(3) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return index == WasteReprocessorBlockEntity.INPUT && resource.is(ModItems.DEPLETED_FUEL_ROD.get());
            }
        }, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 2), ContainerLevelAccess.NULL);
    }

    public WasteReprocessorMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.WASTE_REPROCESSOR.get(), containerId, data, access, ModBlocks.WASTE_REPROCESSOR.get());
        addMachineSlot(items, WasteReprocessorBlockEntity.INPUT, 44, 35);
        addMachineSlot(items, WasteReprocessorBlockEntity.WASTE_OUTPUT, 104, 26);
        addMachineSlot(items, WasteReprocessorBlockEntity.URANIUM_OUTPUT, 104, 44);
        addPlayerInventory(inventory, 84);
    }

    public float getProgress() {
        int total = extra(1);
        return total <= 0 ? 0.0F : (float) extra(0) / total;
    }
}
