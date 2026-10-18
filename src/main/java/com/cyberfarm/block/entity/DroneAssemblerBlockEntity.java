package com.cyberfarm.block.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import com.cyberfarm.drone.DroneModules;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.item.DroneModuleItem;
import com.cyberfarm.menu.DroneAssemblerMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Builds a drone from a frame plus up to three function modules. The frame tier decides how many
 * module slots are usable. Assembly is started from the GUI and costs FE over time.
 */
public class DroneAssemblerBlockEntity extends MachineBlockEntity {
    public static final int FRAME_SLOT = 0;
    public static final int FIRST_MODULE_SLOT = 1;
    public static final int MODULE_SLOTS = 3;
    public static final int OUTPUT_SLOT = FIRST_MODULE_SLOT + MODULE_SLOTS;
    public static final int SLOT_COUNT = OUTPUT_SLOT + 1;
    public static final int CAPACITY = 20_000;
    public static final int MAX_INPUT = 500;
    public static final int ASSEMBLY_TIME = 100;
    public static final int ENERGY_PER_TICK = 20;

    private int progress;
    private boolean assembling;

    public DroneAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_ASSEMBLER.get(), pos, state, SLOT_COUNT, CAPACITY, MAX_INPUT, 0);
    }

    /**
     * Shared by the server and the GUI: a frame, at least one module, no duplicate modules,
     * nothing in slots the frame tier has not unlocked, and an empty output slot.
     */
    public static boolean isValidLayout(IntFunction<ItemStack> slots) {
        if (!(slots.apply(FRAME_SLOT).getItem() instanceof DroneFrameItem frame) || !slots.apply(OUTPUT_SLOT).isEmpty()) {
            return false;
        }
        Set<Item> modules = new HashSet<>();
        for (int i = 0; i < MODULE_SLOTS; i++) {
            ItemStack stack = slots.apply(FIRST_MODULE_SLOT + i);
            if (stack.isEmpty()) {
                continue;
            }
            if (i >= frame.getModuleSlots() || !modules.add(stack.getItem())) {
                return false;
            }
        }
        return !modules.isEmpty();
    }

    public boolean canAssemble() {
        return isValidLayout(this::getStack);
    }

    public void startAssembly() {
        if (!assembling && canAssemble()) {
            assembling = true;
            progress = 0;
            setChanged();
        }
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!assembling) {
            return;
        }
        if (!canAssemble()) {
            assembling = false;
            progress = 0;
            setChanged();
            return;
        }
        if (energy.consume(ENERGY_PER_TICK)) {
            progress++;
            if (progress >= ASSEMBLY_TIME) {
                finishAssembly(level, pos);
            }
            setChanged();
        }
    }

    private void finishAssembly(ServerLevel level, BlockPos pos) {
        List<Item> parts = new ArrayList<>();
        for (int i = FRAME_SLOT; i < OUTPUT_SLOT; i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) {
                parts.add(stack.getItem());
                setStack(i, ItemStack.EMPTY);
            }
        }
        ItemStack drone = new ItemStack(ModItems.DRONE.get());
        drone.set(ModDataComponents.DRONE_MODULES.get(), new DroneModules(parts));
        drone.set(ModDataComponents.ENERGY.get(), 0);
        setStack(OUTPUT_SLOT, drone);

        assembling = false;
        progress = 0;
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        if (index == FRAME_SLOT) {
            return resource.getItem() instanceof DroneFrameItem;
        }
        return index < OUTPUT_SLOT && resource.getItem() instanceof DroneModuleItem;
    }

    @Override
    protected int getSlotLimit(int index) {
        return 1;
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> progress;
            case 1 -> ASSEMBLY_TIME;
            case 2 -> assembling ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 3;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        output.putBoolean("assembling", assembling);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("progress", 0);
        assembling = input.getBooleanOr("assembling", false);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroneAssemblerMenu(containerId, inventory, this, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
