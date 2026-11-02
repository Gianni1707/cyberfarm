package com.cyberfarm.energy;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraftforge.energy.IEnergyStorage;

/**
 * The energy port of a block that only fronts for another one (a Stabilizer Pylon for its Resonance
 * Core, a dynamo for its Steam Engine): every call goes to the handler {@code target} finds right
 * then, or does nothing when there is none. Looked up on every call, so cables keep working as the
 * structure is built, linked or broken.
 */
public class ForwardingEnergyHandler implements IEnergyStorage {
    private final Supplier<IEnergyStorage> target;

    public ForwardingEnergyHandler(Supplier<IEnergyStorage> target) {
        this.target = target;
    }

    private @Nullable IEnergyStorage target() {
        return target.get();
    }

    @Override
    public int getEnergyStored() {
        IEnergyStorage handler = target();
        return handler != null ? handler.getEnergyStored() : 0;
    }

    @Override
    public int getMaxEnergyStored() {
        IEnergyStorage handler = target();
        return handler != null ? handler.getMaxEnergyStored() : 0;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        IEnergyStorage handler = target();
        return handler != null ? handler.receiveEnergy(amount, simulate) : 0;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        IEnergyStorage handler = target();
        return handler != null ? handler.extractEnergy(amount, simulate) : 0;
    }

    @Override
    public boolean canReceive() {
        IEnergyStorage handler = target();
        return handler != null && handler.canReceive();
    }

    @Override
    public boolean canExtract() {
        IEnergyStorage handler = target();
        return handler != null && handler.canExtract();
    }
}
