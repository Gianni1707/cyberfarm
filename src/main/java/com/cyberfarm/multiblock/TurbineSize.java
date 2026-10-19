package com.cyberfarm.multiblock;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The three Steam Turbines: horizontal halls with a square cross-section, the controller in the
 * middle of the front cap. Both caps are full; along the length only the outer ring is shell and
 * the inside, where the rotor spins, must be empty. Bigger turbines take more steam and get more FE
 * out of every mB.
 */
public enum TurbineSize {
    SMALL(1, 4, 200, 4, 100),
    MEDIUM(2, 6, 600, 5, 140),
    LARGE(3, 8, 1_600, 6, 200);

    private final int half;
    private final int length;
    private final int maxSteam;
    private final int fePerMb;
    private final int spinUpTicks;

    TurbineSize(int half, int length, int maxSteam, int fePerMb, int spinUpTicks) {
        this.half = half;
        this.length = length;
        this.maxSteam = maxSteam;
        this.fePerMb = fePerMb;
        this.spinUpTicks = spinUpTicks;
    }

    /** Blocks from the axis to the outer ring: 1, 2 or 3 (a 3x3, 5x5 or 7x7 cross-section). */
    public int half() {
        return half;
    }

    public int length() {
        return length;
    }

    /** Steam used per tick at full speed. */
    public int maxSteam() {
        return maxSteam;
    }

    public int fePerMb() {
        return fePerMb;
    }

    public int maxOutput() {
        return maxSteam * fePerMb;
    }

    /** Ticks the rotor takes from standstill to full speed (and back). */
    public int spinUpTicks() {
        return spinUpTicks;
    }

    /** Energy buffer: two seconds of full output. */
    public int energyCapacity() {
        return maxOutput() * 40;
    }

    /** A cell {@code along} blocks behind the controller, {@code right} to its right and {@code up} above it. */
    public static BlockPos at(BlockPos controller, Direction facing, int along, int right, int up) {
        return controller.relative(facing.getOpposite(), along).relative(facing.getClockWise(), right).above(up);
    }

    public List<Multiblock.Cell> cells(BlockPos controller, Direction facing) {
        List<Multiblock.Cell> cells = new ArrayList<>((2 * half + 1) * (2 * half + 1) * length);
        for (int along = 0; along < length; along++) {
            boolean cap = along == 0 || along == length - 1;
            for (int right = -half; right <= half; right++) {
                for (int up = -half; up <= half; up++) {
                    boolean ring = Math.max(Math.abs(right), Math.abs(up)) == half;
                    Multiblock.Part part = cap || ring ? Multiblock.Part.SHELL : Multiblock.Part.EMPTY;
                    cells.add(new Multiblock.Cell(at(controller, facing, along, right, up), part));
                }
            }
        }
        return cells;
    }

    public static TurbineSize byOrdinal(int ordinal) {
        return values()[Math.clamp(ordinal, 0, values().length - 1)];
    }
}
