package com.cyberfarm.block.entity;

import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * FE handler of an Energy Conduit: energy pushed in is passed on to the network's machines at once.
 */
public class EnergyConduitBlockEntity extends NetworkPipeBlockEntity {
    private final EnergyHandler handler = new EnergyHandler() {
        @Override
        public long getAmountAsLong() {
            return 0;
        }

        @Override
        public long getCapacityAsLong() {
            return rate();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            return distribute(Capabilities.Energy.BLOCK, this, Math.min(amount, rate()),
                    (receiver, left) -> receiver.insert(left, transaction));
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    };

    public EnergyConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CONDUIT.get(), pos, state);
    }

    /** Pulls up to the tier's rate out of the block on {@code side} (a battery, any mod's storage) into the network. */
    @Override
    public void pull(ServerLevel level, BlockPos pos, Direction side) {
        EnergyHandler source = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(side), side.getOpposite());
        if (source != null) {
            EnergyHandlerUtil.move(source, handler, rate(), null);
        }
    }

    public EnergyHandler getEnergyHandler() {
        return handler;
    }
}
