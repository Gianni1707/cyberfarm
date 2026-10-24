package com.cyberfarm.menu;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.MechHangarBlockEntity;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.entity.MechModule;
import com.cyberfarm.item.MechModuleItem;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class MechHangarMenu extends MachineMenu {
    public static final int BUTTON_FOLD = 0;
    private final ContainerLevelAccess access;

    /** Where the four module slots sit, by MechModule order: jet behind the shoulder, drill and blade by the arms, armour on the trunk. */
    public static final int[][] SLOTS = {{148, 24}, {148, 76}, {78, 76}, {113, 46}};

    public MechHangarMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStacksResourceHandler(MechModule.values().length) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return resource.getItem() instanceof MechModuleItem module && module.module().ordinal() == index;
            }
        }, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 6), ContainerLevelAccess.NULL);
    }

    public MechHangarMenu(int containerId, Inventory inventory, ItemStacksResourceHandler modules, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.MECH_HANGAR.get(), containerId, data, access, ModBlocks.MECH_HANGAR.get());
        this.access = access;
        for (MechModule module : MechModule.values()) {
            int[] at = SLOTS[module.ordinal()];
            addMachineSlot(modules, module.ordinal(), at[0], at[1]);
        }
        addPlayerInventory(inventory, 128);
    }

    /** The Fold button: the parked mech folds back into its frame and drops on the pad. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_FOLD) {
            return false;
        }
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof MechHangarBlockEntity hangar) {
                hangar.foldDocked();
            }
        });
        return true;
    }

    public boolean isFormed() {
        return extra(0) != 0;
    }

    /** 0 nothing parked, 1 charging, 2 repairing, 3 ready. */
    public int getStatus() {
        return extra(1);
    }

    public int getMechEnergy() {
        return EnergyUtil.combine(extra(2), extra(3));
    }

    public int getMechIntegrity() {
        return extra(4);
    }

    public boolean isMechDisabled() {
        return extra(5) != 0;
    }
}
