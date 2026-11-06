package com.cyberfarm.block;

import com.cyberfarm.block.entity.SteamEngineBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Steam Engine's boiler, a small multiblock: with four Cyber Frames in a row behind it, three cylinders and a dynamo unroll
 * into them (see tools/machinemodels.py; SteamEngineRenderer draws what moves).
 */
public class SteamEngineBlock extends SmallMultiblockBlock {
    public static final MapCodec<SteamEngineBlock> CODEC = simpleCodec(SteamEngineBlock::new);

    public SteamEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public List<Direction> frameSides(BlockState state) {
        return List.of(state.getValue(FACING).getOpposite());
    }

    @Override
    public int frameDepth() {
        return SteamEngineBlockEntity.CYLINDERS + 1;
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.steam_engine.needs_frames";
    }

    /** The pistons and flywheel show it works, and steam puffs from the boiler: no sparks. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    @Override
    protected MapCodec<SteamEngineBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamEngineBlockEntity(pos, state);
    }

    /** Ticks on the client too, to animate the pistons and the flywheel. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.STEAM_ENGINE.get(), (lvl, pos, st, engine) -> engine.clientTick())
                : machineTicker(level, type, ModBlockEntities.STEAM_ENGINE.get());
    }
}
