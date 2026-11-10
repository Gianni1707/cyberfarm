package com.cyberfarm.block;

import com.cyberfarm.port.PortBlock;
import net.minecraft.world.InteractionHand;

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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Cyber Frame taken into a small multiblock, and clicking it is clicking the machine. Usually invisible, as the machine's model
 * covers its cell; a cell too far for that model shows its own {@link #PIECE}. Breaking it gives the frame back; the machine gives
 * back the others when it folds or goes (see SmallMultiblockBlockEntity).
 */
public class HiddenFrameBlock extends PortBlock {
    /** Towards the machine, along any frames in between. */
    public static final EnumProperty<Direction> TO_MACHINE = HorizontalDirectionalBlock.FACING;
    public static final int NOTHING = 0, STEAM_CYLINDER = 1, STEAM_DYNAMO = 2;
    public static final IntegerProperty PIECE = IntegerProperty.create("piece", NOTHING, STEAM_DYNAMO);

    public HiddenFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TO_MACHINE, Direction.NORTH).setValue(PIECE, NOTHING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TO_MACHINE, PIECE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PIECE) == NOTHING ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos machine = pos.relative(state.getValue(TO_MACHINE));
        while (level.getBlockState(machine).is(this)) {
            machine = machine.relative(state.getValue(TO_MACHINE));
        }
        return level.getBlockState(machine).use(level, player, InteractionHand.MAIN_HAND, hitResult.withPosition(machine));
    }
}
