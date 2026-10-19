package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

public class MethaneExtractorMenu extends MachineMenu {
    public MethaneExtractorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 5), ContainerLevelAccess.NULL);
    }

    public MethaneExtractorMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.METHANE_EXTRACTOR.get(), containerId, data, access, ModBlocks.METHANE_EXTRACTOR.get());
        addPlayerInventory(inventory, 84);
    }

    public boolean isFormed() {
        return extra(0) != 0;
    }

    public int getMobCount() {
        return extra(1);
    }

    public int getRate() {
        return extra(2);
    }

    public int getTankAmount() {
        return extra(3);
    }

    public int getTankFluid() {
        return extra(4);
    }
}
