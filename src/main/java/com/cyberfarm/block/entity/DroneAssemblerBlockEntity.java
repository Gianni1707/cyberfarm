package com.cyberfarm.block.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import com.cyberfarm.drone.AddonType;
import com.cyberfarm.drone.DroneModules;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.item.DroneAddonItem;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.item.DroneItem;
import com.cyberfarm.item.DroneModuleItem;
import com.cyberfarm.menu.DroneAssemblerMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
import org.jspecify.annotations.Nullable;

/**
 * Builds a drone from a frame, up to three function modules and up to four stacks of addons.
 * The frame tier decides how many module slots are usable and how many addons fit.
 * A finished drone put back in the output slot can be disassembled into its parts.
 * Assembly is started from the GUI and costs FE over time.
 */
public class DroneAssemblerBlockEntity extends MachineBlockEntity {
    public static final int FRAME_SLOT = 0;
    public static final int FIRST_MODULE_SLOT = 1;
    public static final int MODULE_SLOTS = 3;
    public static final int FIRST_ADDON_SLOT = FIRST_MODULE_SLOT + MODULE_SLOTS;
    public static final int ADDON_SLOTS = 4;
    public static final int OUTPUT_SLOT = FIRST_ADDON_SLOT + ADDON_SLOTS;
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

    public static boolean isModuleSlot(int index) {
        return index >= FIRST_MODULE_SLOT && index < FIRST_ADDON_SLOT;
    }

    public static boolean isAddonSlot(int index) {
        return index >= FIRST_ADDON_SLOT && index < OUTPUT_SLOT;
    }

    /**
     * Shared by the server and the GUI: a frame, at least one module, no duplicate modules,
     * nothing in slots the frame tier has not unlocked, addons within the frame's limits,
     * and an empty output slot.
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
            if (i >= frame.getModuleSlots() || !fits(frame, stack.getItem()) || !modules.add(stack.getItem())) {
                return false;
            }
        }
        if (modules.isEmpty() || countAddons(slots, null) > frame.getAddonLimit()) {
            return false;
        }
        for (AddonType type : AddonType.values()) {
            int count = countAddons(slots, type);
            if (count > type.getMaxPerDrone()) {
                return false;
            }
            if (count == 0) {
                continue;
            }
            for (AddonType other : AddonType.values()) {
                if (type.conflictsWith(other) && countAddons(slots, other) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Addons in the addon slots; {@code type} null counts all of them. */
    public static int countAddons(IntFunction<ItemStack> slots, @Nullable AddonType type) {
        int count = 0;
        for (int i = FIRST_ADDON_SLOT; i < OUTPUT_SLOT; i++) {
            ItemStack stack = slots.apply(i);
            if (stack.getItem() instanceof DroneAddonItem addon && (type == null || addon.getAddonType() == type)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public boolean canAssemble() {
        return isValidLayout(this::getStack);
    }

    /** A drone sits in the output slot and every part slot is free to receive its parts. */
    public static boolean canDisassemble(IntFunction<ItemStack> slots) {
        if (!(slots.apply(OUTPUT_SLOT).getItem() instanceof DroneItem)) {
            return false;
        }
        for (int i = FRAME_SLOT; i < OUTPUT_SLOT; i++) {
            if (!slots.apply(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Puts the parts of the drone in the output slot back into the frame, module and addon slots,
     * so it can be changed and assembled again. Its charge moves into the assembler's buffer.
     */
    public void disassemble(ServerLevel level) {
        if (assembling || !canDisassemble(this::getStack)) {
            return;
        }
        ItemStack drone = getStack(OUTPUT_SLOT);
        energy.generate(DroneItem.getEnergy(drone));
        setStack(OUTPUT_SLOT, ItemStack.EMPTY);

        int moduleSlot = FIRST_MODULE_SLOT;
        Map<Item, Integer> addons = new LinkedHashMap<>();
        for (Item part : DroneItem.getModules(drone).parts()) {
            if (part instanceof DroneFrameItem) {
                setStack(FRAME_SLOT, new ItemStack(part));
            } else if (part instanceof DroneModuleItem && moduleSlot < FIRST_ADDON_SLOT) {
                setStack(moduleSlot++, new ItemStack(part));
            } else if (part instanceof DroneAddonItem) {
                addons.merge(part, 1, Integer::sum);
            }
        }
        // A drone is built from at most four addon stacks, so they always fit back.
        int addonSlot = FIRST_ADDON_SLOT;
        for (Map.Entry<Item, Integer> addon : addons.entrySet()) {
            if (addonSlot < OUTPUT_SLOT) {
                setStack(addonSlot++, new ItemStack(addon.getKey(), addon.getValue()));
            }
        }
        level.playSound(null, worldPosition, ModSounds.ASSEMBLER_WORK.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    public void startAssembly() {
        if (!assembling && canAssemble()) {
            assembling = true;
            progress = 0;
            setChanged();
        }
    }

    @Override
    protected float progress() {
        return progress / (float) ASSEMBLY_TIME;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!assembling) {
            return;
        }
        if (!canAssemble()) {
            resetAssembly();
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

    private void resetAssembly() {
        assembling = false;
        progress = 0;
    }

    private void finishAssembly(ServerLevel level, BlockPos pos) {
        // Addons are stored once per item, so three Speed Addons appear three times.
        List<Item> parts = new ArrayList<>();
        for (int i = FRAME_SLOT; i < OUTPUT_SLOT; i++) {
            ItemStack stack = getStack(i);
            for (int n = 0; n < stack.getCount(); n++) {
                parts.add(stack.getItem());
            }
            setStack(i, ItemStack.EMPTY);
        }
        ItemStack drone = new ItemStack(ModItems.DRONE.get());
        drone.set(ModDataComponents.DRONE_MODULES.get(), new DroneModules(parts));
        drone.set(ModDataComponents.ENERGY.get(), 0);
        setStack(OUTPUT_SLOT, drone);

        resetAssembly();
        level.playSound(null, pos, ModSounds.ASSEMBLER_WORK.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    /**
     * Shared by the server and the GUI: frames go in the frame slot; a module only goes in a slot
     * the current frame has unlocked, and only if no other slot holds the same module; an addon
     * needs a frame and must not conflict with the addons already installed.
     */
    /** The Builder Module and the Mini Frame go only together: building is work for a swarm of small drones. */
    public static boolean fits(DroneFrameItem frame, Item module) {
        boolean builder = module instanceof DroneModuleItem moduleItem && moduleItem.getTask() == DroneTask.BUILD;
        return builder == (frame == ModItems.MINI_FRAME.get());
    }

    public static boolean canPlace(IntFunction<ItemStack> slots, int index, ItemResource resource) {
        if (index == FRAME_SLOT) {
            return resource.getItem() instanceof DroneFrameItem;
        }
        if (index == OUTPUT_SLOT) {
            // A finished drone can be put back to disassemble it
            return resource.getItem() instanceof DroneItem;
        }
        if (!(slots.apply(FRAME_SLOT).getItem() instanceof DroneFrameItem frame)) {
            return false;
        }
        if (isModuleSlot(index)) {
            if (!(resource.getItem() instanceof DroneModuleItem) || index - FIRST_MODULE_SLOT >= frame.getModuleSlots()
                    || !fits(frame, resource.getItem())) {
                return false;
            }
            for (int i = FIRST_MODULE_SLOT; i < FIRST_ADDON_SLOT; i++) {
                if (i != index && slots.apply(i).is(resource.getItem())) {
                    return false;
                }
            }
            return true;
        }
        if (isAddonSlot(index) && resource.getItem() instanceof DroneAddonItem addon) {
            for (AddonType other : AddonType.values()) {
                if (addon.getAddonType().conflictsWith(other) && countAddons(slots, other) > 0) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * How many of {@code resource} the slot may hold: 1 for frame and modules; for addons, what is
     * already there plus the room left under the frame limit and the addon's own maximum.
     */
    public static int slotCapacity(IntFunction<ItemStack> slots, int index, ItemResource resource) {
        if (!isAddonSlot(index) || !(resource.getItem() instanceof DroneAddonItem addon)) {
            return 1;
        }
        int limit = slots.apply(FRAME_SLOT).getItem() instanceof DroneFrameItem frame ? frame.getAddonLimit() : 0;
        AddonType type = addon.getAddonType();
        int room = Math.min(limit - countAddons(slots, null), type.getMaxPerDrone() - countAddons(slots, type));
        ItemStack current = slots.apply(index);
        int here = current.is(resource.getItem()) ? current.getCount() : 0;
        return Math.max(0, here + room);
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return canPlace(this::getStack, index, resource);
    }

    @Override
    protected int getSlotLimit(int index, ItemResource resource) {
        return slotCapacity(this::getStack, index, resource);
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
