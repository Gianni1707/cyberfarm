package com.cyberfarm.block;

import com.cyberfarm.block.entity.SteamEngineBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/** The Steam Engine's boiler: the controller of the row; its cylinders and dynamo go behind it. */
public class SteamEngineBlock extends MachineBlock {
    public static final MapCodec<SteamEngineBlock> CODEC = simpleCodec(SteamEngineBlock::new);

    /** The row behind is complete: the block hides and the renderer draws the whole engine. */
    public static final BooleanProperty FORMED = MethaneExtractorBlock.FORMED;

    public SteamEngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
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
