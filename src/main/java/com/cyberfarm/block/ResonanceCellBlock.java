package com.cyberfarm.block;

import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A piece of a Resonance Core's shell. A complete cube of them around the crystal grows the core
 * by a tier; once formed they vanish inside the bigger crystal. Right-clicking any of them opens the core.
 */
public class ResonanceCellBlock extends Block {
    public static final MapCodec<ResonanceCellBlock> CODEC = simpleCodec(ResonanceCellBlock::new);
    public static final BooleanProperty FORMED = CyberFrameBlock.FORMED;

    public ResonanceCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<ResonanceCellBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    /** Any cell opens the core it is built around, so the shell's progress can be checked while building. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return ResonanceCoreBlockEntity.openFromShell(level, pos, player);
    }
}
