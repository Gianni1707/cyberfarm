package com.cyberfarm.fluid;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Several tanks of a machine exposed to pipes as one fluid handler: input tanks only take fluid
 * in (what each accepts is decided by its own {@link GasTank}), output tanks only give it out.
 */
public class TankGroup implements ResourceHandler<FluidResource> {
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
    public int size() {
        return tanks.length;
    }

    @Override
    public FluidResource getResource(int index) {
        return tanks[index].getResource(0);
    }

    @Override
    public long getAmountAsLong(int index) {
        return tanks[index].amount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return tanks[index].capacity();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !output[index] && tanks[index].isValid(0, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return output[index] ? 0 : tanks[index].insert(0, resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return output[index] ? tanks[index].extract(0, resource, amount, transaction) : 0;
    }
}
