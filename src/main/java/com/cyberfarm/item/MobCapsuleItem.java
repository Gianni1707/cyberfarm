package com.cyberfarm.item;

import net.minecraft.world.level.Level;

import java.util.List;

import java.util.function.Consumer;

import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
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
        return ModDataComponents.CAPTURED_MOB.has(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        ItemStack capsule = player.getItemInHand(hand);
        if (hasMob(capsule)) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof Mob mob) || target instanceof DroneEntity || target.getType().is(Tags.EntityTypes.BOSSES)) {
            player.displayClientMessage(Component.translatable("message.cyberfarm.capsule.refused"), true);
            return InteractionResult.FAIL;
        }
        if (player.level() instanceof ServerLevel level) {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            mob.save(output);
            ModDataComponents.CAPTURED_MOB.set(capsule, output.buildResult());
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
        CompoundTag tag = ModDataComponents.CAPTURED_MOB.get(capsule);
        if (tag == null) {
            return null;
        }
        CompoundTag data = tag.copy();
        // A copied capsule must not bring back two mobs with the same id
        data.remove("UUID");
        Entity entity = EntityType.loadEntityRecursive(data, level, new EntitySpawnRequest(MobSpawnType.BUCKET, false), loaded -> {
            loaded.moveTo(pos.x, pos.y, pos.z, loaded.getYRot(), loaded.getXRot());
            return loaded;
        });
        if (entity == null) {
            return null;
        }
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(entity);
        ModDataComponents.CAPTURED_MOB.remove(capsule);
        level.playSound(null, entity.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0F, 1.0F);
        return entity;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasMob(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Consumer<Component> builder = tooltip::add;
        CompoundTag tag = ModDataComponents.CAPTURED_MOB.get(stack);
        if (tag == null) {
            builder.accept(Component.translatable("tooltip.cyberfarm.capsule.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        Component name = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(tag.getStringOr("id", "")))
                .map(EntityType::getDescription)
                .orElse(Component.literal("?"));
        builder.accept(Component.translatable("tooltip.cyberfarm.capsule.contains", name).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.cyberfarm.capsule.release").withStyle(ChatFormatting.DARK_GRAY));
    }
}
