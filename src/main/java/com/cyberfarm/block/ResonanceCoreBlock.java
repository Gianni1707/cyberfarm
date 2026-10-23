package com.cyberfarm.block;

import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The pedestal of the energy store; its crystal floats above it, drawn by the renderer. */
public class ResonanceCoreBlock extends MachineBlock {
    public static final MapCodec<ResonanceCoreBlock> CODEC = simpleCodec(ResonanceCoreBlock::new);
    // Base plate plus the emitter ring the crystal floats over
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 6, 16),
            Block.box(3, 6, 3, 13, 8, 13));

    public ResonanceCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ResonanceCoreBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResonanceCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.RESONANCE_CORE.get());
    }
}
