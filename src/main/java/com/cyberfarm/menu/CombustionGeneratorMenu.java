package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class CombustionGeneratorMenu extends MachineMenu {
    public CombustionGeneratorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(1), new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 2), ContainerLevelAccess.NULL);
    }

    public CombustionGeneratorMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.COMBUSTION_GENERATOR.get(), containerId, data, access, ModBlocks.COMBUSTION_GENERATOR.get());
        addMachineSlot(items, 0, 80, 44);
        addPlayerInventory(inventory, 84);
    }

    public boolean isBurning() {
        return extra(0) > 0;
    }

    /** Remaining fuel as a 0..1 fraction. */
    public float getBurnProgress() {
        int duration = extra(1);
        return duration <= 0 ? 0.0F : (float) extra(0) / duration;
    }
}
