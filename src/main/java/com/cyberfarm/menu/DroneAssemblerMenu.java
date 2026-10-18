package com.cyberfarm.menu;

import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

public class DroneAssemblerMenu extends MachineMenu {
    public static final int BUTTON_ASSEMBLE = 0;
    public static final int FRAME_SLOT_X = 17;
    public static final int FRAME_SLOT_Y = 45;
    public static final int MODULE_SLOT_X = 53;
    public static final int MODULE_SLOT_Y = 27;
    public static final int OUTPUT_SLOT_X = 129;
    public static final int OUTPUT_SLOT_Y = 45;

    private final @Nullable DroneAssemblerBlockEntity assembler;

    public DroneAssemblerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new ItemStacksResourceHandler(DroneAssemblerBlockEntity.SLOT_COUNT),
                new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 3), ContainerLevelAccess.NULL);
    }

    public DroneAssemblerMenu(int containerId, Inventory inventory, @Nullable DroneAssemblerBlockEntity assembler,
            ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.DRONE_ASSEMBLER.get(), containerId, data, access, ModBlocks.DRONE_ASSEMBLER.get());
        this.assembler = assembler;
        addMachineSlot(items, DroneAssemblerBlockEntity.FRAME_SLOT, FRAME_SLOT_X, FRAME_SLOT_Y);
        for (int i = 0; i < DroneAssemblerBlockEntity.MODULE_SLOTS; i++) {
            addMachineSlot(items, DroneAssemblerBlockEntity.FIRST_MODULE_SLOT + i, MODULE_SLOT_X, MODULE_SLOT_Y + i * 18);
        }
        addMachineSlot(items, DroneAssemblerBlockEntity.OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y);
        addPlayerInventory(inventory, 102);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_ASSEMBLE && assembler != null) {
            assembler.startAssembly();
            return true;
        }
        return false;
    }

    public float getProgress() {
        int total = extra(1);
        return total <= 0 ? 0.0F : (float) extra(0) / total;
    }

    public boolean isAssembling() {
        return extra(2) != 0;
    }

    public boolean canAssemble() {
        return DroneAssemblerBlockEntity.isValidLayout(index -> getSlot(index).getItem());
    }

    /** Module slots unlocked by the frame currently in the frame slot (0 if none). */
    public int getUnlockedModuleSlots() {
        return getSlot(DroneAssemblerBlockEntity.FRAME_SLOT).getItem().getItem() instanceof DroneFrameItem frame ? frame.getModuleSlots() : 0;
    }
}
