package com.cyberfarm.multiblock;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/** A multiblock controller that its ports expose to pipes, cables and hoppers while it is formed. */
public interface PortOwner {
    boolean isFormed();

    default @Nullable ResourceHandler<ItemResource> portItems() {
        return null;
    }

    default @Nullable ResourceHandler<FluidResource> portFluids() {
        return null;
    }

    default @Nullable EnergyHandler portEnergy() {
        return null;
    }
}
