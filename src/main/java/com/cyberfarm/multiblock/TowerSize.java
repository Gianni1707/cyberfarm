package com.cyberfarm.multiblock;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

/**
 * The three Fission Reactor towers. Each layer of a tower is a square ring of shell blocks,
 * {@code rings[y]} blocks from the axis (layer 0 is the full floor); inside a ring it is empty,
 * outside it, up to the base, anything goes. The controller sits in the middle of one side of layer 1. Ports go only on the
 * middle of the four sides: towards the corners the tower is drawn with diagonal faces, which pipes coming along a block's
 * axis cannot meet.
 */
public enum TowerSize implements Multiblock.Shape {
    SMALL(new int[] {2, 2, 2, 1, 1, 1, 2}, 2, 200, 150, 1.0F),
    MEDIUM(new int[] {3, 3, 3, 3, 2, 2, 2, 2, 2, 3, 3}, 4, 350, 250, 1.3F),
    LARGE(new int[] {4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4}, 8, 500, 400, 1.6F);

    private final int[] rings;
    private final int rods;
    private final int maxBoost;
    private final int baseTransfer;
    private final float explosionScale;

    TowerSize(int[] rings, int rods, int maxBoost, int baseTransfer, float explosionScale) {
        this.rings = rings;
        this.rods = rods;
        this.maxBoost = maxBoost;
        this.baseTransfer = baseTransfer;
        this.explosionScale = explosionScale;
    }

    /** Half the width of the base: 2, 3 or 4 blocks from the axis. */
    public int base() {
        return rings[0];
    }

    public int height() {
        return rings.length;
    }

    /** Fuel rods that burn: the reactor holds up to 8, a smaller tower only uses the first ones. */
    public int rods() {
        return rods;
    }

    public int maxBoost() {
        return maxBoost;
    }

    /** Heat moved per tick with plain water; coolants multiply it. */
    public int baseTransfer() {
        return baseTransfer;
    }

    public float explosionScale() {
        return explosionScale;
    }

    public BlockPos floorCentre(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite(), base()).below();
    }

    @Override
    public List<Multiblock.Cell> cells(BlockPos controller, Direction facing) {
        BlockPos floor = floorCentre(controller, facing);
        int base = base();
        List<Multiblock.Cell> cells = new ArrayList<>((2 * base + 1) * (2 * base + 1) * height());
        for (int y = 0; y < height(); y++) {
            for (int dx = -base; dx <= base; dx++) {
                for (int dz = -base; dz <= base; dz++) {
                    int ring = Math.max(Math.abs(dx), Math.abs(dz));
                    Multiblock.Part part = ring == rings[y] && !Multiblock.onFlatSide(dx, dz) ? Multiblock.Part.CASING
                            : y == 0 || ring == rings[y] ? Multiblock.Part.SHELL
                            : ring < rings[y] ? Multiblock.Part.EMPTY : Multiblock.Part.ANY;
                    cells.add(new Multiblock.Cell(floor.offset(dx, y, dz), part));
                }
            }
        }
        return cells;
    }

    /**
     * The side a pipe reaches a port at (dx, dy, dz) from the floor centre from: away from the tower's axis, along the main
     * horizontal axis of its offset. Null for ports in the middle of the floor, which are reached from below.
     */
    public static @Nullable Direction outward(int dx, int dy, int dz) {
        if (dx == 0 && dz == 0 || dy == 0 && Math.max(Math.abs(dx), Math.abs(dz)) < 2) {
            return null;
        }
        return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    /**
     * How far the drawn tower stands out of the outer face of a port at (dx, dy, dz) from the floor centre, so that a pipe in
     * front of it would be inside the drawing; 0 where it does not.
     */
    public float buried(int dx, int dy, int dz) {
        if (dy < 0 || dy >= height() || outward(dx, dy, dz) == null || Math.max(Math.abs(dx), Math.abs(dz)) != rings[dy]) {
            return 0.0F;
        }
        float depth = (radius(dy) + radius(dy + 1.0F)) / 2.0F - (rings[dy] + 0.5F);
        return depth > 0.02F ? depth : 0.0F;
    }

    /** How far layer {@code y}'s ring of shell blocks is from the axis, in blocks. */
    public int ring(int y) {
        return rings[y];
    }

    // Rendering: the smooth tower drawn over the shell follows a hyperbola fitted to the rings

    /** Radius of the drawn tower at a height: wide enough at the base to cover the floor's corners. */
    public float radius(float height) {
        float waistRadius = rings[height() / 2] + 0.6F;
        float waistHeight = height() * 0.66F;
        float baseRadius = base() + 1.4F;
        float flare = (float) Math.sqrt(Math.pow(baseRadius / waistRadius, 2) - 1);
        float curve = waistHeight / flare;
        float fromWaist = (height - waistHeight) / curve;
        return waistRadius * (float) Math.sqrt(1.0F + fromWaist * fromWaist);
    }

    public static TowerSize byOrdinal(int ordinal) {
        return values()[Mth.clamp(ordinal, 0, values().length - 1)];
    }
}
