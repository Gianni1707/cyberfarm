package com.cyberfarm.block;


import com.cyberfarm.block.entity.BioDigesterBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** A small multiblock: with a Cyber Frame above it and one on its left, as the player sees its front, it assembles into a tall drum and a tank (BioDigesterRenderer draws what moves). */
public class BioDigesterBlock extends SmallMultiblockBlock {

    public BioDigesterBlock(Properties properties) {
        super(properties);
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BioDigesterBlockEntity(pos, state);
    }

    @Override
    public List<Direction> frameSides(BlockState state) {
        return List.of(state.getValue(FACING).getClockWise());
    }

    @Override
    public List<BlockPos> frameCells(BlockState state, BlockPos pos) {
        return List.of(pos.above(), pos.relative(state.getValue(FACING).getClockWise()));
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.bio_digester.needs_frames";
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.BIO_DIGESTER.get());
    }

    /** Bubbles popping on top while it digests. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            double across = (random.nextDouble() - 0.5) * 0.3;
            double along = (random.nextDouble() - 0.5) * 0.3;
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5 + across, pos.getY() + 31.8 / 16.0, pos.getZ() + 0.5 + along, 0.0, 0.02, 0.0);
        }
    }
}
