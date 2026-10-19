package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class CoolantMixerMenu extends MachineMenu {
    public CoolantMixerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(1) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return resource.is(Items.LAPIS_LAZULI) || resource.is(Items.BLUE_ICE);
            }
        }, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 6), ContainerLevelAccess.NULL);
    }

    public CoolantMixerMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.COOLANT_MIXER.get(), containerId, data, access, ModBlocks.COOLANT_MIXER.get());
        addMachineSlot(items, 0, 62, 35);
        addPlayerInventory(inventory, 84);
    }

    public float getProgress() {
        int total = extra(1);
        return total <= 0 ? 0.0F : (float) extra(0) / total;
    }

    public int getInputAmount() {
        return extra(2);
    }

    public int getInputFluid() {
        return extra(3);
    }

    public int getOutputAmount() {
        return extra(4);
    }

    public int getOutputFluid() {
        return extra(5);
    }
}
