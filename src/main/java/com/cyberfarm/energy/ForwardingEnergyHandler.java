package com.cyberfarm.energy;

import java.util.function.Supplier;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * The energy port of a block that only fronts for another one (a Stabilizer Pylon for its Resonance
 * Core, a dynamo for its Steam Engine): every call goes to the handler {@code target} finds right
 * then, or does nothing when there is none. Looked up on every call, so cables keep working as the
 * structure is built, linked or broken.
 */
public class ForwardingEnergyHandler implements EnergyHandler {
    private final Supplier<@Nullable EnergyHandler> target;

    public ForwardingEnergyHandler(Supplier<@Nullable EnergyHandler> target) {
        this.target = target;
    }

    @Override
    public long getAmountAsLong() {
        EnergyHandler handler = target.get();
        return handler != null ? handler.getAmountAsLong() : 0;
    }

    @Override
    public long getCapacityAsLong() {
        EnergyHandler handler = target.get();
        return handler != null ? handler.getCapacityAsLong() : 0;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        EnergyHandler handler = target.get();
        return handler != null ? handler.insert(amount, transaction) : 0;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        EnergyHandler handler = target.get();
        return handler != null ? handler.extract(amount, transaction) : 0;
    }
}
