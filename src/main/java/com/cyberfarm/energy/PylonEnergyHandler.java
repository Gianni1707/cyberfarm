package com.cyberfarm.energy;

import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * The energy port of a Stabilizer Pylon: whatever goes in or out goes to the Resonance Core the
 * column is linked to, within the core's limits. An unlinked pylon takes and gives nothing. The
 * core is looked up on every call, so cables keep working as pylons are linked and unlinked.
 */
public class PylonEnergyHandler implements EnergyHandler {
    private final Level level;
    private final BlockPos pos;

    public PylonEnergyHandler(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    private @Nullable EnergyHandler core() {
        ResonanceCoreBlockEntity core = ResonanceCoreBlockEntity.linkedCore(level, pos);
        return core != null ? core.getEnergy() : null;
    }

    @Override
    public long getAmountAsLong() {
        EnergyHandler core = core();
        return core != null ? core.getAmountAsLong() : 0;
    }

    @Override
    public long getCapacityAsLong() {
        EnergyHandler core = core();
        return core != null ? core.getCapacityAsLong() : 0;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        EnergyHandler core = core();
        return core != null ? core.insert(amount, transaction) : 0;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        EnergyHandler core = core();
        return core != null ? core.extract(amount, transaction) : 0;
    }
}
