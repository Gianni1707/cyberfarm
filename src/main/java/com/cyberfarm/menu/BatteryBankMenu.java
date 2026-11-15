package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/** The Battery Bank's GUI: the whole bank's charge, and a slot that charges an item. */
public class BatteryBankMenu extends MachineMenu {
    public BatteryBankMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(1) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return ItemAccess.forStack(resource.toStack()).getCapability(Capabilities.Energy.ITEM) != null;
            }
        }, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 8), ContainerLevelAccess.NULL);
    }

    public BatteryBankMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.BATTERY_BANK.get(), containerId, data, access, ModBlocks.BATTERY_BANK.get());
        addMachineSlot(items, 0, 12, 28);
        addPlayerInventory(inventory, 84);
    }

    /** What the whole bank holds, all its blocks together. */
    public int getBankEnergy() {
        return EnergyUtil.combine(extra(0), extra(1));
    }

    public int getBankCapacity() {
        return EnergyUtil.combine(extra(2), extra(3));
    }




    /** FE per tick flowing in and out of the bank, smoothed. */
    public int getIn() {
        return EnergyUtil.combine(extra(4), extra(5));
    }

    public int getOut() {
        return EnergyUtil.combine(extra(6), extra(7));
    }
}
