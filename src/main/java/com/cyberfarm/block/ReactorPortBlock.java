package com.cyberfarm.block;

import com.cyberfarm.block.entity.ReactorPortBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Shell block of the Fission Reactor or Steam Turbine that pipes, cables and hoppers can connect to. */
public class ReactorPortBlock extends BaseEntityBlock {
    public static final MapCodec<ReactorPortBlock> CODEC = simpleCodec(ReactorPortBlock::new);
    public static final BooleanProperty FORMED = CyberFrameBlock.FORMED;

    public ReactorPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<ReactorPortBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return state.getValue(FORMED) ? ReactorCasingBlock.openController(level, pos, player) : InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorPortBlockEntity(pos, state);
    }
}
