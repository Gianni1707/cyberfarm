package com.cyberfarm.block;

import java.util.List;

import com.cyberfarm.block.entity.TrinketTableBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Trinket Table: where trinkets and chip programs are made, and programs installed in the chip. A 2x2 workstation
 * standing upright: the table is its bottom cell, with Cyber Frames beside it (on its right, seen from the front), above
 * it and above that one; then it opens and its two arms go to work.
 */
public class TrinketTableBlock extends SmallMultiblockBlock {
    public TrinketTableBlock(Properties properties) {
        super(properties);
    }

    /** Seen from the front, its right side: where the other column of the 2x2 stands. */
    public static Direction beside(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    @Override
    public List<Direction> frameSides(BlockState state) {
        return List.of(beside(state));
    }

    /** Beside it, above it, and above the one beside: the other three cells of the 2x2. */
    @Override
    public List<BlockPos> frameCells(BlockState state, BlockPos pos) {
        BlockPos side = pos.relative(beside(state));
        return List.of(side, pos.above(), side.above());
    }

    /** The bench in the cell beside it; above, only the gantry's beam: the arms' space stays open, to see and reach into. */
    private static final VoxelShape BENCH = Block.box(0, 0, 0, 16, 11, 16), BEAM = Block.box(0, 13.5, 0, 16, 16, 16);

    @Override
    public VoxelShape frameShape(BlockState state, BlockPos machine, BlockPos frame) {
        return frame.getY() > machine.getY() ? BEAM : BENCH;
    }

    /** It takes no power and nothing from pipes: a workbench. */
    @Override
    protected boolean hasPipePorts() {
        return false;
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.trinket_table.needs_frames";
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrinketTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.TRINKET_TABLE.get());
    }
}
