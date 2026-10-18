package com.cyberfarm.menu;

import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.jspecify.annotations.Nullable;

public class DroneAssemblerMenu extends MachineMenu {
    public static final int BUTTON_ASSEMBLE = 0;
    public static final int BUTTON_DISASSEMBLE = 1;
    public static final int FRAME_SLOT_X = 12;
    public static final int FRAME_SLOT_Y = 36;
    public static final int MODULE_SLOT_X = 40;
    public static final int MODULE_SLOT_Y = 18;
    public static final int ADDON_SLOT_X = 66;
    public static final int ADDON_SLOT_Y = 27;
    public static final int OUTPUT_SLOT_X = 132;
    public static final int OUTPUT_SLOT_Y = 37;

    private final @Nullable DroneAssemblerBlockEntity assembler;

    public DroneAssemblerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, clientItems(),
                new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 3), ContainerLevelAccess.NULL);
    }

    /** Client copy of the inventory with the same slot rules, so shift-click predicts the server. */
    private static ItemStacksResourceHandler clientItems() {
        return new ItemStacksResourceHandler(DroneAssemblerBlockEntity.SLOT_COUNT) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return DroneAssemblerBlockEntity.canPlace(i -> ItemUtil.getStack(this, i), index, resource);
            }

            @Override
            protected int getCapacity(int index, ItemResource resource) {
                return DroneAssemblerBlockEntity.slotCapacity(i -> ItemUtil.getStack(this, i), index, resource);
            }
        };
    }

    public DroneAssemblerMenu(int containerId, Inventory inventory, @Nullable DroneAssemblerBlockEntity assembler,
            ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.DRONE_ASSEMBLER.get(), containerId, data, access, ModBlocks.DRONE_ASSEMBLER.get());
        this.assembler = assembler;
        addMachineSlot(items, DroneAssemblerBlockEntity.FRAME_SLOT, FRAME_SLOT_X, FRAME_SLOT_Y);
        for (int i = 0; i < DroneAssemblerBlockEntity.MODULE_SLOTS; i++) {
            addMachineSlot(items, DroneAssemblerBlockEntity.FIRST_MODULE_SLOT + i, MODULE_SLOT_X, MODULE_SLOT_Y + i * 18);
        }
        for (int i = 0; i < DroneAssemblerBlockEntity.ADDON_SLOTS; i++) {
            addMachineSlot(items, DroneAssemblerBlockEntity.FIRST_ADDON_SLOT + i, ADDON_SLOT_X + (i % 2) * 18, ADDON_SLOT_Y + (i / 2) * 18);
        }
        addMachineSlot(items, DroneAssemblerBlockEntity.OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y);
        addPlayerInventory(inventory, 102);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (assembler == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        switch (id) {
            case BUTTON_ASSEMBLE -> assembler.startAssembly();
            case BUTTON_DISASSEMBLE -> assembler.disassemble(level);
            default -> {
                return false;
            }
        }
        return true;
    }

    public float getProgress() {
        int total = extra(1);
        return total <= 0 ? 0.0F : (float) extra(0) / total;
    }

    public boolean isAssembling() {
        return extra(2) != 0;
    }

    private ItemStack slotItem(int index) {
        return getSlot(index).getItem();
    }

    public boolean canAssemble() {
        return DroneAssemblerBlockEntity.isValidLayout(this::slotItem);
    }

    public boolean canDisassemble() {
        return DroneAssemblerBlockEntity.canDisassemble(this::slotItem);
    }

    public @Nullable DroneFrameItem getFrame() {
        return slotItem(DroneAssemblerBlockEntity.FRAME_SLOT).getItem() instanceof DroneFrameItem frame ? frame : null;
    }

    public int getAddonCount() {
        return DroneAssemblerBlockEntity.countAddons(this::slotItem, null);
    }
}
