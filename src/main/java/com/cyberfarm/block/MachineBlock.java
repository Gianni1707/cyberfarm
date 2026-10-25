package com.cyberfarm.block;

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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A horizontal-facing machine that opens its menu on right-click and ticks on the server; on the
 * client it gives off {@link #ambient} particles while the server says it is working.
 */
public abstract class MachineBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    protected MachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
