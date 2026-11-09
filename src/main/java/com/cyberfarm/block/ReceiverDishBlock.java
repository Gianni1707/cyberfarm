package com.cyberfarm.block;

import java.util.List;

import com.cyberfarm.block.entity.ReceiverDishBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Receiver Dish: a small multiblock two blocks tall, the dish block and a Cyber Frame on top; once assembled the small
 * stepped dish unfolds on its mount. ReceiverDishRenderer draws the dish, which turns to its transmitter.
 */
public class ReceiverDishBlock extends SmallMultiblockBlock {
    public static final MapCodec<ReceiverDishBlock> CODEC = simpleCodec(ReceiverDishBlock::new);

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public ReceiverDishBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ReceiverDishBlock> codec() {
        return CODEC;
    }

    @Override
    public List<Direction> frameSides(BlockState state) {
        return List.of(Direction.UP);
    }

    @Override
    protected String frameMessage() {
        return "message.cyberfarm.receiver_dish.needs_frame";
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Once assembled, opens the GUI with the networks the player may link to from here. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(STAGE) == LOOSE || level.isClientSide()) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ReceiverDishBlockEntity dish) {
            List<ReceiverDishBlockEntity.Choice> choices = dish.choices(server);
            server.openMenu(dish, buf -> ReceiverDishBlockEntity.writeChoices(buf, choices));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReceiverDishBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.RECEIVER_DISH.get());
    }
}
