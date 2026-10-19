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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MethaneExtractorBlock extends MachineBlock {
    public static final MapCodec<MethaneExtractorBlock> CODEC = simpleCodec(MethaneExtractorBlock::new);

    public MethaneExtractorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<MethaneExtractorBlock> codec() {
        return CODEC;
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MethaneExtractorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.METHANE_EXTRACTOR.get());
    }
}
