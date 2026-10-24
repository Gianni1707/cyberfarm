package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.block.CrystallizerBlock;
import com.cyberfarm.block.MechHangarBlock;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.block.ReactorControllerBlock;
import com.cyberfarm.block.ResonanceCoreBlock;
import com.cyberfarm.block.TurbineControllerBlock;
import com.cyberfarm.multiblock.Blueprint;
import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
 * While held it follows the block you look at; right-click a block to pin it there (on a controller,
 * it lines up with it), right-click the air to unpin, sneak + right-click to pick the next structure.
 * The structure stands on the block you aim at. In creative, right-clicking builds it instantly.
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

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        Blueprint blueprint = blueprint(stack);
        Placement pinned = pinned(stack);
        Placement placement = pinned != null && player.isCreative() ? pinned
                : placement(level, context.getClickedPos(), context.getClickedFace(), player, blueprint);
        if (player.isCreative()) {
            if (!level.isClientSide()) {
                build(level, blueprint, placement);
                player.sendOverlayMessage(Component.translatable("message.cyberfarm.projector.built", name(blueprint)));
            }
            stack.remove(ModDataComponents.PROJECTOR_ANCHOR.get());
            player.playSound(SoundEvents.ANVIL_USE, 0.4F, 1.4F);
            return InteractionResult.SUCCESS;
        }
        stack.set(ModDataComponents.PROJECTOR_ANCHOR.get(), placement.controller());
        stack.set(ModDataComponents.PROJECTOR_FACING.get(), placement.facing().get2DDataValue());
        player.playSound(SoundEvents.BEACON_POWER_SELECT, 0.4F, 1.6F);
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.projector.pinned"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            Blueprint next = blueprint(stack).next();
            stack.set(ModDataComponents.PROJECTOR_BLUEPRINT.get(), next.ordinal());
            stack.remove(ModDataComponents.PROJECTOR_ANCHOR.get());
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, 1.4F);
            if (!level.isClientSide()) {
                player.sendOverlayMessage(name(next));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.has(ModDataComponents.PROJECTOR_ANCHOR.get())) {
            stack.remove(ModDataComponents.PROJECTOR_ANCHOR.get());
            player.playSound(SoundEvents.BEACON_DEACTIVATE, 0.4F, 1.6F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Creative only: places every block of the structure and clears the spaces that must be empty. */
    private static void build(Level level, Blueprint blueprint, Placement placement) {
        for (Blueprint.Ghost ghost : blueprint.ghosts(placement.controller(), placement.facing())) {
            if (ghost.ok().test(level.getBlockState(ghost.pos()))) {
                continue;
            }
            BlockState state = ghost.show() != null ? ghost.show().defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, placement.facing());
            }
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
