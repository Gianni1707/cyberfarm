package com.cyberfarm.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.entity.ReactorPortBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Shared checks of CyberFarm's big multiblocks (the Fission Reactor tower and the Steam Turbine):
 * a layout is a list of cells, each one a world position that must be a shell block, must be empty,
 * or can be anything.
 */
public final class Multiblock {
    public enum Part { SHELL, EMPTY, ANY }

    public record Cell(BlockPos pos, Part part) {}

    private Multiblock() {}

    /**
     * The ports of a complete structure, or null when a cell is wrong. The controller's own cell is
     * skipped; {@code shell} tells the shell blocks apart and {@code port} the ones that are ports.
     */
    public static @Nullable List<BlockPos> scan(Level level, BlockPos controller, List<Cell> cells,
            Predicate<BlockState> shell, Predicate<BlockState> port) {
        List<BlockPos> ports = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.pos().equals(controller)) {
                continue;
            }
            BlockState state = level.getBlockState(cell.pos());
            switch (cell.part()) {
                case SHELL -> {
                    if (!shell.test(state)) {
                        return null;
                    }
                    if (port.test(state)) {
                        ports.add(cell.pos());
                    }
                }
                case EMPTY -> {
                    if (!state.isAir()) {
                        return null;
                    }
                }
                case ANY -> {}
            }
        }
        return ports;
    }

    public static boolean isShell(List<Cell> cells, BlockPos pos) {
        for (Cell cell : cells) {
            if (cell.part() == Part.SHELL && cell.pos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    /** Lights up (or turns off) every shell block with a FORMED property: casings and ports. */
    public static void setFormed(Level level, List<Cell> cells, boolean formed) {
        for (Cell cell : cells) {
            if (cell.part() != Part.SHELL) {
                continue;
            }
            BlockState state = level.getBlockState(cell.pos());
            if (state.hasProperty(CyberFrameBlock.FORMED) && state.getValue(CyberFrameBlock.FORMED) != formed) {
                // Full update for ports, so pipes and cables next to them connect or disconnect
                boolean port = level.getBlockEntity(cell.pos()) instanceof ReactorPortBlockEntity;
                level.setBlock(cell.pos(), state.setValue(CyberFrameBlock.FORMED, formed), port ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Tells the old ports they are free and the new ones which controller they belong to. */
    public static List<BlockPos> rebindPorts(Level level, BlockPos controller, List<BlockPos> oldPorts, List<BlockPos> newPorts) {
        for (BlockPos port : oldPorts) {
            if (!newPorts.contains(port) && level.getBlockEntity(port) instanceof ReactorPortBlockEntity entity) {
                entity.setController(null);
            }
        }
        for (BlockPos port : newPorts) {
            if (level.getBlockEntity(port) instanceof ReactorPortBlockEntity entity) {
                entity.setController(controller);
            }
        }
        return newPorts;
    }
}
