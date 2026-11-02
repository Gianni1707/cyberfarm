package com.cyberfarm.port;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Base of CyberFarm blocks with a block entity on 1.20.1: runs the 26.x preRemoveSideEffects hook,
 * the 26.x click hooks (see {@link BlockUse}) and renders its model like 26.x does by default.
 */
public abstract class PortEntityBlock extends BaseEntityBlock {
    protected PortEntityBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PortBlockEntity be) {
            be.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = useItemOn(player.getItemInHand(hand), state, level, pos, player, hand, hit);
        return result == BlockUse.TRY_WITH_EMPTY_HAND ? useWithoutItem(state, level, pos, player, hit) : result;
    }

    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockUse.TRY_WITH_EMPTY_HAND;
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
