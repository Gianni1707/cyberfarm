package com.cyberfarm.block;

import com.cyberfarm.block.entity.FluidTankBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * Liquid Tank or Gas Tank: stores one fluid, liquids or gases like the matching pipe. Passive: it
 * takes what pipes and buckets put in, and gives it out to pipes set to pull from it.
 */
public class FluidTankBlock extends MachineBlock {
    private final boolean gas;
    private final MapCodec<FluidTankBlock> codec = simpleCodec(properties -> new FluidTankBlock(properties, gas()));

    public FluidTankBlock(Properties properties, boolean gas) {
        super(properties);
        this.gas = gas;
    }

    public boolean gas() {
        return gas;
    }

    @Override
    protected MapCodec<FluidTankBlock> codec() {
        return codec;
    }

    /** Buckets of any mod fill or empty the tank, like a cauldron. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank && FluidUtil.interactWithFluidHandler(player, hand, pos, tank.getTank(), null)) {
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** No GUI: right-clicking shows what is inside. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            player.sendOverlayMessage(tank.status());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }
}
