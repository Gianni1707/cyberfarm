package com.cyberfarm.fluid;

import java.util.function.Predicate;

import com.cyberfarm.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/**
 * One-fluid tank of a CyberFarm machine, exposed to other mods as a NeoForge fluid handler.
 * {@code acceptsFromOutside} filters what pipes may push in; the machine itself fills and
 * drains it directly.
 */
public class GasTank extends FluidStacksResourceHandler {
    private final int capacity;
    private final Predicate<FluidResource> acceptsFromOutside;
    private final Runnable onChanged;

    public GasTank(int capacity, Predicate<FluidResource> acceptsFromOutside, Runnable onChanged) {
        super(1, capacity);
        this.capacity = capacity;
        this.acceptsFromOutside = acceptsFromOutside;
        this.onChanged = onChanged;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return acceptsFromOutside.test(resource);
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        onChanged.run();
    }

    public Fluid fluid() {
        FluidResource resource = getResource(0);
        return resource.isEmpty() ? Fluids.EMPTY : resource.getFluid();
    }

    public int amount() {
        return getAmountAsInt(0);
    }

    public int capacity() {
        return capacity;
    }

    /** Adds gas made by the machine itself. Returns how much fit. */
    public int fill(Fluid fluid, int amount) {
        if (amount() > 0 && fluid() != fluid) {
            return 0;
        }
        int added = Math.min(amount, capacity - amount());
        if (added > 0) {
            set(0, FluidResource.of(fluid), amount() + added);
        }
        return added;
    }

    /** Removes gas used by the machine itself. Returns how much was taken. */
    public int drain(int amount) {
        int drained = Math.min(amount, amount());
        if (drained > 0) {
            int left = amount() - drained;
            set(0, left == 0 ? FluidResource.EMPTY : getResource(0), left);
        }
        return drained;
    }

    /** Pushes up to {@code maxPerSide} mB into every adjacent block that accepts it, from any mod. */
    public void pushToNeighbors(Level level, BlockPos pos, int maxPerSide) {
        for (Direction direction : Direction.values()) {
            if (amount() <= 0) {
                return;
            }
            ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, pos.relative(direction), direction.getOpposite());
            if (target != null) {
                ResourceHandlerUtil.move(this, target, resource -> true, maxPerSide, null);
            }
        }
    }

    // GUI sync: the fluid travels as a small code in a container data slot

    public int fluidCode() {
        Fluid fluid = fluid();
        if (fluid == Fluids.EMPTY) {
            return 0;
        }
        if (fluid == ModFluids.BIOGAS.get()) {
            return 1;
        }
        if (fluid == ModFluids.METHANE.get()) {
            return 2;
        }
        if (ModFluids.isSteam(getResource(0))) {
            return 4;
        }
        if (fluid == ModFluids.COOLANT.get()) {
            return 6;
        }
        if (fluid == ModFluids.CRYO_COOLANT.get()) {
            return 7;
        }
        return fluid == Fluids.WATER ? 5 : 3;
    }

    public static boolean isFuelGas(FluidResource resource) {
        return resource.is(ModFluids.BIOGAS.get()) || resource.is(ModFluids.METHANE.get());
    }
}
