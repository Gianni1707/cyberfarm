package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.MufflerBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jspecify.annotations.Nullable;

/** Muffler GUI: one toggle button per {@link MufflerBlockEntity.Category}, the button id being its ordinal. */
public class MufflerMenu extends MachineMenu {
    private final @Nullable MufflerBlockEntity muffler;

    public MufflerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 1), ContainerLevelAccess.NULL);
    }

    public MufflerMenu(int containerId, Inventory inventory, @Nullable MufflerBlockEntity muffler, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.MUFFLER.get(), containerId, data, access, ModBlocks.MUFFLER.get());
        this.muffler = muffler;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        MufflerBlockEntity.Category[] categories = MufflerBlockEntity.Category.values();
        if (muffler == null || id < 0 || id >= categories.length) {
            return false;
        }
        muffler.toggle(categories[id]);
        return true;
    }

    public boolean isMuting(MufflerBlockEntity.Category category) {
        return (extra(0) & category.bit()) != 0;
    }
}
