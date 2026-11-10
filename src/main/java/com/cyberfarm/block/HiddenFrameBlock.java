package com.cyberfarm.block;

import com.cyberfarm.port.PortBlock;
import com.cyberfarm.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Cyber Frame taken into a small multiblock: invisible, as the machine's model covers its cell, and clicking it is
 * clicking the machine. Breaking it gives the frame back; without its machine it is a plain Cyber Frame again.
 */
public class HiddenFrameBlock extends PortBlock {
    public static final EnumProperty<Direction> TO_MACHINE = HorizontalDirectionalBlock.FACING;

    public HiddenFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TO_MACHINE, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TO_MACHINE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(TO_MACHINE) && !(neighborState.getBlock() instanceof MachineBlock)
                ? ModBlocks.CYBER_FRAME.get().defaultBlockState() : state;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos machine = pos.relative(state.getValue(TO_MACHINE));
        return level.getBlockState(machine).use(level, player, InteractionHand.MAIN_HAND, hitResult.withPosition(machine));
    }
}
