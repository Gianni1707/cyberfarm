package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.block.CrystallizerBlock;
import com.cyberfarm.block.MechHangarBlock;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.block.ReactorControllerBlock;
import com.cyberfarm.block.ResonanceCoreBlock;
import com.cyberfarm.block.TurbineControllerBlock;
import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.client.screen.ProjectorScreen;
import com.cyberfarm.multiblock.Blueprint;
import com.cyberfarm.multiblock.BuildPlan;
import com.cyberfarm.multiblock.Structure;
import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * Multiblock Projector, after Immersive Engineering's: shows the chosen structure as ghost blocks.
 * While held it follows the block you look at. Right-click opens the list of structures; sneak +
 * right-click a block locks the projection there (on a controller it lines up with it), sneak +
 * right-click the air unlocks it. Right-click a Drone Dock with a locked projection: its Builder
 * drones build it. The structure stands on the block you aim at. In creative, right-clicking a block
 * builds it instantly.
 */
public class ProjectorItem extends Item {
    public ProjectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** The chosen structure; a projector from before the schematics saved a multiblock's number instead. */
    public static Structure structure(ItemStack stack) {
        Identifier id = stack.get(ModDataComponents.PROJECTOR_STRUCTURE.get());
        return id != null ? Structure.byId(id).orElse(Blueprint.byOrdinal(0))
                : Blueprint.byOrdinal(stack.getOrDefault(ModDataComponents.PROJECTOR_BLUEPRINT.get(), 0));
    }

    public static @Nullable BlockPos anchor(ItemStack stack) {
        return stack.get(ModDataComponents.PROJECTOR_ANCHOR.get());
    }

    public static Direction anchorFacing(ItemStack stack) {
        return Direction.from2DDataValue(stack.getOrDefault(ModDataComponents.PROJECTOR_FACING.get(), 0));
    }

    public record Placement(BlockPos controller, Direction facing) {}

    /**
     * Where a projection goes when the player looks at {@code clicked}: a multiblock lines up with a
     * clicked controller; otherwise it stands on the face aimed at, its lowest layer there and its front
     * (a multiblock's controller) facing the player.
     */
    public static Placement placement(Level level, BlockPos clicked, Direction face, Player player, Structure structure) {
        BlockState state = level.getBlockState(clicked);
        if (structure instanceof Blueprint && isController(state)) {
            return new Placement(clicked, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        Direction facing = player.getDirection().getOpposite();
        int bottom = structure.ghosts(BlockPos.ZERO, facing).stream().mapToInt(ghost -> ghost.pos().getY()).min().orElse(0);
        return new Placement(clicked.relative(face).below(bottom), facing);
    }

    public static @Nullable Placement pinned(ItemStack stack) {
        BlockPos anchor = stack.get(ModDataComponents.PROJECTOR_ANCHOR.get());
        return anchor != null ? new Placement(anchor, anchorFacing(stack)) : null;
    }

    private static boolean isController(BlockState state) {
        return state.getBlock() instanceof ReactorControllerBlock || state.getBlock() instanceof TurbineControllerBlock
                || state.getBlock() instanceof MethaneExtractorBlock || state.getBlock() instanceof ResonanceCoreBlock
                || state.getBlock() instanceof MechHangarBlock || state.getBlock() instanceof CrystallizerBlock;
    }

    /** Right-click a Drone Dock (without sneaking): link the locked projection to it, or unlink with none locked. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSecondaryUseActive()
                || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof DroneDockBlockEntity dock)) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Placement pinned = pinned(stack);
        if (pinned == null) {
            boolean linked = dock.getBuildPlan() != null;
            dock.setBuildPlan(null);
            player.sendOverlayMessage(Component.translatable(linked ? "message.cyberfarm.projector.unlinked" : "message.cyberfarm.projector.lock_first"));
            return InteractionResult.SUCCESS;
        }
        if (!pinned.controller().closerThan(dock.getBlockPos(), SurveyToolItem.MAX_DOCK_DISTANCE)) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.survey.too_far", SurveyToolItem.MAX_DOCK_DISTANCE));
            return InteractionResult.FAIL;
        }
        BuildPlan plan = new BuildPlan(structure(stack), pinned.controller(), pinned.facing());
        dock.setBuildPlan(plan);
        // Vanilla only counts item uses that reach the block, and this one acts before: count it here
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, context.getClickedPos(), stack);
        }
        player.playSound(SoundEvents.BEACON_ACTIVATE, 0.4F, 1.6F);
        player.sendOverlayMessage(Component.translatable("message.cyberfarm.projector.linked", plan.structure().displayName(),
                plan.remaining(level), plan.obstacles(level)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        Structure structure = structure(stack);
        if (player.isSecondaryUseActive()) {
            Placement placement = placement(level, context.getClickedPos(), context.getClickedFace(), player, structure);
            stack.set(ModDataComponents.PROJECTOR_ANCHOR.get(), placement.controller());
            stack.set(ModDataComponents.PROJECTOR_FACING.get(), placement.facing().get2DDataValue());
            player.playSound(SoundEvents.BEACON_POWER_SELECT, 0.4F, 1.6F);
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.cyberfarm.projector.pinned"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isCreative()) {
            // Goes on to use(): the list of structures
            return InteractionResult.PASS;
        }
        Placement pinned = pinned(stack);
        Placement placement = pinned != null ? pinned : placement(level, context.getClickedPos(), context.getClickedFace(), player, structure);
        if (level instanceof ServerLevel serverLevel) {
            structure.buildInstantly(serverLevel, placement.controller(), placement.facing());
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.projector.built", structure.displayName()));
        }
        stack.remove(ModDataComponents.PROJECTOR_ANCHOR.get());
        player.playSound(SoundEvents.ANVIL_USE, 0.4F, 1.4F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!stack.has(ModDataComponents.PROJECTOR_ANCHOR.get())) {
                return InteractionResult.PASS;
            }
            stack.remove(ModDataComponents.PROJECTOR_ANCHOR.get());
            player.playSound(SoundEvents.BEACON_DEACTIVATE, 0.4F, 1.6F);
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            ProjectorScreen.open(hand, structure(stack));
        }
        return InteractionResult.SUCCESS;
    }

    /** The structure picked in the list: the projection follows the player's aim again. */
    public static void select(ItemStack stack, Structure structure) {
        stack.set(ModDataComponents.PROJECTOR_STRUCTURE.get(), structure.id());
        stack.remove(ModDataComponents.PROJECTOR_BLUEPRINT.get());
        stack.remove(ModDataComponents.PROJECTOR_ANCHOR.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(structure(stack).displayName().copy().withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.cyberfarm.projector").withStyle(ChatFormatting.GRAY));
    }
}
