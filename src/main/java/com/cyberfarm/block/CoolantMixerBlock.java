package com.cyberfarm.block;

import com.cyberfarm.block.entity.CoolantMixerBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A small multiblock: with a Cyber Frame on its right, as the player sees its front, it assembles into a 2x1 bench that
 * opens and shows both tanks (see tools/machinemodels.py; CoolantMixerRenderer draws what moves). Until then it does not work.
 */
public class CoolantMixerBlock extends LitMachineBlock {
    public static final MapCodec<CoolantMixerBlock> CODEC = simpleCodec(CoolantMixerBlock::new);
    public static final int LOOSE = 0, OPENING = 1, OPEN = 2, CLOSING = 3;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", LOOSE, CLOSING);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 1, 16), Block.box(0.5, 1, 0.5, 15.5, 16, 15.5));
    /** Points along each edge of the frame's cell when it is asked where the frame goes. */
    private static final int HINT_POINTS = 5;

    public CoolantMixerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STAGE, LOOSE));
    }

    @Override
    protected MapCodec<CoolantMixerBlock> codec() {
        return CODEC;
    }

    /** The player faces its front, so their right is the machine's counter-clockwise. */
    public static Direction frameSide(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    @Override
    protected boolean hasPipePorts() {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        boolean frameGone = direction == frameSide(state) && !neighborState.is(ModBlocks.HIDDEN_FRAME.get());
        return frameGone && (stage == OPENING || stage == OPEN) ? updated.setValue(STAGE, CLOSING) : updated;
    }

    /** Until it is assembled, a click shows where its frame goes instead of opening the GUI. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(STAGE) != LOOSE) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level.isClientSide()) {
            outline(level, pos.relative(frameSide(state)));
        } else {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.small_multiblock.needs_frame"));
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoolantMixerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.COOLANT_MIXER.get());
    }

    /** Frost drifting off the coolant tank's cap, over the frame's cell, while it mixes. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            Direction side = frameSide(state);
            Direction back = state.getValue(FACING).getOpposite();
            double across = (random.nextDouble() - 0.5) * 6.0 / 16.0;
            double out = 17.5 / 16.0;
            double x = pos.getX() + 0.5 + side.getStepX() * (out + across) + back.getStepX() / 16.0;
            double z = pos.getZ() + 0.5 + side.getStepZ() * (out + across) + back.getStepZ() / 16.0;
            level.addParticle(ParticleTypes.SNOWFLAKE, x, pos.getY() + 1.02, z, 0.0, 0.01, 0.0);
        }
    }
}
