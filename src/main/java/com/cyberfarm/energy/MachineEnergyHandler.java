package com.cyberfarm.energy;

import com.cyberfarm.port.ValueInput;
import com.cyberfarm.port.ValueOutput;

import net.minecraftforge.energy.EnergyStorage;

/**
 * FE buffer used by every CyberFarm machine. The insert/extract limits only apply to other mods
 * (cables, other machines); the machine itself changes the buffer through the helpers below.
 */
public class MachineEnergyHandler extends EnergyStorage {
    private final Runnable onChanged;
    /** What the machine itself made and used since {@link #takeGenerated}/{@link #takeUsed} last read it. */
    private int generated;
    private int used;

    public MachineEnergyHandler(int capacity, int maxInsert, int maxExtract, Runnable onChanged) {
        super(capacity, maxInsert, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate) {
            onChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0 && !simulate) {
            onChanged.run();
        }
        return extracted;
    }

    private void set(int amount) {
        energy = amount;
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
        this.maxReceive = maxInsert;
        this.maxExtract = maxExtract;
    }

    /** Adds energy produced by the machine itself. Returns the amount actually stored. */
    public int generate(int amount) {
        int added = Math.min(amount, capacity - energy);
        if (added > 0) {
            set(energy + added);
            generated += added;
        }
        return added;
    }

    /** Uses exactly {@code amount} FE, or nothing at all if there is not enough. */
    public boolean consume(int amount) {
        if (energy < amount) {
            return false;
        }
        set(energy - amount);
        used += amount;
        return true;
    }

    /** Takes up to {@code max} FE out of the buffer. Returns the amount taken. */
    public int drain(int max) {
        int drained = Math.min(max, energy);
        if (drained > 0) {
            set(energy - drained);
            used += drained;
        }
        return drained;
    }

    public int takeGenerated() {
        int value = generated;
        generated = 0;
        return value;
    }

    public int takeUsed() {
        int value = used;
        used = 0;
        return value;
    }

    public void serialize(ValueOutput output) {
        output.putInt("energy", energy);
    }

    public void deserialize(ValueInput input) {
        energy = Math.min(input.getIntOr("energy", 0), capacity);
    }
}
