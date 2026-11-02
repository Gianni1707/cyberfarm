package com.cyberfarm.fluid;

import java.util.function.Predicate;

import com.cyberfarm.port.Caps;
import com.cyberfarm.port.FluidResource;
import com.cyberfarm.port.ValueInput;
import com.cyberfarm.port.ValueOutput;
import com.cyberfarm.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * One-fluid tank of a CyberFarm machine, exposed to other mods as a Forge fluid handler.
 * {@code acceptsFromOutside} filters what pipes may push in; the machine itself fills and
 * drains it directly.
 */
public class GasTank extends FluidTank {
    private final Predicate<FluidResource> acceptsFromOutside;
    private final Runnable onChanged;

    public GasTank(int capacity, Predicate<FluidResource> acceptsFromOutside, Runnable onChanged) {
        super(capacity);
        this.acceptsFromOutside = acceptsFromOutside;
        this.onChanged = onChanged;
    }

    @Override
    public boolean isFluidValid(FluidStack stack) {
        return acceptsFromOutside.test(FluidResource.of(stack));
    }

    public boolean isValid(int index, FluidResource resource) {
        return acceptsFromOutside.test(resource);
    }

    @Override
    protected void onContentsChanged() {
        onChanged.run();
    }

    public FluidResource getResource(int index) {
        return FluidResource.of(getFluid());
    }

    public Fluid fluid() {
        return getFluid().isEmpty() ? Fluids.EMPTY : getFluid().getFluid();
    }

    public int amount() {
        return getFluidAmount();
    }

    public int capacity() {
        return getCapacity();
    }

    /** Adds gas made by the machine itself. Returns how much fit. */
    public int fill(Fluid fluid, int amount) {
        if (amount() > 0 && fluid() != fluid) {
            return 0;
        }
        int added = Math.min(amount, capacity - amount());
        if (added > 0) {
            this.fluid = new FluidStack(fluid, amount() + added);
            onContentsChanged();
        }
        return added;
    }

    /** Removes gas used by the machine itself. Returns how much was taken. */
    public int drain(int amount) {
        int drained = Math.min(amount, amount());
        if (drained > 0) {
            int left = amount() - drained;
            if (left == 0) {
                this.fluid = FluidStack.EMPTY;
            } else {
                this.fluid.setAmount(left);
            }
            onContentsChanged();
        }
        return drained;
    }

    /** Pushes up to {@code maxPerSide} mB into every adjacent block that accepts it, from any mod. */
    public void pushToNeighbors(Level level, BlockPos pos, int maxPerSide) {
        for (Direction direction : Direction.values()) {
            if (amount() <= 0) {
                return;
            }
            IFluidHandler target = Caps.fluids(level, pos.relative(direction), direction.getOpposite());
            if (target != null) {
                FluidUtil.tryFluidTransfer(target, this, maxPerSide, true);
            }
        }
    }

    public void serialize(ValueOutput output) {
        writeToNBT(output.tag());
    }

    public void deserialize(ValueInput input) {
        readFromNBT(input.tag());
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
        if (fluid == ModFluids.ACID.get()) {
            return 8;
        }
        return fluid == Fluids.WATER ? 5 : 3;
    }

    public static boolean isFuelGas(FluidResource resource) {
        return resource.is(ModFluids.BIOGAS.get()) || resource.is(ModFluids.METHANE.get());
    }
}
