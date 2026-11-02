package com.cyberfarm.fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Several tanks of a machine exposed to pipes as one fluid handler: input tanks only take fluid
 * in (what each accepts is decided by its own {@link GasTank}), output tanks only give it out.
 */
public class TankGroup implements IFluidHandler {
    private final GasTank[] tanks;
    private final boolean[] output;

    private TankGroup(GasTank[] tanks, boolean[] output) {
        this.tanks = tanks;
        this.output = output;
    }

    public static TankGroup of(GasTank[] inputs, GasTank... outputs) {
        GasTank[] tanks = new GasTank[inputs.length + outputs.length];
        boolean[] output = new boolean[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = i < inputs.length ? inputs[i] : outputs[i - inputs.length];
            output[i] = i >= inputs.length;
        }
        return new TankGroup(tanks, output);
    }

    @Override
    public int getTanks() {
        return tanks.length;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tanks[tank].getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tanks[tank].capacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return !output[tank] && tanks[tank].isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        for (int i = 0; i < tanks.length; i++) {
            if (!output[i]) {
                int filled = tanks[i].fill(resource, action);
                if (filled > 0) {
                    return filled;
                }
            }
        }
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        for (int i = 0; i < tanks.length; i++) {
            if (output[i]) {
                FluidStack drained = tanks[i].drain(resource, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (int i = 0; i < tanks.length; i++) {
            if (output[i]) {
                FluidStack drained = tanks[i].drain(maxDrain, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
        }
        return FluidStack.EMPTY;
    }
}
