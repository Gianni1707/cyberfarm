package com.cyberfarm.item;

import net.minecraft.world.InteractionResultHolder;

import net.minecraftforge.fluids.FluidStack;

import net.minecraftforge.fluids.capability.IFluidHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import com.cyberfarm.port.Caps;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Bucket of a CyberFarm fluid. Right-click a machine, reactor port or tank of any mod to pour its
 * 1000 mB in. Coolants are real liquids and can also be poured into the world; gases cannot.
 * An empty vanilla bucket fills up from any block holding 1000 mB of one of these fluids.
 */
public class FluidBucketItem extends BucketItem {
    public static final int AMOUNT = 1_000;
    /** Ticks a freshly filled bucket waits, so the same click does not pour it out again. */
    private static final int FILL_COOLDOWN = 5;

    public FluidBucketItem(Fluid fluid, Properties properties) {
        super(fluid, properties.stacksTo(1).craftRemainder(Items.BUCKET));
    }

    public boolean isGas() {
        return !(content instanceof FlowingFluid);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        IFluidHandler target = Caps.fluids(level, context.getClickedPos(), context.getClickedFace());
        if (target == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel) {
            // All or nothing, like a vanilla bucket
            FluidStack poured = new FluidStack(getFluid(), AMOUNT);
            if (target.fill(poured, IFluidHandler.FluidAction.SIMULATE) < AMOUNT) {
                return InteractionResult.FAIL;
            }
            target.fill(poured, IFluidHandler.FluidAction.EXECUTE);
            Player player = context.getPlayer();
            if (player != null) {
                player.setItemInHand(context.getHand(), BucketItem.getEmptySuccessItem(context.getItemInHand(), player));
            }
            level.playSound(null, context.getClickedPos(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Gases have nothing to place in the world; coolants pour like water. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return isGas() ? InteractionResultHolder.pass(player.getItemInHand(hand)) : super.use(level, player, hand);
    }

    /** An empty bucket right-clicked on a machine or tank takes out 1000 mB of a CyberFarm fluid. */
    public static void fillFromBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        Level level = event.getLevel();
        if (!held.is(Items.BUCKET) || !(level instanceof ServerLevel)) {
            return;
        }
        IFluidHandler source = Caps.fluids(level, event.getPos(), event.getFace());
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.getTanks(); i++) {
            FluidStack fluid = source.getFluidInTank(i);
            if (fluid.isEmpty() || !(fluid.getFluid().getBucket() instanceof FluidBucketItem bucket) || fluid.getAmount() < AMOUNT) {
                continue;
            }
            FluidStack taken = fluid.copy();
            taken.setAmount(AMOUNT);
            if (source.drain(taken, IFluidHandler.FluidAction.SIMULATE).getAmount() < AMOUNT) {
                continue;
            }
            source.drain(taken, IFluidHandler.FluidAction.EXECUTE);
            Player player = event.getEntity();
            ItemStack filled = new ItemStack(bucket);
            player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(held, player, filled));
            player.getCooldowns().addCooldown(filled.getItem(), FILL_COOLDOWN);
            level.playSound(null, event.getPos(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
    }
}
