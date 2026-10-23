package com.cyberfarm.block;

import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;
import com.cyberfarm.item.MobCapsuleItem;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

public class MethaneExtractorBlock extends MachineBlock {
    public static final MapCodec<MethaneExtractorBlock> CODEC = simpleCodec(MethaneExtractorBlock::new);
    /** The chamber below is complete; switches the model to the lit one. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public MethaneExtractorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<MethaneExtractorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // A full Mob Capsule puts its mob straight into the chamber below
        if (!(stack.getItem() instanceof MobCapsuleItem) || !MobCapsuleItem.hasMob(stack)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof MethaneExtractorBlockEntity extractor) {
            String error = extractor.releaseFromCapsule(serverLevel, stack);
            player.sendOverlayMessage(Component.translatable(error != null ? error : "message.cyberfarm.extractor.loaded"));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Right-clicking any block of a formed chamber (frames, glass, ports, gates) opens the extractor on top.
     * Sneaking skips it, so blocks can still be placed against the chamber.
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos clicked = event.getPos();
        // Buckets fill from and pour into the ports themselves
        ItemStack held = event.getItemStack();
        if (player.isSecondaryUseActive() || level.getBlockState(clicked).getBlock() instanceof MethaneExtractorBlock
                || held.getItem() instanceof BucketItem) {
            return;
        }
        // The chamber spans one block around the extractor and three below it
        for (BlockPos top : BlockPos.betweenClosed(clicked.offset(-1, 0, -1), clicked.offset(1, 3, 1))) {
            BlockState state = level.getBlockState(top);
            if (state.getBlock() instanceof MethaneExtractorBlock && state.getValue(FORMED)) {
                if (!level.isClientSide() && level.getBlockEntity(top) instanceof MenuProvider provider) {
                    player.openMenu(provider);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MethaneExtractorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.METHANE_EXTRACTOR.get());
    }
}
