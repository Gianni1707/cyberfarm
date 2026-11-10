package com.cyberfarm.block;

import java.util.List;

import com.cyberfarm.block.entity.TransmitterDishBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Transmitter Dish: with three Cyber Frames it makes a 2x2 base (the frames to its right, behind it and in the corner
 * between, as the player sees its front), and the dish unfolds on its turret. TransmitterDishRenderer draws the turret
 * and the dish, which turn to the receivers. Conduits plug into it from the two sides without frames and from below.
 */
public class TransmitterDishBlock extends SmallMultiblockBlock {
    public static final MapCodec<TransmitterDishBlock> CODEC = simpleCodec(TransmitterDishBlock::new);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public TransmitterDishBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<TransmitterDishBlock> codec() {
        return CODEC;
    }

    @Override
    public List<Direction> frameSides(BlockState state) {
        Direction facing = state.getValue(FACING);
        return List.of(facing.getCounterClockWise(), facing.getOpposite());
    }

    @Override
    public List<BlockPos> frameCells(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos side = pos.relative(facing.getCounterClockWise());
        BlockPos back = pos.relative(facing.getOpposite());
        return List.of(side, back, side.relative(facing.getOpposite()));
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.transmitter_dish.needs_frames";
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (by instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof TransmitterDishBlockEntity dish) {
            dish.network(player);
        }
    }

    /** Once assembled, opens the GUI with the network's name and receivers. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(STAGE) == LOOSE || level.isClientSide()) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof TransmitterDishBlockEntity dish) {
            String name = dish.network(server).name;
            List<String> receivers = dish.receiverLabels();
            server.openMenu(dish, buf -> {
                buf.writeUtf(name);
                buf.writeCollection(receivers, (b, label) -> b.writeUtf(label));
            });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransmitterDishBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.TRANSMITTER_DISH.get());
    }
}
