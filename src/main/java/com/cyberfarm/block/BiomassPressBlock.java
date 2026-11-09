package com.cyberfarm.block;

import com.cyberfarm.block.entity.BiomassPressBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.List;

/** A small multiblock: with a Cyber Frame above it, it assembles into a press two blocks tall (see tools/machinemodels.py; BiomassPressRenderer draws what moves). */
public class BiomassPressBlock extends SmallMultiblockBlock {
    public static final MapCodec<BiomassPressBlock> CODEC = simpleCodec(BiomassPressBlock::new);

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public BiomassPressBlock(Properties properties) {
        super(properties);
    }

    /** The side its nozzle points to: the player faces its front, so their right is the machine's counter-clockwise. */
    public static Direction nozzle(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    @Override
    protected MapCodec<BiomassPressBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasPipePorts() {
        return false;
    }

    @Override
    public List<Direction> frameSides(BlockState state) {
        return List.of(Direction.UP);
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.biomass_press.needs_frame";
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BiomassPressBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.BIOMASS_PRESS.get());
    }
}
