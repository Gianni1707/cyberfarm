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
import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    public static Blueprint blueprint(ItemStack stack) {
        return Blueprint.byOrdinal(stack.getOrDefault(ModDataComponents.PROJECTOR_BLUEPRINT.get(), 0));
    }

    public static @Nullable BlockPos anchor(ItemStack stack) {
        return stack.get(ModDataComponents.PROJECTOR_ANCHOR.get());
    }

    public static Direction anchorFacing(ItemStack stack) {
        return Direction.from2DDataValue(stack.getOrDefault(ModDataComponents.PROJECTOR_FACING.get(), 0));
    }

    public record Placement(BlockPos controller, Direction facing) {}

    /**
     * Where a projection goes when the player looks at {@code clicked}: lined up with a clicked controller,
     * otherwise standing on the face aimed at, its lowest layer there and the controller facing the player.
     */
    public static Placement placement(Level level, BlockPos clicked, Direction face, Player player, Blueprint blueprint) {
        BlockState state = level.getBlockState(clicked);
        if (isController(state)) {
            return new Placement(clicked, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        Direction facing = player.getDirection().getOpposite();
        int bottom = blueprint.ghosts(BlockPos.ZERO, facing).stream().mapToInt(ghost -> ghost.pos().getY()).min().orElse(0);
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
        BuildPlan plan = new BuildPlan(blueprint(stack), pinned.controller(), pinned.facing());
        dock.setBuildPlan(plan);
        player.playSound(SoundEvents.BEACON_ACTIVATE, 0.4F, 1.6F);
        player.sendOverlayMessage(Component.translatable("message.cyberfarm.projector.linked", name(plan.blueprint()),
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
        Blueprint blueprint = blueprint(stack);
        if (player.isSecondaryUseActive()) {
            Placement placement = placement(level, context.getClickedPos(), context.getClickedFace(), player, blueprint);
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
        Placement placement = pinned != null ? pinned : placement(level, context.getClickedPos(), context.getClickedFace(), player, blueprint);
        if (!level.isClientSide()) {
            build(level, blueprint, placement);
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.projector.built", name(blueprint)));
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
            ProjectorScreen.open(hand, blueprint(stack));
        }
        return InteractionResult.SUCCESS;
    }

    /** The structure picked in the list: the projection follows the player's aim again. */
    public static void select(ItemStack stack, Blueprint blueprint) {
        stack.set(ModDataComponents.PROJECTOR_BLUEPRINT.get(), blueprint.ordinal());
        stack.remove(ModDataComponents.PROJECTOR_ANCHOR.get());
    }

    /** Creative only: places every block of the structure and clears the spaces that must be empty. */
    private static void build(Level level, Blueprint blueprint, Placement placement) {
        for (Blueprint.Ghost ghost : blueprint.ghosts(placement.controller(), placement.facing())) {
            if (ghost.ok().test(level.getBlockState(ghost.pos()))) {
                continue;
            }
            BlockState state = ghost.show() != null ? Blueprint.placedState(ghost.show(), placement.facing()) : Blocks.AIR.defaultBlockState();
            level.setBlock(ghost.pos(), state, Block.UPDATE_ALL);
        }
    }

    public static Component name(Blueprint blueprint) {
        Component name = Component.translatable(blueprint.key());
        return blueprint.sizeKey() == null ? name
                : Component.translatable("message.cyberfarm.projector.sized", name, Component.translatable(blueprint.sizeKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(name(blueprint(stack)).copy().withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.cyberfarm.projector").withStyle(ChatFormatting.GRAY));
    }
}
