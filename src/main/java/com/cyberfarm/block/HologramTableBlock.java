package com.cyberfarm.block;

import com.cyberfarm.block.entity.HologramTableBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Hologram Table: projects the land around it with its docks and drones; see {@code HologramRenderer}. Right-click to zoom. */
public class HologramTableBlock extends BaseEntityBlock {
    public static final MapCodec<HologramTableBlock> CODEC = simpleCodec(HologramTableBlock::new);

    // A low projector plate, the emitter in its middle, and an emitter post on each corner
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 5, 16),
            Block.box(0, 5, 0, 2, 8, 2), Block.box(14, 5, 0, 16, 8, 2), Block.box(0, 5, 14, 2, 8, 16), Block.box(14, 5, 14, 16, 8, 16));

    public HologramTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<HologramTableBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HologramTableBlockEntity table) {
            int radius = table.cycleZoom();
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.hologram.zoom", radius));
            level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.4F, 1.2F + radius / 80.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HologramTableBlockEntity(pos, state);
    }
}
