package com.cyberfarm.block.entity;

import java.util.List;

import com.cyberfarm.block.NetworkPipeBlock;
import com.cyberfarm.energy.ConduitNetwork;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Fluid handler of a Gas Pipe: whatever is pushed in goes straight to the tanks and machines on
 * the network, taking turns between them. Works with any fluid and any mod's fluid blocks.
 */
public class GasPipeBlockEntity extends BlockEntity {
    private List<ConduitNetwork.Endpoint> endpoints = List.of();
    private int cachedVersion = -1;
    private int nextEndpoint;

    private final ResourceHandler<FluidResource> handler = new ResourceHandler<>() {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            return FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return rate();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return true;
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return distribute(resource, Math.min(amount, rate()), transaction);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };

    public GasPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_PIPE.get(), pos, state);
    }

    /** mB that can enter the network per insertion, i.e. per pushing machine per tick: set by the tier. */
    private int rate() {
        return getBlockState().getBlock() instanceof NetworkPipeBlock pipe ? pipe.rate() : 0;
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return handler;
    }

    private List<ConduitNetwork.Endpoint> endpoints() {
        if (cachedVersion != ConduitNetwork.version()) {
            endpoints = ConduitNetwork.findEndpoints(level, worldPosition);
            cachedVersion = ConduitNetwork.version();
        }
        return endpoints;
    }

    private int distribute(FluidResource resource, int amount, TransactionContext transaction) {
        if (level == null || amount <= 0 || resource.isEmpty()) {
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
            ResourceHandler<FluidResource> receiver = level.getCapability(Capabilities.Fluid.BLOCK, target.pos(), target.side());
            if (receiver != null && receiver != handler) {
                moved += receiver.insert(resource, amount - moved, transaction);
            }
        }
        return moved;
    }
}
