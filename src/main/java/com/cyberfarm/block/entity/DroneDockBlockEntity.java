package com.cyberfarm.block.entity;

import java.util.UUID;

import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.menu.DroneDockMenu;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
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

    private @Nullable BoundingBox workArea;
    private @Nullable UUID droneId;
    private int droneStatus;
    private int droneEnergyPercent;
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

    public void setWorkArea(BoundingBox area) {
        workArea = area;
        setChanged();
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

    public void updateDroneStatus(long gameTime, int status, int energyPercent) {
        droneStatus = status;
        droneEnergyPercent = energyPercent;
        lastStatusTick = gameTime;
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

    @Override
    protected int getExtraData(int id) {
        BoundingBox area = getWorkArea();
        return switch (id) {
            case 0 -> area.getXSpan();
            case 1 -> area.getZSpan();
            case 2 -> droneStatus;
            case 3 -> droneEnergyPercent;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 4;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("work_area", BoundingBox.CODEC, workArea);
        output.storeNullable("drone", UUIDUtil.CODEC, droneId);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        workArea = input.read("work_area", BoundingBox.CODEC).orElse(null);
        droneId = input.read("drone", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroneDockMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
