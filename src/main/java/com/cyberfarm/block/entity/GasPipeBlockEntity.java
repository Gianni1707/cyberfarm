package com.cyberfarm.block.entity;

import com.cyberfarm.block.GasPipeBlock;
import com.cyberfarm.port.Caps;
import com.cyberfarm.port.FluidResource;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Fluid handler of a Gas or Liquid Pipe: whatever is pushed in goes straight to the tanks and
 * machines on the network. Works with any mod's fluid blocks.
 */
public class GasPipeBlockEntity extends NetworkPipeBlockEntity {
    private final IFluidHandler handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return rate();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return carries(FluidResource.of(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !carries(FluidResource.of(resource))) {
                return 0;
            }
            return distribute(Caps.FLUID, this, Math.min(resource.getAmount(), rate()), (receiver, left) -> {
                FluidStack offer = resource.copy();
                offer.setAmount(left);
                return receiver.fill(offer, action);
            });
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
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
        IFluidHandler source = Caps.fluids(level, pos.relative(side), side.getOpposite());
        if (source != null) {
            FluidUtil.tryFluidTransfer(handler, source, rate(), true);
        }
    }

    public IFluidHandler getFluidHandler() {
        return handler;
    }
}
