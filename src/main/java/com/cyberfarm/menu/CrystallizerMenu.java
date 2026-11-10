package com.cyberfarm.menu;

import com.cyberfarm.block.entity.CrystallizerBlockEntity;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Items;
import com.cyberfarm.port.ItemResource;
import com.cyberfarm.port.ItemStacksResourceHandler;

public class CrystallizerMenu extends MachineMenu {
    public CrystallizerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(3) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return index == CrystallizerBlockEntity.ISOTOPE ? resource.is(ModItems.ISOTOPE_SHARD.get())
                        : index == CrystallizerBlockEntity.AMETHYST && resource.is(Items.AMETHYST_SHARD);
            }
        }, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 5), ContainerLevelAccess.NULL);
    }

    public CrystallizerMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.CRYSTALLIZER.get(), containerId, data, access, ModBlocks.CRYSTALLIZER.get());
        addMachineSlot(items, CrystallizerBlockEntity.ISOTOPE, 74, 26);
        addMachineSlot(items, CrystallizerBlockEntity.AMETHYST, 74, 46);
        addMachineSlot(items, CrystallizerBlockEntity.OUTPUT, 128, 35);
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

    public boolean isFormed() {
        return extra(4) != 0;
    }

    @Override
    public boolean hasAddons() {
        return true;
    }
}
