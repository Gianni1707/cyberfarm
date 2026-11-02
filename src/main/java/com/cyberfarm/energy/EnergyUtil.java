package com.cyberfarm.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.energy.IEnergyStorage;

import com.cyberfarm.port.Caps;

public final class EnergyUtil {
    private EnergyUtil() {}

    /** Pushes FE into every adjacent block that accepts it, from any mod. */
    public static void pushToNeighbors(Level level, BlockPos pos, IEnergyStorage source, int maxPerSide) {
        for (Direction direction : Direction.values()) {
            if (source.getEnergyStored() <= 0) {
                return;
            }
            IEnergyStorage target = Caps.energy(level, pos.relative(direction), direction.getOpposite());
            if (target != null) {
                move(source, target, maxPerSide);
            }
        }
    }

    /** Moves up to {@code max} FE from {@code source} to {@code target}. Returns the amount moved. */
    public static int move(IEnergyStorage source, IEnergyStorage target, int max) {
        int offered = source.extractEnergy(max, true);
        int accepted = offered > 0 ? target.receiveEnergy(offered, true) : 0;
        return accepted > 0 ? target.receiveEnergy(source.extractEnergy(accepted, false), false) : 0;
    }

    // Container data slots are synced as shorts, so ints travel as two 16-bit halves.
    public static int low(int value) {
        return value & 0xFFFF;
    }

    public static int high(int value) {
        return value >>> 16;
    }

    public static int combine(int low, int high) {
        return (low & 0xFFFF) | ((high & 0xFFFF) << 16);
    }
}
