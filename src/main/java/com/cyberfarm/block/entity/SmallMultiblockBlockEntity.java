package com.cyberfarm.block.entity;

import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.HiddenFrameBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** What the machines of {@link SmallMultiblockBlock} share: assembling, and the time the opening and fold-back take. */
public abstract class SmallMultiblockBlockEntity extends MachineBlockEntity {
    /** How long the renderer goes on drawing after a stage changes, while the chunk rebuilds under it. */
    public static final int REBUILD_TICKS = 20;
    /** Folding back is quicker than opening, and has less to wait for: a few frames of both models don't show in a moving machine. */
    private static final int CLOSE_SPEED = 2;
    private static final int CLOSE_WAIT_TICKS = 4;
    private static final int FORM_CHECK_INTERVAL = 10;
    /** Ticks between the knocks of a machine assembling. */
    private static final int ASSEMBLE_KNOCK = 9;

    private int stageTicks;
    private int stageWas = -1;
    /** Client side, for the renderer: the stage it last saw, and when. */
    private int shownStage = -1;
    private float shownStageTime;

    protected SmallMultiblockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, int capacity, int maxInsert, int maxExtract) {
        super(type, pos, state, slots, capacity, maxInsert, maxExtract);
    }

    /** How many ticks its opening takes. */
    public abstract int openTicks();

    /** A new stage has begun, or the machine has just loaded in one. */
    protected void stageChanged(ServerLevel level, BlockPos pos, BlockState state, int stage) {
    }

    /** Moves the stage on; returns whether the machine is assembled and open, so it may work. */
    protected final boolean advanceStage(ServerLevel level, BlockPos pos, BlockState state) {
        int stage = state.getValue(SmallMultiblockBlock.STAGE);
        if (stage != stageWas) {
            stageWas = stage;
            stageTicks = 0;
            stageChanged(level, pos, state, stage);
        }
        stageTicks++;
        if (stage == SmallMultiblockBlock.LOOSE && level.getGameTime() % FORM_CHECK_INTERVAL == 0) {
            tryToAssemble(level, pos, state);
        } else if ((stage == SmallMultiblockBlock.OPENING || stage == SmallMultiblockBlock.OPEN) && frameGone(level, pos, state)) {
            level.setBlock(pos, state.setValue(SmallMultiblockBlock.STAGE, SmallMultiblockBlock.CLOSING), Block.UPDATE_ALL);
        } else if (stage == SmallMultiblockBlock.OPENING && stageTicks >= openTicks()) {
            level.setBlock(pos, state.setValue(SmallMultiblockBlock.STAGE, SmallMultiblockBlock.OPEN), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.MULTIBLOCK_LOCK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (stage == SmallMultiblockBlock.OPENING && stageTicks % ASSEMBLE_KNOCK == 0) {
            // Knocks while its parts move into place, each a little higher or lower than the last
            level.playSound(null, pos, ModSounds.MULTIBLOCK_ASSEMBLE.get(), SoundSource.BLOCKS, 1.0F, 0.85F + level.getRandom().nextFloat() * 0.3F);
        } else if (stage == SmallMultiblockBlock.CLOSING && stageTicks >= openTicks() / CLOSE_SPEED + CLOSE_WAIT_TICKS) {
            releaseFrames(level, pos, state);
            level.setBlock(pos, state.setValue(SmallMultiblockBlock.STAGE, SmallMultiblockBlock.LOOSE), Block.UPDATE_ALL);
        }
        return stage == SmallMultiblockBlock.OPEN;
    }

    private static void tryToAssemble(ServerLevel level, BlockPos pos, BlockState state) {
        SmallMultiblockBlock machine = (SmallMultiblockBlock) state.getBlock();
        for (BlockPos cell : machine.frameCells(state, pos)) {
            BlockState frame = level.getBlockState(cell);
            if (!frame.is(ModBlocks.CYBER_FRAME.get()) || frame.getValue(CyberFrameBlock.FORMED)) {
                return;
            }
        }
        for (BlockPos cell : machine.frameCells(state, pos)) {
            // A frame above goes down first, one beside it along its row
            Direction toMachine = cell.getY() > pos.getY() ? Direction.DOWN
                    : Direction.getApproximateNearest(pos.getX() - cell.getX(), 0.0F, pos.getZ() - cell.getZ());
            level.setBlock(cell, ModBlocks.HIDDEN_FRAME.get().defaultBlockState().setValue(HiddenFrameBlock.TO_MACHINE, toMachine), Block.UPDATE_ALL);
        }
        level.setBlock(pos, level.getBlockState(pos).setValue(SmallMultiblockBlock.STAGE, SmallMultiblockBlock.OPENING), Block.UPDATE_ALL);
        level.playSound(null, pos, ModSounds.MULTIBLOCK_UNLATCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static boolean frameGone(ServerLevel level, BlockPos pos, BlockState state) {
        return !((SmallMultiblockBlock) state.getBlock()).frameCells(state, pos).stream()
                .allMatch(cell -> level.getBlockState(cell).is(ModBlocks.HIDDEN_FRAME.get()));
    }

    /** The frames still taken in are plain Cyber Frames again. */
    private static void releaseFrames(Level level, BlockPos pos, BlockState state) {
        for (BlockPos cell : ((SmallMultiblockBlock) state.getBlock()).frameCells(state, pos)) {
            if (level.getBlockState(cell).is(ModBlocks.HIDDEN_FRAME.get())) {
                level.setBlock(cell, ModBlocks.CYBER_FRAME.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        // A loose machine holds no frames: the hidden ones in its cells are a neighbour's
        if (level instanceof ServerLevel server && getBlockState().getValue(SmallMultiblockBlock.STAGE) != SmallMultiblockBlock.LOOSE) {
            releaseFrames(server, pos, getBlockState());
        }
    }

    /** Client side: how far the opening has got, in ticks. Arriving at a machine already open, the chunk has its model: nothing to hold. */
    public final float shownOpening(int stage, float time) {
        if (stage != shownStage) {
            shownStageTime = shownStage < 0 ? time - REBUILD_TICKS : time;
            shownStage = stage;
        }
        float since = time - shownStageTime;
        return switch (stage) {
            case SmallMultiblockBlock.OPENING -> Math.min(since, openTicks());
            case SmallMultiblockBlock.CLOSING -> Math.max(openTicks() - Math.max(since - CLOSE_WAIT_TICKS, 0.0F) * CLOSE_SPEED, 0.0F);
            default -> openTicks();
        };
    }

    /** Client side: whether the static model may still be rebuilding under the renderer. Call after {@link #shownOpening}. */
    public final boolean shownHolding(int stage, float time) {
        return stage == SmallMultiblockBlock.OPEN ? time - shownStageTime < REBUILD_TICKS
                : stage == SmallMultiblockBlock.CLOSING && time - shownStageTime < CLOSE_WAIT_TICKS;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
