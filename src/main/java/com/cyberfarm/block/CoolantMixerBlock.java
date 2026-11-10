package com.cyberfarm.block;


import com.cyberfarm.block.entity.CoolantMixerBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A small multiblock: with a Cyber Frame on its right, as the player sees its front, it assembles into a 2x1 bench that
 * opens and shows both tanks (see tools/machinemodels.py; CoolantMixerRenderer draws what moves).
 */
public class CoolantMixerBlock extends SmallMultiblockBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 1, 16), Block.box(0.5, 1, 0.5, 15.5, 16, 15.5));

    public CoolantMixerBlock(Properties properties) {
        super(properties);
    }


    /** The player faces its front, so their right is the machine's counter-clockwise. */
    public static Direction frameSide(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    @Override
    public List<Direction> frameSides(BlockState state) {
        return List.of(frameSide(state));
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.coolant_mixer.needs_frame";
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoolantMixerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.COOLANT_MIXER.get());
    }

    /** Frost drifting off the coolant tank's cap, over the frame's cell, while it mixes. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            Direction side = frameSide(state);
            Direction back = state.getValue(FACING).getOpposite();
            double across = (random.nextDouble() - 0.5) * 6.0 / 16.0;
            double out = 17.5 / 16.0;
            double x = pos.getX() + 0.5 + side.getStepX() * (out + across) + back.getStepX() / 16.0;
            double z = pos.getZ() + 0.5 + side.getStepZ() * (out + across) + back.getStepZ() / 16.0;
            level.addParticle(ParticleTypes.SNOWFLAKE, x, pos.getY() + 1.02, z, 0.0, 0.01, 0.0);
        }
    }
}
