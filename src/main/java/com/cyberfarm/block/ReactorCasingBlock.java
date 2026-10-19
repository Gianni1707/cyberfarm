package com.cyberfarm.block;

import com.cyberfarm.block.entity.ReactorControllerBlockEntity;
import com.cyberfarm.block.entity.TurbineControllerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Shell of the Fission Reactor tower and of the Steam Turbine hall. Once formed it becomes invisible
 * inside the smooth body the controller draws, and opens the controller GUI when right-clicked.
 */
public class ReactorCasingBlock extends CyberFrameBlock {
    public ReactorCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return state.getValue(FORMED) ? openController(level, pos, player) : InteractionResult.PASS;
    }

    static InteractionResult openController(Level level, BlockPos pos, Player player) {
        InteractionResult result = ReactorControllerBlockEntity.openFromShell(level, pos, player);
        return result != InteractionResult.PASS ? result : TurbineControllerBlockEntity.openFromShell(level, pos, player);
    }
}
