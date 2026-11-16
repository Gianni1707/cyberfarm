package com.cyberfarm.block;

import com.cyberfarm.block.entity.CapabilityPortBlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import com.cyberfarm.port.PortBlock;
import net.minecraft.world.InteractionHand;

import com.cyberfarm.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Cyber Frame taken into a small multiblock, and clicking it is clicking the machine. Usually invisible, as the machine's model
 * covers its cell; a cell too far for that model shows its own {@link #PIECE}. Breaking it gives the frame back; the machine gives
 * back the others when it folds or goes (see SmallMultiblockBlockEntity). Pipes and cables plug into it as into the machine (it
 * hands over the machine's capabilities), and it shows a socket where they do.
 */
public class HiddenFrameBlock extends PortBlock implements EntityBlock {
    /** Towards the machine, along any frames in between, which may turn on the way. */
    public static final EnumProperty<Direction> TO_MACHINE = DirectionalBlock.FACING;
    public static final int NOTHING = 0, STEAM_CYLINDER = 1, STEAM_DYNAMO = 2;
    public static final IntegerProperty PIECE = IntegerProperty.create("piece", NOTHING, STEAM_DYNAMO);

    public HiddenFrameBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(TO_MACHINE, Direction.NORTH).setValue(PIECE, NOTHING);
        for (BooleanProperty port : MachineBlock.PORTS.values()) {
            state = state.setValue(port, false);
        }
        registerDefaultState(state);
    }

    /**
     * On Forge 1.20.1 only block entities expose capabilities: every frame carries one, so conduits plug into it and
     * a hopper or pipe on any frame reaches the machine.
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CapabilityPortBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TO_MACHINE, PIECE);
        MachineBlock.PORTS.values().forEach(builder::add);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        if (state.getValue(PIECE) != NOTHING) {
            return RenderShape.MODEL;
        }
        for (BooleanProperty port : MachineBlock.PORTS.values()) {
            if (state.getValue(port)) {
                return RenderShape.MODEL;
            }
        }
        return RenderShape.INVISIBLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        return updated.is(this) ? MachineBlock.plug(updated, direction, neighborState) : updated;
    }

    /** Its machine decides: most fill the cell, an open one (the Trinket Table) only where it has something there. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockPos machine = machine(level, pos, state);
        BlockState at = level.getBlockState(machine);
        return at.getBlock() instanceof SmallMultiblockBlock block ? block.frameShape(at, machine, pos) : Shapes.block();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos machine = machine(level, pos, state);
        return level.getBlockState(machine).use(level, player, InteractionHand.MAIN_HAND, hitResult.withPosition(machine));
    }

    /** The machine this frame belongs to. */
    public BlockPos machine(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos machine = pos.relative(state.getValue(TO_MACHINE));
        while (level.getBlockState(machine).is(this)) {
            machine = machine.relative(level.getBlockState(machine).getValue(TO_MACHINE));
        }
        return machine;
    }
}
