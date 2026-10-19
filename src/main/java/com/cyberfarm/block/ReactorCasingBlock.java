package com.cyberfarm.block;

import com.cyberfarm.block.entity.ReactorControllerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Shell of the Fission Reactor tower; lights up when formed, and opens the reactor GUI when right-clicked. */
public class ReactorCasingBlock extends CyberFrameBlock {
    public ReactorCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return state.getValue(FORMED) ? ReactorControllerBlockEntity.openFromShell(level, pos, player) : InteractionResult.PASS;
    }
}
