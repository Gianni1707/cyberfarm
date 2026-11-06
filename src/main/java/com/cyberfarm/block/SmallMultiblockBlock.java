package com.cyberfarm.block;

import com.cyberfarm.client.renderer.FrameDroneRenderer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * A machine that assembles with Cyber Frames beside it into something bigger, and only then works: its frames hide
 * ({@link HiddenFrameBlock}) and it opens with an animation; breaking a frame folds it back. Alone, a click sends drones to show where the frames go.
 * <p>
 * shortcut: the frames go in straight arms from the machine block, enough for 2x1, 1x3 and 1x5; a 2x2 needs a frame off an arm.
 */
public abstract class SmallMultiblockBlock extends LitMachineBlock {
    public static final int LOOSE = 0, OPENING = 1, OPEN = 2, CLOSING = 3;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", LOOSE, CLOSING);

    protected SmallMultiblockBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STAGE, LOOSE));
    }

    public abstract List<Direction> frameSides(BlockState state);

    /** How many frames in a row on each of those sides. */
    public int frameDepth() {
        return 1;
    }

    public final List<BlockPos> frameCells(BlockState state, BlockPos pos) {
        List<BlockPos> cells = new ArrayList<>();
        for (Direction side : frameSides(state)) {
            for (int i = 1; i <= frameDepth(); i++) {
                cells.add(pos.relative(side, i));
            }
        }
        return cells;
    }

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

    /** Until it is assembled, a click sends out the drones that show where its frames go, instead of opening the GUI. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(STAGE) != LOOSE) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level.isClientSide()) {
            FrameDroneRenderer.clicked(level, pos, state, this);
        } else {
            player.sendOverlayMessage(Component.translatable(frameMessage()));
        }
        return InteractionResult.SUCCESS;
    }


}
