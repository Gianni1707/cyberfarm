package com.cyberfarm.block;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.item.DroneItem;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DroneDockBlock extends MachineBlock {
    public static final MapCodec<DroneDockBlock> CODEC = simpleCodec(DroneDockBlock::new);

    // Landing pad plus four corner beacons
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 6, 16),
            Block.box(0, 6, 0, 3, 9, 3),
            Block.box(13, 6, 0, 16, 9, 3),
            Block.box(0, 6, 13, 3, 9, 16),
            Block.box(13, 6, 13, 16, 9, 16));

    public DroneDockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<DroneDockBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Let the drone handle the click itself instead of opening the GUI (the survey tool acts before the block).
        if (stack.getItem() instanceof DroneItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DroneDockBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.DRONE_DOCK.get());
    }
}
