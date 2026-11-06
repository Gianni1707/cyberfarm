package com.cyberfarm.block;

import com.cyberfarm.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * A machine that assembles with Cyber Frames beside it into something bigger, and only then works: its frames hide
 * ({@link HiddenFrameBlock}) and it opens with an animation; breaking a frame folds it back. Alone, a click shows where the frames go.
 * <p>
 * shortcut: every frame touches the machine block, enough for 2x1 and 1x3; a 2x2 needs a frame reached through another.
 */
public abstract class SmallMultiblockBlock extends LitMachineBlock {
    public static final int LOOSE = 0, OPENING = 1, OPEN = 2, CLOSING = 3;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", LOOSE, CLOSING);
    /** Points along each edge of a frame's cell when it is asked where the frames go. */
    private static final int HINT_POINTS = 5;

    protected SmallMultiblockBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STAGE, LOOSE));
    }

    public abstract List<Direction> frameSides(BlockState state);

    /** What a click tells a player whose machine is still alone. */
    protected abstract String frameMessage();

    @Override
    protected boolean hasPipePorts() {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STAGE);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
        int stage = state.getValue(STAGE);
        boolean frameGone = frameSides(state).contains(direction) && !neighborState.is(ModBlocks.HIDDEN_FRAME.get());
        return frameGone && (stage == OPENING || stage == OPEN) ? updated.setValue(STAGE, CLOSING) : updated;
    }

    /** Until it is assembled, a click shows where its frames go instead of opening the GUI. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(STAGE) != LOOSE) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level.isClientSide()) {
            frameSides(state).forEach(side -> outline(level, pos.relative(side)));
        } else {
            player.sendOverlayMessage(Component.translatable(frameMessage()));
        }
        return InteractionResult.SUCCESS;
    }

    /** Sparks along the twelve edges of a block's cell. */
    private static void outline(Level level, BlockPos cell) {
        for (int i = 0; i <= HINT_POINTS; i++) {
            double along = (double) i / HINT_POINTS;
            for (int a = 0; a <= 1; a++) {
                for (int b = 0; b <= 1; b++) {
                    spark(level, cell, along, a, b);
                    spark(level, cell, a, along, b);
                    spark(level, cell, a, b, along);
                }
            }
        }
    }

    private static void spark(Level level, BlockPos cell, double x, double y, double z) {
        level.addParticle(ParticleTypes.END_ROD, cell.getX() + x, cell.getY() + y, cell.getZ() + z, 0.0, 0.0, 0.0);
    }
}
