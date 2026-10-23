package com.cyberfarm.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;
import com.cyberfarm.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The multiblocks the Multiblock Projector can show, each laid out from its controller's position
 * and facing, with the same rules the controllers check.
 */
public enum Blueprint {
    REACTOR_SMALL("reactor", TowerSize.SMALL, null),
    REACTOR_MEDIUM("reactor", TowerSize.MEDIUM, null),
    REACTOR_LARGE("reactor", TowerSize.LARGE, null),
    TURBINE_SMALL("turbine", null, TurbineSize.SMALL),
    TURBINE_MEDIUM("turbine", null, TurbineSize.MEDIUM),
    TURBINE_LARGE("turbine", null, TurbineSize.LARGE),
    METHANE_CHAMBER("methane_chamber", null, null),
    RESONANCE_CORE_TIER_2("resonance_core", 1),
    RESONANCE_CORE_TIER_3("resonance_core", 2),
    RESONANCE_CORE_TIER_4("resonance_core", 3);

    /**
     * One block of the layout: {@code show} is what to place there (null: leave it empty) and
     * {@code ok} whether what is there now is right.
     */
    public record Ghost(BlockPos pos, @Nullable Block show, Predicate<BlockState> ok) {}

    private final String name;
    private final @Nullable TowerSize tower;
    private final @Nullable TurbineSize turbine;
    /** Shell half-size of a Resonance Core tier, 0 for the other structures. */
    private final int coreRadius;

    Blueprint(String name, @Nullable TowerSize tower, @Nullable TurbineSize turbine) {
        this.name = name;
        this.tower = tower;
        this.turbine = turbine;
        this.coreRadius = 0;
    }

    Blueprint(String name, int coreRadius) {
        this.name = name;
        this.tower = null;
        this.turbine = null;
        this.coreRadius = coreRadius;
    }

    /** Translation key of the name shown to the player, with the size when there is one. */
    public String key() {
        return "projector.cyberfarm." + name;
    }

    public @Nullable String sizeKey() {
        if (coreRadius > 0) {
            return "projector.cyberfarm.size.tier_" + (coreRadius + 1);
        }
        String size = tower != null ? tower.name() : turbine != null ? turbine.name() : null;
        return size != null ? "gui.cyberfarm.reactor.size." + size.toLowerCase() : null;
    }

    public Blueprint next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Blueprint byOrdinal(int ordinal) {
        return values()[Math.floorMod(ordinal, values().length)];
    }

    public List<Ghost> ghosts(BlockPos controller, Direction facing) {
        if (tower != null) {
            return fromCells(tower.cells(controller, facing), controller, ModBlocks.REACTOR_CONTROLLER.get(),
                    ModBlocks.REACTOR_CASING.get(), ModBlocks.REACTOR_PORT.get());
        }
        if (turbine != null) {
            return fromCells(turbine.cells(controller, facing), controller, ModBlocks.TURBINE_CONTROLLER.get(),
                    ModBlocks.TURBINE_CASING.get(), ModBlocks.TURBINE_PORT.get());
        }
        return coreRadius > 0 ? resonanceCore(controller, coreRadius) : methaneChamber(controller);
    }

    /**
     * A Resonance Core tier: the shell cube over the pedestal, empty inside, and the pylon columns
     * it needs at their closest, east and west first. The core checks itself against the same rules.
     */
    private static List<Ghost> resonanceCore(BlockPos core, int radius) {
        Block cell = ModBlocks.RESONANCE_CELL.get();
        Block pylon = ModBlocks.STABILIZER_PYLON.get();
        List<Ghost> ghosts = new ArrayList<>();
        ghosts.add(new Ghost(core, ModBlocks.RESONANCE_CORE.get(), state -> state.is(ModBlocks.RESONANCE_CORE.get())));
        BlockPos centre = core.above(1 + radius);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            boolean surface = Math.max(Math.abs(pos.getX() - centre.getX()), Math.max(Math.abs(pos.getY() - centre.getY()),
                    Math.abs(pos.getZ() - centre.getZ()))) == radius;
            ghosts.add(surface ? frame(pos.immutable(), cell) : new Ghost(pos.immutable(), null, BlockState::isAir));
        }
        int tier = radius + 1;
        Direction[] sides = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
        for (int i = 0; i < ResonanceCoreBlockEntity.requiredPylons(tier); i++) {
            BlockPos ground = core.relative(sides[i], ResonanceCoreBlockEntity.minDistance(tier));
            for (int y = 0; y < ResonanceCoreBlockEntity.requiredHeight(tier); y++) {
                ghosts.add(frame(ground.above(y), pylon));
            }
        }
        return ghosts;
    }

    private static List<Ghost> fromCells(List<Multiblock.Cell> cells, BlockPos controller, Block controllerBlock, Block casing, Block port) {
        List<Ghost> ghosts = new ArrayList<>(cells.size());
        for (Multiblock.Cell cell : cells) {
            if (cell.pos().equals(controller)) {
                ghosts.add(new Ghost(cell.pos(), controllerBlock, state -> state.is(controllerBlock)));
            } else if (cell.part() == Multiblock.Part.SHELL) {
                ghosts.add(new Ghost(cell.pos(), casing, state -> state.is(casing) || state.is(port)));
            } else if (cell.part() == Multiblock.Part.EMPTY) {
                ghosts.add(new Ghost(cell.pos(), null, BlockState::isAir));
            }
        }
        return ghosts;
    }

    /**
     * The 3x3x4 chamber under a Methane Extractor; the extractor checks itself against it too. Any
     * Cyber Frame may be an Acid Inlet or a Methane Outlet instead, any glass of the lower wall an
     * Intake Gate: the projector suggests one of each.
     */
    private static List<Ghost> methaneChamber(BlockPos top) {
        Block frame = ModBlocks.CYBER_FRAME.get();
        Block glass = ModBlocks.CONTAINMENT_GLASS.get();
        Block extractor = ModBlocks.METHANE_EXTRACTOR.get();
        Predicate<BlockState> shell = state -> state.is(frame) || state.is(ModBlocks.ACID_INLET.get()) || state.is(ModBlocks.METHANE_OUTLET.get());
        Predicate<BlockState> lowerWall = state -> state.is(glass) || state.is(ModBlocks.INTAKE_GATE.get());
        List<Ghost> ghosts = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean centre = dx == 0 && dz == 0;
                boolean corner = dx != 0 && dz != 0;
                Block roof = dx == -1 && dz == 0 ? ModBlocks.ACID_INLET.get() : dx == 1 && dz == 0 ? ModBlocks.METHANE_OUTLET.get() : frame;
                ghosts.add(centre ? new Ghost(top, extractor, state -> state.is(extractor)) : new Ghost(top.offset(dx, 0, dz), roof, shell));
                for (int dy = 1; dy <= 2; dy++) {
                    BlockPos pos = top.offset(dx, -dy, dz);
                    Block gate = dy == 2 && dx == 0 && dz == -1 ? ModBlocks.INTAKE_GATE.get() : glass;
                    ghosts.add(centre ? new Ghost(pos, null, BlockState::isAir)
                            : corner ? new Ghost(pos, frame, shell)
                            : dy == 2 ? new Ghost(pos, gate, lowerWall)
                            : frame(pos, glass));
                }
                ghosts.add(new Ghost(top.offset(dx, -3, dz), frame, shell));
            }
        }
        return ghosts;
    }

    private static Ghost frame(BlockPos pos, Block frame) {
        return new Ghost(pos, frame, state -> state.is(frame));
    }
}
