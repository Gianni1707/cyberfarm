package com.cyberfarm.block;

import com.cyberfarm.block.entity.TurbineControllerBlockEntity;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;

/** Steam Turbine controller: FORMED when its hall is complete (SIZE says which), LIT while the rotor turns. */
public class TurbineControllerBlock extends MachineBlock {
    public static final MapCodec<TurbineControllerBlock> CODEC = simpleCodec(TurbineControllerBlock::new);
    public static final BooleanProperty FORMED = CyberFrameBlock.FORMED;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final IntegerProperty SIZE = ReactorControllerBlock.SIZE;

    public TurbineControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false).setValue(LIT, false).setValue(SIZE, 0));
    }

    @Override
    protected MapCodec<TurbineControllerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED, LIT, SIZE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            // The rotor's angle is animated on the client from the synced speed
            return createTickerHelper(type, ModBlockEntities.TURBINE_CONTROLLER.get(), (lvl, pos, st, be) -> be.clientTick());
        }
        return machineTicker(level, type, ModBlockEntities.TURBINE_CONTROLLER.get());
    }
}
