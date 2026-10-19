package com.cyberfarm.drone;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.drone.job.DroneJob;
import com.cyberfarm.drone.job.HarvestJob;
import com.cyberfarm.drone.job.LogJob;
import com.cyberfarm.drone.job.MineJob;
import com.cyberfarm.drone.job.PlantJob;
import com.cyberfarm.drone.job.TransportJob;
import com.cyberfarm.entity.DroneEntity;

import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/** The kind of work a function module gives a drone, and how to find the next job of that kind. */
public enum DroneTask {
    HARVEST(HarvestJob::find),
    QUARRY(MineJob::find),
    TRANSPORT(TransportJob::find),
    PLANT(PlantJob::find),
    LOG(LogJob::find),
    /** Taxi trips start when a player right-clicks the waiting drone, never on their own. */
    TAXI((drone, level, dock) -> null);

    @FunctionalInterface
    public interface JobFinder {
        @Nullable DroneJob find(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock);
    }

    private final JobFinder finder;

    DroneTask(JobFinder finder) {
        this.finder = finder;
    }

    public @Nullable DroneJob findJob(DroneEntity drone, ServerLevel level, DroneDockBlockEntity dock) {
        return finder.find(drone, level, dock);
    }

    /** Bit used to sync the installed modules to the client, for the drone model. */
    public int bit() {
        return 1 << ordinal();
    }
}
