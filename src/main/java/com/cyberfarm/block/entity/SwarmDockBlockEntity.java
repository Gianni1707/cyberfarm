package com.cyberfarm.block.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Builder Swarm Dock: a Drone Dock for up to four Mini Drones at once. They share its work area,
 * route, build and stock, park on the corners of its pad, and the screen shows them as one: the
 * busiest one's job, their batteries added up.
 */
public class SwarmDockBlockEntity extends DroneDockBlockEntity {
    public static final int MAX_DRONES = 4;
    /** Reports older than this are from a drone that left (picked up, unloaded): the screen ignores them. */
    private static final int REPORT_TIMEOUT = 100;

    private record Report(long tick, int status, int energy, int capacity, int task, int modules, BlockPos pos) {}

    /** The drones in the order they arrived, which is also the corner of the pad each parks on. */
    private final List<UUID> drones = new ArrayList<>();
    private final Map<UUID, Report> reports = new HashMap<>();

    public SwarmDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWARM_DOCK.get(), pos, state);
    }

    /** Full when four live drones are on it; drones that are gone free their place. */
    @Override
    public boolean hasLiveDrone(ServerLevel level) {
        drones.removeIf(id -> !(level.getEntity(id) instanceof DroneEntity drone && drone.isAlive()));
        return drones.size() >= MAX_DRONES;
    }

    @Override
    public void setDrone(@Nullable UUID id) {
        if (id != null && !drones.contains(id)) {
            drones.add(id);
            setChanged();
        }
    }

    @Override
    public void releaseDrone(UUID id) {
        drones.remove(id);
        reports.remove(id);
        setChanged();
    }

    /** The corner of the pad {@code drone} parks on (0-3), or -1 if it is not one of this dock's. */
    public int parkingSpot(UUID drone) {
        return drones.indexOf(drone);
    }

    @Override
    protected int droneCount() {
        return drones.size();
    }

    @Override
    protected int maxDrones() {
        return MAX_DRONES;
    }

    @Override
    public void updateDroneStatus(UUID drone, long gameTime, int status, int energy, int capacity, int task, int modules, BlockPos pos) {
        reports.put(drone, new Report(gameTime, status, energy, capacity, task, modules, pos));
        reports.values().removeIf(report -> gameTime - report.tick() > REPORT_TIMEOUT);
        // The busiest drone stands for the swarm: working, then flying home, then charging, then idle
        Report shown = reports.values().stream().max(Comparator.comparingInt(report -> rank(report.status()))).orElseThrow();
        int totalEnergy = reports.values().stream().mapToInt(Report::energy).sum();
        int totalCapacity = reports.values().stream().mapToInt(Report::capacity).sum();
        int allModules = reports.values().stream().mapToInt(Report::modules).reduce(0, (a, b) -> a | b);
        super.updateDroneStatus(drone, gameTime, shown.status(), totalEnergy, totalCapacity, shown.task(), allModules, shown.pos());
    }

    private static int rank(int status) {
        return switch (status) {
            case DroneEntity.STATUS_WORKING -> 3;
            case DroneEntity.STATUS_RETURNING -> 2;
            case DroneEntity.STATUS_CHARGING -> 1;
            default -> 0;
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("drones", UUIDUtil.CODEC.listOf(), List.copyOf(drones));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        drones.clear();
        drones.addAll(input.read("drones", UUIDUtil.CODEC.listOf()).orElse(List.of()));
    }
}
