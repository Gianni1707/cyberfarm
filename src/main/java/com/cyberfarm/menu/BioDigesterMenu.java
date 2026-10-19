package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class BioDigesterMenu extends MachineMenu {
    public BioDigesterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(1), new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 4), ContainerLevelAccess.NULL);
    }

    public BioDigesterMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.BIO_DIGESTER.get(), containerId, data, access, ModBlocks.BIO_DIGESTER.get());
        addMachineSlot(items, 0, 44, 35);
        addPlayerInventory(inventory, 84);
    }

    public float getProgress() {
        int total = extra(1);
        return total <= 0 ? 0.0F : (float) extra(0) / total;
    }

    public int getTankAmount() {
        return extra(2);
    }

    public int getTankFluid() {
        return extra(3);
    }
}
