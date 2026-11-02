package com.cyberfarm.multiblock;

import net.minecraftforge.items.IItemHandler;

import net.minecraftforge.fluids.capability.IFluidHandler;

import net.minecraftforge.energy.IEnergyStorage;
import com.cyberfarm.port.FluidResource;
import com.cyberfarm.port.ItemResource;
import org.jspecify.annotations.Nullable;

/** A multiblock controller that its ports expose to pipes, cables and hoppers while it is formed. */
public interface PortOwner {
    boolean isFormed();

    default @Nullable IItemHandler portItems() {
        return null;
    }

    default @Nullable IFluidHandler portFluids() {
        return null;
    }

    default @Nullable IEnergyStorage portEnergy() {
        return null;
    }
}
