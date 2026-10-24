package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.block.entity.MechHangarBlockEntity;
import com.cyberfarm.entity.MechEntity;
import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/**
 * A folded mech. It only unfolds on the pad of a complete Mech Hangar, in the middle and facing out
 * of the hangar; the hangar's console folds it back. The item keeps the battery's charge and the
 * modules.
 */
public class MechFrameItem extends Item {
    public MechFrameItem(Properties properties) {
        super(properties);
    }

    public static int energyOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), MechEntity.FACTORY_CHARGE);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        MechHangarBlockEntity hangar = MechHangarBlockEntity.ofPad(level, context.getClickedPos());
        if (hangar == null) {
            if (context.getPlayer() != null) {
                context.getPlayer().sendOverlayMessage(Component.translatable("message.cyberfarm.mech.hangar_only"));
            }
            return InteractionResult.FAIL;
        }
        Vec3 at = Vec3.atBottomCenterOf(hangar.padCentre().above());
        MechEntity mech = ModEntities.MECH.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (mech == null) {
            return InteractionResult.FAIL;
        }
        float yaw = hangar.facingOut().toYRot();
        mech.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        mech.setYHeadRot(yaw);
        mech.setYBodyRot(yaw);
        // It needs room to stand: 3.5 blocks of free space
        if (!level.noCollision(mech, mech.getBoundingBox()) || !level.getEntities(mech, mech.getBoundingBox().deflate(0.1)).isEmpty()) {
            if (context.getPlayer() != null) {
                context.getPlayer().sendOverlayMessage(Component.translatable("message.cyberfarm.mech.no_room"));
            }
            return InteractionResult.FAIL;
        }
        mech.setEnergy(energyOf(context.getItemInHand()));
        mech.setModules(context.getItemInHand().getOrDefault(ModDataComponents.MECH_MODULES.get(), 0));
        level.addFreshEntity(mech);
        mech.startUnfold();
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.cyberfarm.energy", String.format("%,d", energyOf(stack)), String.format("%,d", MechEntity.CAPACITY))
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.cyberfarm.mech_frame.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
