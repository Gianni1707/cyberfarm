package com.cyberfarm.block;

import com.cyberfarm.block.entity.AcidSynthesizerBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

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

/** A contact tower between two tanks (the model, see tools/machinemodels.py; AcidSynthesizerRenderer draws the rest). */
public class AcidSynthesizerBlock extends LitMachineBlock {
    public static final MapCodec<AcidSynthesizerBlock> CODEC = simpleCodec(AcidSynthesizerBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.5, 0, 0.5, 15.5, 15, 15.5);

    public AcidSynthesizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AcidSynthesizerBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasPipePorts() {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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
            level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5 - front.getStepX() * 0.125, pos.getY() + 16.2 / 16.0,
                    pos.getZ() + 0.5 - front.getStepZ() * 0.125, 0.0, 0.03, 0.0);
        }
    }
}
