package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.phys.Vec3;

/** The core's screen has no slots: it only shows its tier, its pylons and its charge. */
public class ResonanceCoreMenu extends MachineMenu {
    /** It opens from any pylon or cell, which can stand far from the pedestal: stay open within this. */
    private static final double RANGE = 24.0;

    private final ContainerLevelAccess access;

    public ResonanceCoreMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + ResonanceCoreBlockEntity.EXTRA_DATA),
                ContainerLevelAccess.NULL);
    }

    public ResonanceCoreMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.RESONANCE_CORE.get(), containerId, data, access, ModBlocks.RESONANCE_CORE.get());
        this.access = access;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).is(ModBlocks.RESONANCE_CORE.get())
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= RANGE * RANGE, true);
    }

    /** 1 to 4; 0 for a moment while the data syncs. */
    public int getTier() {
        return extra(0);
    }

    public int getLinkedPylons() {
        return extra(1);
    }

    public int getStatus() {
        return extra(2);
    }

    /** What the core found on a side, by 2D data value: see {@code ResonanceCoreBlockEntity.sideCode} and friends. */
    public int getSide(int index) {
        return extra(3 + index);
    }
}
