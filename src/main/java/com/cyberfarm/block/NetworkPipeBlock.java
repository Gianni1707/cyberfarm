package com.cyberfarm.block;

import net.minecraft.world.level.LevelAccessor;

import com.cyberfarm.port.PortEntityBlock;


import java.util.Map;

import com.cyberfarm.block.entity.NetworkPipeBlockEntity;
import com.cyberfarm.energy.ConduitNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Shared base of conduits and pipes: connects by itself to pipes of the same kind (any tier) and to
 * any block that handles what it carries, from any mod. Pipes store nothing; their block entity
 * passes whatever is pushed in straight to the blocks on the network, up to the {@link #rate()} of
 * the pipe it was pushed into.
 */
public abstract class NetworkPipeBlock extends PortEntityBlock {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;
    /** Per side: the pipe pulls out of the block there (right-click the arm to switch). */
    public static final Map<Direction, BooleanProperty> EXTRACT = Util.make(new java.util.EnumMap<Direction, BooleanProperty>(Direction.class), map -> {
        for (Direction side : Direction.values()) {
            map.put(side, BooleanProperty.create("extract_" + side.getSerializedName()));
        }
    });

    /** A pipe's block entity, which pulls from the blocks its arms are set to extract from. */
    public interface Puller {
        void pull(ServerLevel level, BlockPos pos, Direction side);
    }

    /** The node's glass cube. */
    private static final VoxelShape CORE = Block.box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5);
    /** Up to the tier sleeves, which every pipe and cable shares. */
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, Block.box(5.3, 5.3, 0, 10.7, 10.7, 5),
            Direction.SOUTH, Block.box(5.3, 5.3, 11, 10.7, 10.7, 16),
            Direction.WEST, Block.box(0, 5.3, 5.3, 5, 10.7, 10.7),
            Direction.EAST, Block.box(11, 5.3, 5.3, 16, 10.7, 10.7),
            Direction.DOWN, Block.box(5.3, 0, 5.3, 10.7, 5, 10.7),
            Direction.UP, Block.box(5.3, 11, 5.3, 10.7, 16, 10.7));

    private final int rate;

    protected NetworkPipeBlock(Properties properties, int rate) {
        super(properties);
        this.rate = rate;
        BlockState state = stateDefinition.any();
        for (Direction side : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(side), false).setValue(EXTRACT.get(side), false);
        }
        registerDefaultState(state);
    }

    /** How much can enter the network through this pipe per tick (FE or mB). */
    public int rate() {
        return rate;
    }

    /** Pipes of the same kind, whatever their tier, join into one network. */
    public boolean sameKind(BlockState state) {
        return state.getBlock().getClass() == getClass();
    }

    /** Whether the block at {@code pos} can exchange what this pipe carries through {@code side}. */
    protected abstract boolean canConnectToMachine(Level level, BlockPos pos, Direction side);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
        EXTRACT.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(direction), connectsTo(context.getLevel(), context.getClickedPos(), direction));
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Any change next to a pipe may change which blocks the network reaches.
        ConduitNetwork.invalidate();
        if (!(level instanceof Level realLevel)) {
            return state;
        }
        BlockState updated = state.setValue(CONNECTIONS.get(direction), connectsTo(realLevel, pos, direction));
        // Pulling stops when the block is gone
        return isMachineSide(updated, level, pos, direction) ? updated : updated.setValue(EXTRACT.get(direction), false);
    }

    /** Whether {@code side} leads to a machine or tank (not another pipe), so it can be set to pull. */
    public boolean isMachineSide(BlockState state, LevelReader level, BlockPos pos, Direction side) {
        return state.getValue(CONNECTIONS.get(side)) && !sameKind(level.getBlockState(pos.relative(side)));
    }

    private static boolean isPulling(BlockState state) {
        return EXTRACT.values().stream().anyMatch(state::getValue);
    }

    /** Right-click an arm touching a machine or tank: the pipe starts (or stops) pulling out of it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Vec3 offset = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        Direction side = Direction.getNearest(offset.x, offset.y, offset.z);
        if (!isMachineSide(state, level, pos, side)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            boolean extract = !state.getValue(EXTRACT.get(side));
            level.setBlock(pos, state.setValue(EXTRACT.get(side), extract), Block.UPDATE_ALL);
            // The network now treats that block as a source, not a destination
            ConduitNetwork.invalidate();
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, extract ? 1.4F : 0.8F);
            player.displayClientMessage(Component.translatable(extract ? "message.cyberfarm.pipe.extract" : "message.cyberfarm.pipe.normal"), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Scheduled by a pipe that started showing a flow, to empty the glass once it stops. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof NetworkPipeBlockEntity pipe) {
            pipe.checkFlow();
        }
    }

    /** Only pipes with an arm set to extract tick. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || !isPulling(state)) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> {
            if (entity instanceof Puller puller) {
                for (Direction side : Direction.values()) {
                    if (tickState.getValue(EXTRACT.get(side))) {
                        puller.pull((ServerLevel) tickLevel, pos, side);
                    }
                }
            }
        };
    }

    private boolean connectsTo(Level level, BlockPos pos, Direction direction) {
        BlockPos neighbor = pos.relative(direction);
        BlockState state = level.getBlockState(neighbor);
        // Another kind of pipe exposes the same capability, but a gas line never joins a liquid one
        if (state.getBlock() instanceof NetworkPipeBlock) {
            return sameKind(state);
        }
        return canConnectToMachine(level, neighbor, direction.getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        ConduitNetwork.invalidate();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        ConduitNetwork.invalidate();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(CONNECTIONS.get(direction))) {
                shape = Shapes.or(shape, ARMS.get(direction));
            }
        }
        return shape;
    }
}
