package com.cyberfarm.block;

import com.cyberfarm.port.PortParticles;


import com.cyberfarm.block.entity.AcidSynthesizerBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A small multiblock: with Cyber Frames on both sides and three on top it assembles into a 3x1x2 contact tower between two tall
 * tanks, which rise out of its base (see tools/machinemodels.py; AcidSynthesizerRenderer draws what moves).
 */
public class AcidSynthesizerBlock extends SmallMultiblockBlock {
    private static final VoxelShape SHAPE = Block.box(0.5, 0, 0.5, 15.5, 15, 15.5);

    public AcidSynthesizerBlock(Properties properties) {
        super(properties);
    }


    @Override
    public List<Direction> frameSides(BlockState state) {
        Direction front = state.getValue(FACING);
        return List.of(front.getClockWise(), front.getCounterClockWise());
    }

    /** The row beside it, then the row above. */
    @Override
    public List<BlockPos> frameCells(BlockState state, BlockPos pos) {
        List<BlockPos> cells = new ArrayList<>(super.frameCells(state, pos));
        for (BlockPos beside : List.copyOf(cells)) {
            cells.add(beside.above());
        }
        cells.add(pos.above());
        return cells;
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.acid_synthesizer.needs_frames";
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AcidSynthesizerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.ACID_SYNTHESIZER.get());
    }

    /** White puffs from the chimney while it works, on the tower's axis, 2 px behind the middle of the block. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            Direction front = state.getValue(FACING);
            level.addParticle(PortParticles.WHITE_SMOKE, pos.getX() + 0.5 - front.getStepX() * 0.125, pos.getY() + 32.2 / 16.0,
                    pos.getZ() + 0.5 - front.getStepZ() * 0.125, 0.0, 0.03, 0.0);
        }
    }
}
