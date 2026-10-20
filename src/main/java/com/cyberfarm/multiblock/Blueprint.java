package com.cyberfarm.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.cyberfarm.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
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
    METHANE_CHAMBER("methane_chamber", null, null);

    /**
     * One block of the layout: {@code show} is what to place there (null: leave it empty) and
     * {@code ok} whether what is there now is right.
     */
    public record Ghost(BlockPos pos, @Nullable Block show, Predicate<BlockState> ok) {}

    private final String name;
    private final @Nullable TowerSize tower;
    private final @Nullable TurbineSize turbine;

    Blueprint(String name, @Nullable TowerSize tower, @Nullable TurbineSize turbine) {
        this.name = name;
        this.tower = tower;
        this.turbine = turbine;
    }

    /** Translation key of the name shown to the player, with the size when there is one. */
    public String key() {
        return "projector.cyberfarm." + name;
    }

    public @Nullable String sizeKey() {
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
        return methaneChamber(controller);
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

    /** The 3x3x4 chamber under a Methane Extractor; the extractor checks itself against it too. */
    private static List<Ghost> methaneChamber(BlockPos top) {
        Block frame = ModBlocks.CYBER_FRAME.get();
        Block extractor = ModBlocks.METHANE_EXTRACTOR.get();
        List<Ghost> ghosts = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean centre = dx == 0 && dz == 0;
                boolean corner = dx != 0 && dz != 0;
                ghosts.add(centre ? new Ghost(top, extractor, state -> state.is(extractor)) : frame(top.offset(dx, 0, dz), frame));
                for (int dy = 1; dy <= 2; dy++) {
                    BlockPos pos = top.offset(dx, -dy, dz);
                    ghosts.add(centre ? new Ghost(pos, null, BlockState::isAir)
                            : corner ? frame(pos, frame)
                            : new Ghost(pos, Blocks.GLASS, state -> state.is(Tags.Blocks.GLASS_BLOCKS)));
                }
                ghosts.add(frame(top.offset(dx, -3, dz), frame));
            }
        }
        return ghosts;
    }

    private static Ghost frame(BlockPos pos, Block frame) {
        return new Ghost(pos, frame, state -> state.is(frame));
    }
}
