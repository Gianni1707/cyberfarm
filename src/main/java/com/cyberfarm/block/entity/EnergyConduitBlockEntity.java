package com.cyberfarm.block.entity;

import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.port.Caps;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * FE handler of an Energy Conduit: energy pushed in is passed on to the network's machines at once.
 */
public class EnergyConduitBlockEntity extends NetworkPipeBlockEntity {
    private final IEnergyStorage handler = new IEnergyStorage() {
        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return rate();
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int moved = distribute(Caps.ENERGY, this, Math.min(amount, rate()),
                    (receiver, left) -> receiver.receiveEnergy(left, simulate));
            if (moved > 0 && !simulate) {
                flowed(ENERGY);
            }
            return moved;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return 0;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        @Override
        public boolean canExtract() {
            return false;
        }
    };

    public EnergyConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CONDUIT.get(), pos, state);
    }

    /** Pulls up to the tier's rate out of the block on {@code side} (a battery, any mod's storage) into the network. */
    @Override
    public void pull(ServerLevel level, BlockPos pos, Direction side) {
        IEnergyStorage source = Caps.energy(level, pos.relative(side), side.getOpposite());
        if (source != null) {
            EnergyUtil.move(source, handler, rate());
        }
    }

    public IEnergyStorage getEnergyHandler() {
        return handler;
    }
}
