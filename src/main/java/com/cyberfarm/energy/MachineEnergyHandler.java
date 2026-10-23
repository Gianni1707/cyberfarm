package com.cyberfarm.energy;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * FE buffer used by every CyberFarm machine. The insert/extract limits only apply to other mods
 * (cables, other machines); the machine itself changes the buffer through the helpers below.
 */
public class MachineEnergyHandler extends SimpleEnergyHandler {
    private final Runnable onChanged;

    public MachineEnergyHandler(int capacity, int maxInsert, int maxExtract, Runnable onChanged) {
        super(capacity, maxInsert, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChanged.run();
    }

    public int getEnergy() {
        return energy;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isFull() {
        return energy >= capacity;
    }

    /** Resizes the buffer, e.g. when cells go in or out; energy that no longer fits is lost. */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        if (energy > capacity) {
            set(capacity);
        }
    }

    /** Changes how much other mods may put in and take out per call; 0 closes that way. */
    public void setTransferLimits(int maxInsert, int maxExtract) {
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
    }

    /** Adds energy produced by the machine itself. Returns the amount actually stored. */
    public int generate(int amount) {
        int added = Math.min(amount, capacity - energy);
        if (added > 0) {
            set(energy + added);
        }
        return added;
    }

    /** Uses exactly {@code amount} FE, or nothing at all if there is not enough. */
    public boolean consume(int amount) {
        if (energy < amount) {
            return false;
        }
        set(energy - amount);
        return true;
    }

    /** Takes up to {@code max} FE out of the buffer. Returns the amount taken. */
    public int drain(int max) {
        int drained = Math.min(max, energy);
        if (drained > 0) {
            set(energy - drained);
        }
        return drained;
    }
}
