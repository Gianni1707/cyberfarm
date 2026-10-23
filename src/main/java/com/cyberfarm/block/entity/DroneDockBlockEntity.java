package com.cyberfarm.block.entity;

import java.util.UUID;
import java.util.function.Predicate;

import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.menu.DroneDockMenu;
import com.cyberfarm.menu.ItemFilterMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Home base of a drone: stores the work area, charges the drone and receives its harvest.
 * Without a surveyed area the drone works a 9x9 square centred on the dock.
 */
public class DroneDockBlockEntity extends MachineBlockEntity {
    public static final int CARGO_SLOTS = 9;
    public static final int CAPACITY = 100_000;
    public static final int MAX_INPUT = 1_000;
    public static final int CHARGE_RATE = 500;
    public static final int DEFAULT_RADIUS = 4;
    /** Ticks without news from the drone before the dock shows it as offline. */
    private static final int STATUS_TIMEOUT = 100;
    /** Data slots after the energy ones; see {@link #getExtraData}. */
    public static final int EXTRA_DATA = 21;

    private @Nullable BoundingBox workArea;
    /** Next layer the Quarry Module digs; unset means the top of the work area. */
    private @Nullable Integer quarryLayer;
    private @Nullable BlockPos transportSource;
    private @Nullable BlockPos transportDestination;
    private @Nullable BlockPos taxiDestination;
    private @Nullable UUID droneId;
    /** What the Quarry keeps (the rest is thrown away) and what the Transport carries. */
    private final ItemFilter filter = new ItemFilter(this::setChanged);
    /** Set by the Return button: the drone comes home and takes no new jobs until resumed. */
    private boolean paused;
    // What the drone last reported, shown in the GUI
    private int droneStatus;
    private int droneEnergy;
    private int droneCapacity;
    /** {@code DroneTask} ordinal + 1 of the job in progress, 0 for none. */
    private int droneTask;
    private int droneModules;
    private BlockPos dronePos = BlockPos.ZERO;
    private long lastStatusTick;

    public DroneDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE_DOCK.get(), pos, state, CARGO_SLOTS, CAPACITY, MAX_INPUT, 0);
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (droneStatus != DroneEntity.STATUS_NONE && level.getGameTime() - lastStatusTick > STATUS_TIMEOUT) {
            droneStatus = DroneEntity.STATUS_NONE;
        }
    }

    public BoundingBox getWorkArea() {
        if (workArea != null) {
            return workArea;
        }
        BlockPos pos = getBlockPos();
        return new BoundingBox(
                pos.getX() - DEFAULT_RADIUS, pos.getY() - 2, pos.getZ() - DEFAULT_RADIUS,
                pos.getX() + DEFAULT_RADIUS, pos.getY() + 3, pos.getZ() + DEFAULT_RADIUS);
    }

    /** Sets the work area; {@code null} goes back to the default square around the dock. */
    public void setWorkArea(@Nullable BoundingBox area) {
        workArea = area;
        quarryLayer = null;
        setChanged();
    }

    public boolean hasCustomArea() {
        return workArea != null;
    }

    public int getQuarryLayer() {
        return quarryLayer != null ? quarryLayer : getWorkArea().maxY();
    }

    public void setQuarryLayer(int y) {
        quarryLayer = y;
        setChanged();
    }

    public @Nullable BlockPos getTransportSource() {
        return transportSource;
    }

    public @Nullable BlockPos getTransportDestination() {
        return transportDestination;
    }

    public @Nullable BlockPos getTaxiDestination() {
        return taxiDestination;
    }

    /** Where a Taxi drone carries players; null clears it. */
    public void setTaxiDestination(@Nullable BlockPos destination) {
        taxiDestination = destination;
        setChanged();
    }

    /** Sets the Transport Module route; pass nulls to clear it. */
    public void setTransportRoute(@Nullable BlockPos source, @Nullable BlockPos destination) {
        transportSource = source;
        transportDestination = destination;
        setChanged();
    }

    public ItemFilter filter() {
        return filter;
    }

    public void openFilter(Player player) {
        player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new ItemFilterMenu(id, inventory, filter,
                ContainerLevelAccess.create(level, worldPosition), getBlockState().getBlock()),
                Component.translatable("gui.cyberfarm.dock.filter_title")));
    }

    public boolean hasLiveDrone(ServerLevel level) {
        return droneId != null && level.getEntity(droneId) instanceof DroneEntity drone && drone.isAlive();
    }

    public void setDrone(@Nullable UUID id) {
        droneId = id;
        setChanged();
    }

    public void releaseDrone(UUID id) {
        if (id.equals(droneId)) {
            setDrone(null);
            droneStatus = DroneEntity.STATUS_NONE;
        }
    }

    public void updateDroneStatus(long gameTime, int status, int energy, int capacity, int task, int modules, BlockPos pos) {
        droneStatus = status;
        droneEnergy = energy;
        droneCapacity = capacity;
        droneTask = task;
        droneModules = modules;
        dronePos = pos;
        lastStatusTick = gameTime;
    }

    public boolean isPaused() {
        return paused;
    }

    public void togglePaused() {
        paused = !paused;
        setChanged();
    }

    /** Takes up to {@code max} FE out of the dock to recharge the drone. */
    public int charge(int max) {
        return energy.drain(Math.min(max, CHARGE_RATE));
    }

    /** Stores a harvested stack in the dock. Returns whatever did not fit. */
    public ItemStack deposit(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = ResourceHandlerUtil.insertStacking(items, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    /** Takes up to {@code max} items of the first stack matching {@code filter}, e.g. seeds for a Planter. */
    public ItemStack withdraw(Predicate<ItemStack> filter, int max) {
        int slot = findSlot(filter);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = getStack(slot);
        int taken = Math.min(max, stack.getCount());
        setStack(slot, taken == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - taken));
        return stack.copyWithCount(taken);
    }

    public boolean contains(Predicate<ItemStack> filter) {
        return findSlot(filter) >= 0;
    }

    /** Index of the first stored stack matching {@code filter}, or -1. */
    private int findSlot(Predicate<ItemStack> filter) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected int getExtraData(int id) {
        BoundingBox area = getWorkArea();
        return switch (id) {
            case 0 -> area.getXSpan();
            case 1 -> area.getZSpan();
            case 2 -> droneStatus;
            case 3 -> droneCapacity > 0 ? (int) ((long) droneEnergy * 100 / droneCapacity) : 0;
            case 4 -> transportSource != null && transportDestination != null ? 1 : 0;
            case 5 -> quarryLayer != null ? 1 : 0;
            case 6 -> getQuarryLayer();
            case 7 -> droneTask;
            case 8 -> droneModules;
            case 9 -> EnergyUtil.low(dronePos.getX());
            case 10 -> EnergyUtil.high(dronePos.getX());
            case 11 -> EnergyUtil.low(dronePos.getY());
            case 12 -> EnergyUtil.high(dronePos.getY());
            case 13 -> EnergyUtil.low(dronePos.getZ());
            case 14 -> EnergyUtil.high(dronePos.getZ());
            case 15 -> EnergyUtil.low(droneEnergy);
            case 16 -> EnergyUtil.high(droneEnergy);
            case 17 -> EnergyUtil.low(droneCapacity);
            case 18 -> EnergyUtil.high(droneCapacity);
            case 19 -> paused ? 1 : 0;
            case 20 -> taxiDestination != null ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return EXTRA_DATA;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("work_area", BoundingBox.CODEC, workArea);
        output.storeNullable("quarry_layer", Codec.INT, quarryLayer);
        output.storeNullable("transport_source", BlockPos.CODEC, transportSource);
        output.storeNullable("transport_destination", BlockPos.CODEC, transportDestination);
        output.storeNullable("taxi_destination", BlockPos.CODEC, taxiDestination);
        output.storeNullable("drone", UUIDUtil.CODEC, droneId);
        output.putBoolean("paused", paused);
        filter.save(output.child("filter"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        workArea = input.read("work_area", BoundingBox.CODEC).orElse(null);
        quarryLayer = input.read("quarry_layer", Codec.INT).orElse(null);
        transportSource = input.read("transport_source", BlockPos.CODEC).orElse(null);
        transportDestination = input.read("transport_destination", BlockPos.CODEC).orElse(null);
        taxiDestination = input.read("taxi_destination", BlockPos.CODEC).orElse(null);
        droneId = input.read("drone", UUIDUtil.CODEC).orElse(null);
        paused = input.getBooleanOr("paused", false);
        filter.load(input.childOrEmpty("filter"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroneDockMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
