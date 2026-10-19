package com.cyberfarm.block.entity;

import java.util.List;

import com.cyberfarm.block.NetworkPipeBlock;
import com.cyberfarm.energy.ConduitNetwork;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Holds the cached list of machines its network reaches, and the FE handler other blocks push
 * energy into. Energy is passed on immediately, taking turns between machines so none starves.
 */
public class EnergyConduitBlockEntity extends BlockEntity implements NetworkPipeBlock.Puller {
    private List<ConduitNetwork.Endpoint> endpoints = List.of();
    private int cachedVersion = -1;
    private int nextEndpoint;

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
            return distribute(Math.min(amount, rate()), transaction);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    };

    public EnergyConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CONDUIT.get(), pos, state);
    }

    /** FE that can enter the network per insertion, i.e. per pushing machine per tick: set by the tier. */
    private int rate() {
        return getBlockState().getBlock() instanceof NetworkPipeBlock pipe ? pipe.rate() : 0;
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

    private List<ConduitNetwork.Endpoint> endpoints() {
        if (cachedVersion != ConduitNetwork.version()) {
            endpoints = ConduitNetwork.findEndpoints(level, worldPosition);
            cachedVersion = ConduitNetwork.version();
        }
        return endpoints;
    }

    private int distribute(int amount, TransactionContext transaction) {
        if (level == null || amount <= 0) {
            return 0;
        }
        List<ConduitNetwork.Endpoint> targets = endpoints();
        int count = targets.size();
        if (count == 0) {
            return 0;
        }
        int start = Math.floorMod(nextEndpoint++, count);
        int moved = 0;
        for (int i = 0; i < count && moved < amount; i++) {
            ConduitNetwork.Endpoint target = targets.get((start + i) % count);
            EnergyHandler receiver = target.source() ? null : level.getCapability(Capabilities.Energy.BLOCK, target.pos(), target.side());
            if (receiver != null && receiver != handler) {
                moved += receiver.insert(amount - moved, transaction);
            }
        }
        return moved;
    }
}
