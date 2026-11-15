package com.cyberfarm.block;

import java.util.List;

import com.cyberfarm.block.entity.HumanDockBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Human Dock: step on it to charge everything you carry. A pad with a Cyber Frame above it: once the frame is there it
 * hides, the arch rises behind the pad, and the dock works.
 */
public class HumanDockBlock extends SmallMultiblockBlock {
    public static final MapCodec<HumanDockBlock> CODEC = simpleCodec(HumanDockBlock::new);

    public HumanDockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<HumanDockBlock> codec() {
        return CODEC;
    }

    @Override
    public List<Direction> frameSides(BlockState state) {
        return List.of(Direction.UP);
    }

    /** Stumps and arch stand 3.8 px deep along the back; the pad is a low step, so you can walk on and stand there. */
    private static final double BACK = 3.8, PAD = 3.75;

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Folded, the lintel rests on the stumps: the back fills the cell's height
        return Shapes.or(Block.box(0, 0, 0, 16, PAD, 16), back(state, 16));
    }

    /** In the frame's cell only the raised arch along the back (it tops out at 27.9 px): the player's head has room. */
    @Override
    public VoxelShape frameShape(BlockState state, BlockPos machine, BlockPos frame) {
        return back(state, 12);
    }

    private static VoxelShape back(BlockState state, double height) {
        return switch (state.getValue(FACING).getOpposite()) {
            case NORTH -> Block.box(0, 0, 0, 16, height, BACK);
            case SOUTH -> Block.box(0, 0, 16 - BACK, 16, height, 16);
            case WEST -> Block.box(0, 0, 0, BACK, height, 16);
            default -> Block.box(16 - BACK, 0, 0, 16, height, 16);
        };
    }

    /** Nothing to set: once it stands, a right-click does nothing (alone, it still shows where its frame goes). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return state.getValue(STAGE) == LOOSE ? super.useWithoutItem(state, level, pos, player, hitResult) : InteractionResult.PASS;
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.human_dock.needs_frame";
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HumanDockBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.HUMAN_DOCK.get());
    }
}
