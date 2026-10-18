package com.cyberfarm.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;

public final class EnergyUtil {
    private EnergyUtil() {}

    /** Pushes FE into every adjacent block that accepts it, from any mod. */
    public static void pushToNeighbors(Level level, BlockPos pos, EnergyHandler source, int maxPerSide) {
        for (Direction direction : Direction.values()) {
            if (source.getAmountAsLong() <= 0) {
                return;
            }
            EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(direction), direction.getOpposite());
            if (target != null) {
                EnergyHandlerUtil.move(source, target, maxPerSide, null);
            }
        }
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
