package com.cyberfarm.item;

import net.minecraft.world.InteractionResultHolder;

import org.jspecify.annotations.Nullable;

import java.util.List;

import java.util.Locale;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import com.cyberfarm.port.Caps;

/**
 * Configures a Drone Dock. Sneak + mouse wheel (or right-click in the air) switches between its
 * modes, so a click only ever does what the current mode says:
 * <ul>
 *   <li><b>Dock</b>: right-click a dock to link the tool; sneak + right-click it to reset its area and route;</li>
 *   <li><b>Area</b>: right-click two opposite corners to set the work area;</li>
 *   <li><b>Route</b>: right-click two containers (source, then destination), other drones' docks too;</li>
 *   <li><b>Taxi</b>: right-click the block where passengers land.</li>
 * </ul>
 * Sneak + right-click in the air clears a half-set corner or route. The tool never opens or uses
 * the block it clicks.
 */
public class SurveyToolItem extends Item {
    public static final int MAX_AREA_SIZE = 33;
    /** Max height difference between the two corners; the drone scans the whole box often. */
    public static final int MAX_AREA_HEIGHT = 8;
    public static final int MAX_DOCK_DISTANCE = 48;
    /** Taxi destinations can be much further away than work areas. */
    public static final int MAX_TAXI_DISTANCE = 256;

    public enum Mode {
        DOCK, AREA, ROUTE, TAXI;

        public Component displayName() {
            return Component.translatable("survey_mode.cyberfarm." + name().toLowerCase(Locale.ROOT));
        }

        private String hintKey() {
            return "tooltip.cyberfarm.survey.hint_" + name().toLowerCase(Locale.ROOT);
        }
    }

    public SurveyToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static Mode mode(ItemStack stack) {
        Mode[] modes = Mode.values();
        return modes[Math.floorMod(ModDataComponents.SURVEY_MODE.getOrDefault(stack, 0), modes.length)];
    }

    /** Moves to the next ({@code step} 1) or previous (-1) mode and drops anything half-set. */
    public static void cycleMode(Player player, ItemStack stack, int step) {
        Mode[] modes = Mode.values();
        Mode next = modes[Math.floorMod(mode(stack).ordinal() + step, modes.length)];
        ModDataComponents.SURVEY_MODE.set(stack, next.ordinal());
        clearPending(stack);
        message(player, Component.translatable("message.cyberfarm.survey.mode", next.displayName(), nextStep(stack)));
    }

    /** What to click next in the current mode, shown above the hotbar after every step. */
    private static Component nextStep(ItemStack stack) {
        if (!ModDataComponents.LINKED_DOCK.has(stack)) {
            return step("dock");
        }
        return step(switch (mode(stack)) {
            case DOCK -> "pick_mode";
            case AREA -> ModDataComponents.SURVEY_CORNER.has(stack) ? "area_second" : "area_first";
            case ROUTE -> ModDataComponents.ROUTE_SOURCE.has(stack) ? "route_destination" : "route_source";
            case TAXI -> "taxi";
        });
    }

    private static Component step(String key) {
        return Component.translatable("message.cyberfarm.survey.step." + key);
    }

    /** Runs before the clicked block reacts, so chests and machines never open their GUI. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Mode mode = mode(stack);
        if (mode == Mode.DOCK) {
            return link(level, player, stack, pos);
        }

        BlockPos dockPos = ModDataComponents.LINKED_DOCK.get(stack);
        if (dockPos != null && !(level.getBlockEntity(dockPos) instanceof DroneDockBlockEntity)) {
            ModDataComponents.LINKED_DOCK.remove(stack);
            clearPending(stack);
            dockPos = null;
            if (!(level.getBlockEntity(pos) instanceof DroneDockBlockEntity)) {
                message(player, Component.translatable("message.cyberfarm.survey.dock_missing"));
                return InteractionResult.FAIL;
            }
        }
        // In any mode the first click picks the dock; the message then says what comes next
        if (dockPos == null || !(level.getBlockEntity(dockPos) instanceof DroneDockBlockEntity dock)) {
            return link(level, player, stack, pos);
        }
        if (mode == Mode.TAXI) {
            return setTaxiDestination(level, player, dockPos, dock, pos);
        }
        if (!pos.closerThan(dockPos, MAX_DOCK_DISTANCE)) {
            message(player, Component.translatable("message.cyberfarm.survey.too_far", MAX_DOCK_DISTANCE));
            return InteractionResult.FAIL;
        }
        if (mode == Mode.ROUTE && Caps.items(level, pos, null) == null) {
            message(player, Component.translatable("message.cyberfarm.survey.not_container"));
            return InteractionResult.FAIL;
        }

        InteractionResult result = mode == Mode.ROUTE ? setRoute(level, player, stack, dock, pos) : setArea(level, player, stack, dock, pos);
        if (result == InteractionResult.SUCCESS) {
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
        }
        return result;
    }

    private static InteractionResult link(ServerLevel level, Player player, ItemStack stack, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof DroneDockBlockEntity dock)) {
            // Already linked: they most likely want another mode, so say how to get there
            message(player, ModDataComponents.LINKED_DOCK.has(stack)
                    ? Component.translatable("message.cyberfarm.survey.wrong_mode") : step("dock"));
            return InteractionResult.FAIL;
        }
        clearPending(stack);
        ModDataComponents.LINKED_DOCK.set(stack, pos);
        if (player != null && player.isSecondaryUseActive()) {
            dock.setWorkArea(null);
            dock.setTransportRoute(null, null);
            dock.setTaxiDestination(null);
            message(player, Component.translatable("message.cyberfarm.survey.reset"));
        } else {
            message(player, Component.translatable("message.cyberfarm.survey.linked", pos.toShortString(), nextStep(stack)));
        }
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.6F);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult setArea(ServerLevel level, Player player, ItemStack stack, DroneDockBlockEntity dock, BlockPos pos) {
        BlockPos corner = ModDataComponents.SURVEY_CORNER.get(stack);
        if (corner == null) {
            ModDataComponents.SURVEY_CORNER.set(stack, pos);
            message(player, Component.translatable("message.cyberfarm.survey.corner", pos.toShortString()));
            return InteractionResult.SUCCESS;
        }

        ModDataComponents.SURVEY_CORNER.remove(stack);
        int width = Math.abs(pos.getX() - corner.getX()) + 1;
        int depth = Math.abs(pos.getZ() - corner.getZ()) + 1;
        if (width > MAX_AREA_SIZE || depth > MAX_AREA_SIZE || Math.abs(pos.getY() - corner.getY()) > MAX_AREA_HEIGHT) {
            message(player, Component.translatable("message.cyberfarm.survey.too_big", MAX_AREA_SIZE, MAX_AREA_SIZE, MAX_AREA_HEIGHT));
            return InteractionResult.FAIL;
        }

        // Covers both the farmland and the crops on it, whichever the player clicked.
        dock.setWorkArea(new BoundingBox(
                Math.min(pos.getX(), corner.getX()), Math.min(pos.getY(), corner.getY()) - 1, Math.min(pos.getZ(), corner.getZ()),
                Math.max(pos.getX(), corner.getX()), Math.max(pos.getY(), corner.getY()) + 2, Math.max(pos.getZ(), corner.getZ())));
        message(player, Component.translatable("message.cyberfarm.survey.area_set", width, depth));
        playSaved(level, pos);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult setRoute(ServerLevel level, Player player, ItemStack stack, DroneDockBlockEntity dock, BlockPos pos) {
        BlockPos source = ModDataComponents.ROUTE_SOURCE.get(stack);
        if (source == null) {
            ModDataComponents.ROUTE_SOURCE.set(stack, pos);
            message(player, Component.translatable("message.cyberfarm.survey.source", pos.toShortString()));
            return InteractionResult.SUCCESS;
        }
        if (source.equals(pos)) {
            message(player, Component.translatable("message.cyberfarm.survey.same_container"));
            return InteractionResult.FAIL;
        }

        ModDataComponents.ROUTE_SOURCE.remove(stack);
        dock.setTransportRoute(source, pos);
        message(player, Component.translatable("message.cyberfarm.survey.route_set", source.toShortString(), pos.toShortString()));
        playSaved(level, pos);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult setTaxiDestination(ServerLevel level, Player player, BlockPos dockPos, DroneDockBlockEntity dock, BlockPos pos) {
        if (!pos.closerThan(dockPos, MAX_TAXI_DISTANCE)) {
            message(player, Component.translatable("message.cyberfarm.survey.too_far", MAX_TAXI_DISTANCE));
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(pos.above()).isAir() || !level.getBlockState(pos.above(2)).isAir()) {
            message(player, Component.translatable("message.cyberfarm.survey.taxi_blocked"));
            return InteractionResult.FAIL;
        }
        dock.setTaxiDestination(pos);
        message(player, Component.translatable("message.cyberfarm.survey.taxi_set", pos.toShortString()));
        playSaved(level, pos);
        return InteractionResult.SUCCESS;
    }

    /** In the air: sneak clears a half-set corner or route, a plain click moves to the next mode (no wheel needed). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResult result = useItem(level, player, hand);
        return new InteractionResultHolder<>(result, player.getItemInHand(hand));
    }

    private InteractionResult useItem(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!ModDataComponents.SURVEY_CORNER.has(stack) && !ModDataComponents.ROUTE_SOURCE.has(stack)) {
                return InteractionResult.PASS;
            }
            clearPending(stack);
            if (!level.isClientSide()) {
                message(player, Component.translatable("message.cyberfarm.survey.cleared"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            cycleMode(player, stack, 1);
        }
        return InteractionResult.SUCCESS;
    }

    /** The chime of an area, route or destination saved on the dock. */
    private static void playSaved(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.8F);
    }

    private static void clearPending(ItemStack stack) {
        ModDataComponents.SURVEY_CORNER.remove(stack);
        ModDataComponents.ROUTE_SOURCE.remove(stack);
    }

    private static void message(Player player, Component text) {
        if (player != null) {
            player.displayClientMessage(text, true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Consumer<Component> builder = tooltip::add;
        Mode mode = mode(stack);
        builder.accept(Component.translatable("tooltip.cyberfarm.survey.mode", mode.displayName()).withStyle(ChatFormatting.YELLOW));
        BlockPos dock = ModDataComponents.LINKED_DOCK.get(stack);
        if (dock != null) {
            builder.accept(Component.translatable("tooltip.cyberfarm.survey.linked", dock.toShortString()).withStyle(ChatFormatting.AQUA));
        } else {
            builder.accept(Component.translatable("tooltip.cyberfarm.survey.unlinked").withStyle(ChatFormatting.GRAY));
        }
        BlockPos corner = ModDataComponents.SURVEY_CORNER.get(stack);
        if (corner != null) {
            builder.accept(Component.translatable("tooltip.cyberfarm.survey.corner", corner.toShortString()).withStyle(ChatFormatting.DARK_AQUA));
        }
        BlockPos source = ModDataComponents.ROUTE_SOURCE.get(stack);
        if (source != null) {
            builder.accept(Component.translatable("tooltip.cyberfarm.survey.source", source.toShortString()).withStyle(ChatFormatting.DARK_AQUA));
        }
        builder.accept(Component.translatable(mode.hintKey()).withStyle(ChatFormatting.DARK_GRAY));
        builder.accept(Component.translatable("tooltip.cyberfarm.survey.hint_mode").withStyle(ChatFormatting.DARK_GRAY));
    }
}
