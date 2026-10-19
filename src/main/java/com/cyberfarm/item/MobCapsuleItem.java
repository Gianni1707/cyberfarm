package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

/**
 * Mob Capsule: right-click a mob to store it, right-click a block to let it out, or a Methane
 * Extractor to put it straight into the extraction chamber. Holds one mob; bosses are refused.
 */
public class MobCapsuleItem extends Item {
    public MobCapsuleItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean hasMob(ItemStack stack) {
        return stack.has(ModDataComponents.CAPTURED_MOB.get());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        ItemStack capsule = player.getItemInHand(hand);
        if (hasMob(capsule)) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof Mob mob) || target instanceof DroneEntity || target.getType().builtInRegistryHolder().is(Tags.EntityTypes.BOSSES)) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.capsule.refused"));
            return InteractionResult.FAIL;
        }
        if (player.level() instanceof ServerLevel level) {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            mob.save(output);
            capsule.set(ModDataComponents.CAPTURED_MOB.get(), output.buildResult());
            level.playSound(null, mob.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0F, 0.8F);
            mob.discard();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack capsule = context.getItemInHand();
        if (!hasMob(capsule)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel() instanceof ServerLevel level) {
            Vec3 pos = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
            release(level, capsule, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Lets the stored mob out at {@code pos} and empties the capsule. Released mobs never despawn. */
    public static @Nullable Entity release(ServerLevel level, ItemStack capsule, Vec3 pos) {
        CompoundTag tag = capsule.get(ModDataComponents.CAPTURED_MOB.get());
        if (tag == null) {
            return null;
        }
        CompoundTag data = tag.copy();
        // A copied capsule must not bring back two mobs with the same id
        data.remove("UUID");
        Entity entity = EntityType.loadEntityRecursive(data, level, EntitySpawnReason.BUCKET, loaded -> {
            loaded.snapTo(pos.x, pos.y, pos.z, loaded.getYRot(), loaded.getXRot());
            return loaded;
        });
        if (entity == null) {
            return null;
        }
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(entity);
        capsule.remove(ModDataComponents.CAPTURED_MOB.get());
        level.playSound(null, entity.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0F, 1.0F);
        return entity;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasMob(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        CompoundTag tag = stack.get(ModDataComponents.CAPTURED_MOB.get());
        if (tag == null) {
            builder.accept(Component.translatable("tooltip.cyberfarm.capsule.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        Component name = EntityType.byString(tag.getStringOr("id", ""))
                .map(EntityType::getDescription)
                .orElse(Component.literal("?"));
        builder.accept(Component.translatable("tooltip.cyberfarm.capsule.contains", name).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.cyberfarm.capsule.release").withStyle(ChatFormatting.DARK_GRAY));
    }
}
