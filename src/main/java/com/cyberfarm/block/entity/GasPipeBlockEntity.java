package com.cyberfarm.block.entity;

import com.cyberfarm.block.GasPipeBlock;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Fluid handler of a Gas or Liquid Pipe: whatever is pushed in goes straight to the tanks and
 * machines on the network. Works with any mod's fluid blocks.
 */
public class GasPipeBlockEntity extends NetworkPipeBlockEntity {
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
            return carries(resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || !carries(resource)) {
                return 0;
            }
            return distribute(Capabilities.Fluid.BLOCK, this, Math.min(amount, rate()),
                    (receiver, left) -> receiver.insert(resource, left, transaction));
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };

    public GasPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_PIPE.get(), pos, state);
    }

    private boolean carries(FluidResource resource) {
        return getBlockState().getBlock() instanceof GasPipeBlock pipe && pipe.carries(resource);
    }

    /** Pulls up to the tier's rate out of the tank or machine on {@code side} into the network. */
    @Override
    public void pull(ServerLevel level, BlockPos pos, Direction side) {
        ResourceHandler<FluidResource> source = level.getCapability(Capabilities.Fluid.BLOCK, pos.relative(side), side.getOpposite());
        if (source != null) {
            ResourceHandlerUtil.move(source, handler, this::carries, rate(), null);
        }
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return handler;
    }
}
