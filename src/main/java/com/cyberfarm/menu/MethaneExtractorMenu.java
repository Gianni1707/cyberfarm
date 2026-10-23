package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

public class MethaneExtractorMenu extends MachineMenu {
    public MethaneExtractorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 10), ContainerLevelAccess.NULL);
    }

    public MethaneExtractorMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.METHANE_EXTRACTOR.get(), containerId, data, access, ModBlocks.METHANE_EXTRACTOR.get());
        addPlayerInventory(inventory, 128);
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

    public int getAcidAmount() {
        return extra(5);
    }

    public int getAcidFluid() {
        return extra(6);
    }

    /** Network id of the mob in the chamber, 0 when it is empty. */
    public int getMobId() {
        return EnergyUtil.combine(extra(7), extra(8));
    }

    /** Acid is being pumped in right now. */
    public boolean isFilling() {
        return extra(9) != 0;
    }
}
