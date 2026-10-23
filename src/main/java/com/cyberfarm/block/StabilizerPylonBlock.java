package com.cyberfarm.block;

import java.util.Locale;

import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Stabilizer Pylon: stacked into a column, with a footing at the bottom and the emitter on top.
 * A Resonance Core finds the columns lined up on its sides, fires their beams, and takes and gives
 * its power through every block of them.
 */
public class StabilizerPylonBlock extends Block {
    public static final MapCodec<StabilizerPylonBlock> CODEC = simpleCodec(StabilizerPylonBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    // A socket on each side where an Energy Conduit plugs in, bridging the gap to the narrow shaft
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    /** Height of the emitter's centre in the top block, where the beam starts. */
    public static final float EMITTER_Y = 8.5F / 16;

    private static final VoxelShape FOOTING = Block.box(2, 0, 2, 14, 3, 14);
    private static final VoxelShape EMITTER = Shapes.or(Block.box(3, 6, 3, 13, 11, 13), Block.box(5, 11, 5, 11, 14, 11), Block.box(6, 14, 6, 10, 16, 10));
    private static final VoxelShape SINGLE = Shapes.or(FOOTING, Block.box(4, 3, 4, 12, 6, 12), EMITTER);
    private static final VoxelShape BOTTOM = Shapes.or(FOOTING, Block.box(4, 3, 4, 12, 16, 12));
    private static final VoxelShape MIDDLE = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape TOP = Shapes.or(Block.box(4, 0, 4, 12, 6, 12), EMITTER);

    public enum Part implements StringRepresentable {
        SINGLE, BOTTOM, MIDDLE, TOP;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public StabilizerPylonBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, Part.SINGLE)
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected MapCodec<StabilizerPylonBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = withPart(defaultBlockState(), context.getLevel(), context.getClickedPos());
        for (Direction side : Direction.Plane.HORIZONTAL) {
            state = state.setValue(socket(side), isConduit(context.getLevel().getBlockState(context.getClickedPos().relative(side))));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction.getAxis() == Direction.Axis.Y ? withPart(state, level, pos) : state.setValue(socket(direction), isConduit(neighbourState));
    }

    private static BooleanProperty socket(Direction side) {
        return switch (side) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    private static boolean isConduit(BlockState state) {
        return state.getBlock() instanceof EnergyConduitBlock;
    }

    private BlockState withPart(BlockState state, LevelReader level, BlockPos pos) {
        boolean above = level.getBlockState(pos.above()).is(this);
        boolean below = level.getBlockState(pos.below()).is(this);
        Part part = above ? (below ? Part.MIDDLE : Part.BOTTOM) : (below ? Part.TOP : Part.SINGLE);
        return state.setValue(PART, part);
    }

    /** Right-clicking any block of a column opens the core it is lined up with. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return ResonanceCoreBlockEntity.openFromPylon(level, pos, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case SINGLE -> SINGLE;
            case BOTTOM -> BOTTOM;
            case MIDDLE -> MIDDLE;
            case TOP -> TOP;
        };
    }
}
