package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Links to a Drone Dock (right-click the dock), then marks two opposite corners of the work area.
 * Sneak + right-click in the air clears the first corner.
 */
public class SurveyToolItem extends Item {
    public static final int MAX_AREA_SIZE = 33;
    public static final int MAX_DOCK_DISTANCE = 48;

    public SurveyToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        if (level.getBlockEntity(pos) instanceof DroneDockBlockEntity) {
            stack.set(ModDataComponents.LINKED_DOCK.get(), pos);
            stack.remove(ModDataComponents.SURVEY_CORNER.get());
            message(player, Component.translatable("message.cyberfarm.survey.linked", pos.toShortString()));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.6F);
            return InteractionResult.SUCCESS;
        }

        BlockPos dockPos = stack.get(ModDataComponents.LINKED_DOCK.get());
        if (dockPos == null) {
            message(player, Component.translatable("message.cyberfarm.survey.no_dock"));
            return InteractionResult.FAIL;
        }

        BlockPos corner = stack.get(ModDataComponents.SURVEY_CORNER.get());
        if (corner == null) {
            stack.set(ModDataComponents.SURVEY_CORNER.get(), pos);
            message(player, Component.translatable("message.cyberfarm.survey.corner", pos.toShortString()));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
            return InteractionResult.SUCCESS;
        }

        stack.remove(ModDataComponents.SURVEY_CORNER.get());
        if (!(level.getBlockEntity(dockPos) instanceof DroneDockBlockEntity dock)) {
            stack.remove(ModDataComponents.LINKED_DOCK.get());
            message(player, Component.translatable("message.cyberfarm.survey.dock_missing"));
            return InteractionResult.FAIL;
        }

        int width = Math.abs(pos.getX() - corner.getX()) + 1;
        int depth = Math.abs(pos.getZ() - corner.getZ()) + 1;
        if (width > MAX_AREA_SIZE || depth > MAX_AREA_SIZE) {
            message(player, Component.translatable("message.cyberfarm.survey.too_big", MAX_AREA_SIZE, MAX_AREA_SIZE));
            return InteractionResult.FAIL;
        }

        // Covers both the farmland and the crops on it, whichever the player clicked.
        BoundingBox area = new BoundingBox(
                Math.min(pos.getX(), corner.getX()), Math.min(pos.getY(), corner.getY()) - 1, Math.min(pos.getZ(), corner.getZ()),
                Math.max(pos.getX(), corner.getX()), Math.max(pos.getY(), corner.getY()) + 2, Math.max(pos.getZ(), corner.getZ()));
        if (!area.inflatedBy(MAX_DOCK_DISTANCE).isInside(dockPos)) {
            message(player, Component.translatable("message.cyberfarm.survey.too_far", MAX_DOCK_DISTANCE));
            return InteractionResult.FAIL;
        }

        dock.setWorkArea(area);
        message(player, Component.translatable("message.cyberfarm.survey.area_set", width, depth));
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.8F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(ModDataComponents.SURVEY_CORNER.get())) {
            stack.remove(ModDataComponents.SURVEY_CORNER.get());
            if (!level.isClientSide()) {
                message(player, Component.translatable("message.cyberfarm.survey.cleared"));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void message(Player player, Component text) {
        if (player != null) {
            player.sendOverlayMessage(text);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        BlockPos dock = stack.get(ModDataComponents.LINKED_DOCK.get());
        if (dock != null) {
            builder.accept(Component.translatable("tooltip.cyberfarm.survey.linked", dock.toShortString()).withStyle(ChatFormatting.AQUA));
        } else {
            builder.accept(Component.translatable("tooltip.cyberfarm.survey.unlinked").withStyle(ChatFormatting.GRAY));
        }
        BlockPos corner = stack.get(ModDataComponents.SURVEY_CORNER.get());
        if (corner != null) {
            builder.accept(Component.translatable("tooltip.cyberfarm.survey.corner", corner.toShortString()).withStyle(ChatFormatting.DARK_AQUA));
        }
        builder.accept(Component.translatable("tooltip.cyberfarm.survey.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
