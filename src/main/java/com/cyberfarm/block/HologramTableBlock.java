package com.cyberfarm.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cyberfarm.block.entity.HologramTableBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hologram Table: projects the land around it with its docks and drones; see {@code HologramRenderer}. Right-click to zoom.
 * Tables side by side merge by themselves: four in a square make a 2x2, nine a 3x3, with one bigger projection that reaches
 * further. {@link #CELL} says which piece of which size a block is, so each piece has its own model.
 */
public class HologramTableBlock extends BaseEntityBlock {
    public static final MapCodec<HologramTableBlock> CODEC = simpleCodec(HologramTableBlock::new);
    /** 0: a table on its own; 1-4: the cells of a 2x2; 5-13: the cells of a 3x3, row by row from the north-west. */
    public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, 13);
    /** Enough to merge a 9x9 of tables; past that the far ones wait for the next change near them. */
    private static final int MAX_GROUP = 81;

    // A low projector plate and an emitter post on each outer corner of the whole table
    private static final VoxelShape[] SHAPES = new VoxelShape[14];
    static {
        for (int cell = 0; cell < SHAPES.length; cell++) {
            int last = size(cell) - 1;
            VoxelShape shape = Block.box(0, 0, 0, 16, 5, 16);
            for (int x = 0; x <= 14; x += 14) {
                for (int z = 0; z <= 14; z += 14) {
                    if (dx(cell) == (x == 0 ? 0 : last) && dz(cell) == (z == 0 ? 0 : last)) {
                        shape = Shapes.or(shape, Block.box(x, 5, z, x + 2, 8, z + 2));
                    }
                }
            }
            SHAPES[cell] = shape;
        }
    }

    public HologramTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CELL, 0));
    }

    public static int size(int cell) {
        return cell == 0 ? 1 : cell <= 4 ? 2 : 3;
    }

    private static int dx(int cell) {
        return cell == 0 ? 0 : cell <= 4 ? (cell - 1) % 2 : (cell - 5) % 3;
    }

    private static int dz(int cell) {
        return cell == 0 ? 0 : cell <= 4 ? (cell - 1) / 2 : (cell - 5) / 3;
    }

    private static int cell(int size, int dx, int dz) {
        return size == 1 ? 0 : size == 2 ? 1 + dz * 2 + dx : 5 + dz * 3 + dx;
    }

    /** Whether this cell is the one that keeps the zoom and draws the projection: the middle one (of a 2x2, the south-east). */
    public static boolean isMaster(int cell) {
        return dx(cell) == size(cell) / 2 && dz(cell) == size(cell) / 2;
    }

    private static BlockPos master(BlockPos pos, BlockState state) {
        int cell = state.getValue(CELL);
        return pos.offset(size(cell) / 2 - dx(cell), 0, size(cell) / 2 - dz(cell));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CELL);
    }

    @Override
    protected MapCodec<HologramTableBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(CELL)];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(master(pos, state)) instanceof HologramTableBlockEntity table) {
            int radius = table.cycleZoom();
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.hologram.zoom", radius));
            level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.4F, 1.2F + radius / 80.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            regroup(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            regroup(level, pos.relative(side));
        }
    }

    /** Sorts out the tables joined to {@code from}: from the north-west, the biggest square that fits, again and again. */
    private void regroup(Level level, BlockPos from) {
        Set<BlockPos> free = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>(List.of(from));
        while (!open.isEmpty() && free.size() < MAX_GROUP) {
            BlockPos pos = open.poll();
            if (level.getBlockState(pos).is(this) && free.add(pos)) {
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    open.add(pos.relative(side));
                }
            }
        }
        List<BlockPos> order = new ArrayList<>(free);
        order.sort(Comparator.<BlockPos>comparingInt(pos -> pos.getZ()).thenComparingInt(pos -> pos.getX()));
        for (BlockPos pos : order) {
            if (!free.contains(pos)) {
                continue;
            }
            int size = fits(free, pos, 3) ? 3 : fits(free, pos, 2) ? 2 : 1;
            for (int dz = 0; dz < size; dz++) {
                for (int dx = 0; dx < size; dx++) {
                    BlockPos at = pos.offset(dx, 0, dz);
                    free.remove(at);
                    level.setBlock(at, level.getBlockState(at).setValue(CELL, cell(size, dx, dz)), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static boolean fits(Set<BlockPos> free, BlockPos corner, int size) {
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                if (!free.contains(corner.offset(dx, 0, dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HologramTableBlockEntity(pos, state);
    }
}
