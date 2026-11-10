package com.cyberfarm.block;

import com.cyberfarm.port.PortEntityBlock;


import com.cyberfarm.block.entity.MachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * A horizontal-facing machine that opens its menu on right-click and ticks on the server; on the
 * client it gives off {@link #ambient} particles while the server says it is working.
 */
public abstract class MachineBlock extends PortEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Sockets the model shows where a pipe or conduit is plugged in, for machines with {@link #hasPipePorts}. */
    public static final Map<Direction, BooleanProperty> PORTS = PipeBlock.PROPERTY_BY_DIRECTION;

    protected MachineBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
        if (hasPipePorts()) {
            for (BooleanProperty port : PORTS.values()) {
                state = state.setValue(port, false);
            }
        }
        registerDefaultState(state);
    }

    /** Called from the constructor, so it must not read fields. */
    protected boolean hasPipePorts() {
        return false;
    }

    /** A port is shown only where the pipe next door actually connects to this side. */
    private static BlockState plug(BlockState state, Direction side, BlockState neighbor) {
        boolean plugged = neighbor.getBlock() instanceof NetworkPipeBlock
                && neighbor.getValue(NetworkPipeBlock.CONNECTIONS.get(side.getOpposite()));
        return state.setValue(PORTS.get(side), plugged);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        return hasPipePorts() && updated.is(this) ? plug(updated, direction, neighborState) : updated;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (hasPipePorts()) {
            for (Direction side : Direction.values()) {
                state = plug(state, side, context.getLevel().getBlockState(context.getClickedPos().relative(side)));
            }
        }
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        if (hasPipePorts()) {
            PORTS.values().forEach(builder::add);
        }
    }

    protected static <T extends BlockEntity, E extends MachineBlockEntity> @Nullable BlockEntityTicker<T> machineTicker(
            Level level, BlockEntityType<T> actualType, BlockEntityType<E> expectedType) {
        return level instanceof ServerLevel
                ? createTickerHelper(actualType, expectedType, (lvl, pos, state, machine) -> {
                    machine.serverTick((ServerLevel) lvl, pos, state);
                    machine.tickStatus((ServerLevel) lvl, pos);
                })
                : createTickerHelper(actualType, expectedType, (lvl, pos, state, machine) -> {
                    if (machine.status.working() && state.getBlock() instanceof MachineBlock block) {
                        block.ambient(state, lvl, pos, lvl.getRandom());
                    }
                });
    }

    /**
     * Called every client tick while the machine works: by default a spark now and then off one of
     * its sides. Machines with a look of their own override it.
     */
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            Vec3 at = onSide(state, level, pos, random);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    /** A random point just outside one of the machine's four sides, between its foot and its top. */
    protected static Vec3 onSide(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        double along = random.nextDouble() * 0.8 - 0.4;
        double x = pos.getX() + 0.5 + side.getStepX() * 0.52 + (side.getAxis() == Direction.Axis.Z ? along : 0.0);
        double z = pos.getZ() + 0.5 + side.getStepZ() * 0.52 + (side.getAxis() == Direction.Axis.X ? along : 0.0);
        return new Vec3(x, pos.getY() + 0.05 + random.nextDouble() * (top(state, level, pos) - 0.1), z);
    }

    /** A random point on the machine's top, within {@code spread} of its middle. */
    protected static Vec3 onTop(BlockState state, Level level, BlockPos pos, RandomSource random, double spread) {
        return new Vec3(pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2 * spread, pos.getY() + top(state, level, pos) + 0.02,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2 * spread);
    }

    private static double top(BlockState state, Level level, BlockPos pos) {
        VoxelShape shape = state.getShape(level, pos);
        return shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y);
    }
}
