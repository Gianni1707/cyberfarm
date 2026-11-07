package com.cyberfarm.block;

import com.cyberfarm.block.entity.FabricatorBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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

import java.util.List;

/** A small multiblock, a roller line: with a Cyber Frame behind it the line grows a block back, to a card magazine, and the card runs under the rollers and the acid drum (see tools/machinemodels.py). */
public class FabricatorBlock extends SmallMultiblockBlock {
    public static final MapCodec<FabricatorBlock> CODEC = simpleCodec(FabricatorBlock::new);

    // The low body, then the bearing blocks, the roller and the drum (whichever way it faces)
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 10, 15), Block.box(1.5, 10, 1.5, 14.5, 15.5, 14.5));

    public FabricatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FabricatorBlock> codec() {
        return CODEC;
    }

    @Override
    public List<Direction> frameSides(BlockState state) {
        return List.of(state.getValue(FACING).getOpposite());
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.fabricator.needs_frame";
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Nothing extra: FabricatorRenderer animates the print run. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {}

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FabricatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.FABRICATOR.get());
    }
}
