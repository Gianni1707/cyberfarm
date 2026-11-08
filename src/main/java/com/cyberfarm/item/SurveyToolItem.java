package com.cyberfarm.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;

/**
 * Sets up a Drone Dock. Right-click a dock to link the tool: from then on it reads what the drone on
 * that dock does and asks for what that needs, above the hotbar for as long as it is in hand:
 * <ul>
 *   <li><b>Area</b>: two opposite corners, for every drone that works an area;</li>
 *   <li><b>Route</b>: two containers (source, then destination), other drones' docks too, for a Transport drone;</li>
 *   <li><b>Taxi</b>: the block where passengers land, for a Taxi drone.</li>
 * </ul>
 * A drone with several modules may need more than one: the tool moves on by itself, and sneak + mouse
 * wheel (or right-click in the air) switches between them. The linked dock clicked twice in a row is a route's source. Sneak + right-click a dock links that one,
 * or resets the linked one; sneak + right-click in the air clears a half-set corner or route. The tool
 * never opens or uses the block it clicks.
 */
public class SurveyToolItem extends Item {
    public static final int MAX_AREA_SIZE = 33;
    /** Max height difference between the two corners; the drone scans the whole box often. */
    public static final int MAX_AREA_HEIGHT = 8;
    public static final int MAX_DOCK_DISTANCE = 48;
    /** Taxi destinations can be much further away than work areas. */
    public static final int MAX_TAXI_DISTANCE = 256;
    /** While in hand it repeats what to do next, once what it last said has had time to be read. */
    private static final int HINT_TICKS = 60;

    public enum Mode {
        AREA, ROUTE, TAXI;

        public Component displayName() {
            return Component.translatable("survey_mode.cyberfarm." + name().toLowerCase(Locale.ROOT));
        }
    }

    // shortcut: in memory, only delays a hint; a restart just shows the next one sooner
    private static final Map<Player, Long> LAST_MESSAGE = new WeakHashMap<>();
    // shortcut: in memory, a restart between two clicks just takes one more click
    private static final Map<Player, BlockPos> DOCK_CLICKED = new WeakHashMap<>();

    public SurveyToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** What the drone on this dock needs set up, in the order the tool asks for it. */
    private static List<Mode> needs(DroneDockBlockEntity dock) {
        int tasks = dock.droneTasks();
        List<Mode> needs = new ArrayList<>();
        if ((tasks & ~DroneTask.NO_AREA_TASKS) != 0) {
            needs.add(Mode.AREA);
        }
        if ((tasks & DroneTask.TRANSPORT.bit()) != 0) {
            needs.add(Mode.ROUTE);
        }
        if ((tasks & DroneTask.TAXI.bit()) != 0) {
            needs.add(Mode.TAXI);
        }
        return needs;
    }

    private static boolean isSet(DroneDockBlockEntity dock, Mode mode) {
        return switch (mode) {
            case AREA -> dock.hasCustomArea();
            case ROUTE -> dock.getTransportSource() != null && dock.getTransportDestination() != null;
            case TAXI -> dock.getTaxiDestination() != null;
        };
    }

    /** The one the tool is asking for: the one last picked while it is still needed, else the first not set yet. */
    private static @Nullable Mode current(ItemStack stack, DroneDockBlockEntity dock) {
        List<Mode> needs = needs(dock);
        Mode[] modes = Mode.values();
        Integer picked = stack.get(ModDataComponents.SURVEY_MODE.get());
        if (picked != null && needs.contains(modes[Math.floorMod(picked, modes.length)])) {
            return modes[Math.floorMod(picked, modes.length)];
        }
        return needs.stream().filter(mode -> !isSet(dock, mode)).findFirst().orElse(needs.isEmpty() ? null : needs.getFirst());
    }

    /** After saving {@code done}: on to the next thing not set yet, if any. */
    private static void moveOn(ItemStack stack, DroneDockBlockEntity dock) {
        needs(dock).stream().filter(mode -> !isSet(dock, mode)).findFirst().ifPresent(mode -> stack.set(ModDataComponents.SURVEY_MODE.get(), mode.ordinal()));
    }

    /** Moves to the next ({@code step} 1) or previous (-1) of what this drone needs and drops anything half-set. */
    public static void cycleMode(Player player, ItemStack stack, int step) {
        DroneDockBlockEntity dock = linkedDock(player.level(), stack);
        if (dock != null) {
            List<Mode> needs = needs(dock);
            Mode mode = current(stack, dock);
            if (needs.size() > 1) {
                stack.set(ModDataComponents.SURVEY_MODE.get(), needs.get(Math.floorMod(needs.indexOf(mode) + step, needs.size())).ordinal());
                clearPending(stack);
            }
        }
        message(player, nextStep(player.level(), stack));
    }

    private static @Nullable DroneDockBlockEntity linkedDock(Level level, ItemStack stack) {
        BlockPos pos = stack.get(ModDataComponents.LINKED_DOCK.get());
        return pos != null && level.getBlockEntity(pos) instanceof DroneDockBlockEntity dock ? dock : null;
    }

    /** What to do next, shown above the hotbar: which drone this is, and what it wants clicked. */
    private static Component nextStep(Level level, ItemStack stack) {
        DroneDockBlockEntity dock = linkedDock(level, stack);
        if (dock == null) {
            return step("dock");
        }
        if (dock.droneTasks() == 0) {
            return step("no_drone");
        }
        Mode mode = current(stack, dock);
        Component what = mode == null ? step("builder") : step(switch (mode) {
            case AREA -> stack.has(ModDataComponents.SURVEY_CORNER.get()) ? "area_second" : dock.hasCustomArea() ? "area_change" : "area_first";
            case ROUTE -> stack.has(ModDataComponents.ROUTE_SOURCE.get()) ? "route_destination" : isSet(dock, mode) ? "route_change" : "route_source";
            case TAXI -> dock.getTaxiDestination() != null ? "taxi_change" : "taxi";
        });
        int needed = needs(dock).size();
        return Component.translatable("message.cyberfarm.survey.for", droneName(dock), needed > 1
                ? Component.translatable("message.cyberfarm.survey.of", what, needs(dock).indexOf(mode) + 1, needed) : what);
    }

    /** The modules of the drone on {@code dock}: "Harvester + Transport". */
    private static Component droneName(DroneDockBlockEntity dock) {
        Component name = null;
        for (DroneTask task : DroneTask.values()) {
            if ((dock.droneTasks() & task.bit()) != 0) {
                Component module = Component.translatable("gui.cyberfarm.dock.module." + task.name().toLowerCase(Locale.ROOT));
                name = name == null ? module : Component.empty().append(name).append(" + ").append(module);
            }
        }
        return name != null ? name : Component.empty();
    }

    private static Component step(String key) {
        return Component.translatable("message.cyberfarm.survey.step." + key);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if ((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && owner instanceof Player player
                && level.getGameTime() - LAST_MESSAGE.getOrDefault(player, 0L) >= HINT_TICKS) {
            message(player, nextStep(level, stack));
        }
    }

    /** Runs before the clicked block reacts, so chests and machines never open their GUI. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        boolean clickedDock = level.getBlockEntity(pos) instanceof DroneDockBlockEntity;
        BlockPos lastClicked = DOCK_CLICKED.remove(player);
        if (stack.has(ModDataComponents.LINKED_DOCK.get()) && linkedDock(level, stack) == null) {
            stack.remove(ModDataComponents.LINKED_DOCK.get());
            clearPending(stack);
            if (!clickedDock) {
                message(player, Component.translatable("message.cyberfarm.survey.dock_missing"));
                return InteractionResult.FAIL;
            }
        }
        DroneDockBlockEntity dock = linkedDock(level, stack);
        if (dock == null || clickedDock && player != null && player.isSecondaryUseActive()) {
            return link(level, player, stack, pos);
        }
        // One click on the linked dock only asks what comes next, so starting over never picks it by mistake;
        // two in a row take it as a route's source
        if (clickedDock && pos.equals(dock.getBlockPos()) && !stack.has(ModDataComponents.SURVEY_CORNER.get()) && !stack.has(ModDataComponents.ROUTE_SOURCE.get())
                && !(pos.equals(lastClicked) && current(stack, dock) == Mode.ROUTE)) {
            DOCK_CLICKED.put(player, pos);
            return refuse(player, nextStep(level, stack));
        }
        Mode mode = current(stack, dock);
        // Another dock is a fair end for a route and a landing spot, but as a first corner it means "this dock instead"
        if (mode == null || clickedDock && mode == Mode.AREA && !stack.has(ModDataComponents.SURVEY_CORNER.get())) {
            return clickedDock ? link(level, player, stack, pos) : refuse(player, nextStep(level, stack));
        }
        BlockPos dockPos = dock.getBlockPos();
        if (mode == Mode.TAXI) {
            return setTaxiDestination(level, player, stack, dockPos, dock, pos);
        }
        if (!pos.closerThan(dockPos, MAX_DOCK_DISTANCE)) {
            return refuse(player, Component.translatable("message.cyberfarm.survey.too_far", MAX_DOCK_DISTANCE));
        }
        // With a Tank Module the drone carries liquids too, so a tank is a fair end
        if (mode == Mode.ROUTE && level.getCapability(Capabilities.Item.BLOCK, pos, null) == null
                && ((dock.droneTasks() & DroneTask.TANK.bit()) == 0 || level.getCapability(Capabilities.Fluid.BLOCK, pos, null) == null)) {
            return refuse(player, Component.translatable("message.cyberfarm.survey.not_container"));
        }
        InteractionResult result = mode == Mode.ROUTE ? setRoute(level, player, stack, dock, pos) : setArea(level, player, stack, dock, pos);
        if (result == InteractionResult.SUCCESS) {
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
        }
        return result;
    }

    private static InteractionResult refuse(@Nullable Player player, Component why) {
        message(player, why);
        return InteractionResult.FAIL;
    }

    private static InteractionResult link(ServerLevel level, @Nullable Player player, ItemStack stack, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof DroneDockBlockEntity dock)) {
            return refuse(player, step("dock"));
        }
        boolean again = pos.equals(stack.get(ModDataComponents.LINKED_DOCK.get()));
        clearPending(stack);
        stack.set(ModDataComponents.LINKED_DOCK.get(), pos);
        if (again && player != null && player.isSecondaryUseActive()) {
            dock.setWorkArea(null);
            dock.setTransportRoute(null, null);
            dock.setTaxiDestination(null);
            stack.remove(ModDataComponents.SURVEY_MODE.get());
            message(player, Component.translatable("message.cyberfarm.survey.reset"));
        } else {
            stack.remove(ModDataComponents.SURVEY_MODE.get());
            DOCK_CLICKED.put(player, pos);
            message(player, again ? nextStep(level, stack) : Component.translatable("message.cyberfarm.survey.linked", pos.toShortString(), nextStep(level, stack)));
        }
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.6F);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult setArea(ServerLevel level, @Nullable Player player, ItemStack stack, DroneDockBlockEntity dock, BlockPos pos) {
        BlockPos corner = stack.get(ModDataComponents.SURVEY_CORNER.get());
        if (corner == null) {
            stack.set(ModDataComponents.SURVEY_CORNER.get(), pos);
            message(player, Component.translatable("message.cyberfarm.survey.corner", pos.toShortString()));
            return InteractionResult.SUCCESS;
        }

        stack.remove(ModDataComponents.SURVEY_CORNER.get());
        int width = Math.abs(pos.getX() - corner.getX()) + 1;
        int depth = Math.abs(pos.getZ() - corner.getZ()) + 1;
        if (width > MAX_AREA_SIZE || depth > MAX_AREA_SIZE || Math.abs(pos.getY() - corner.getY()) > MAX_AREA_HEIGHT) {
            return refuse(player, Component.translatable("message.cyberfarm.survey.too_big", MAX_AREA_SIZE, MAX_AREA_SIZE, MAX_AREA_HEIGHT));
        }

        // Covers both the farmland and the crops on it, whichever the player clicked.
        dock.setWorkArea(new BoundingBox(
                Math.min(pos.getX(), corner.getX()), Math.min(pos.getY(), corner.getY()) - 1, Math.min(pos.getZ(), corner.getZ()),
                Math.max(pos.getX(), corner.getX()), Math.max(pos.getY(), corner.getY()) + 2, Math.max(pos.getZ(), corner.getZ())));
        saved(level, player, stack, dock, pos, Component.translatable("message.cyberfarm.survey.area_set", width, depth));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult setRoute(ServerLevel level, @Nullable Player player, ItemStack stack, DroneDockBlockEntity dock, BlockPos pos) {
        BlockPos source = stack.get(ModDataComponents.ROUTE_SOURCE.get());
        if (source == null) {
            stack.set(ModDataComponents.ROUTE_SOURCE.get(), pos);
            message(player, Component.translatable("message.cyberfarm.survey.source", pos.toShortString()));
            return InteractionResult.SUCCESS;
        }
        if (source.equals(pos)) {
            return refuse(player, Component.translatable("message.cyberfarm.survey.same_container"));
        }

        stack.remove(ModDataComponents.ROUTE_SOURCE.get());
        dock.setTransportRoute(source, pos);
        saved(level, player, stack, dock, pos, Component.translatable("message.cyberfarm.survey.route_set", source.toShortString(), pos.toShortString()));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult setTaxiDestination(ServerLevel level, @Nullable Player player, ItemStack stack, BlockPos dockPos, DroneDockBlockEntity dock, BlockPos pos) {
        if (!pos.closerThan(dockPos, MAX_TAXI_DISTANCE)) {
            return refuse(player, Component.translatable("message.cyberfarm.survey.too_far", MAX_TAXI_DISTANCE));
        }
        if (!level.getBlockState(pos.above()).isAir() || !level.getBlockState(pos.above(2)).isAir()) {
            return refuse(player, Component.translatable("message.cyberfarm.survey.taxi_blocked"));
        }
        dock.setTaxiDestination(pos);
        saved(level, player, stack, dock, pos, Component.translatable("message.cyberfarm.survey.taxi_set", pos.toShortString()));
        return InteractionResult.SUCCESS;
    }

    /** Something was saved on the dock: the chime, what it was, and the next thing the drone needs if any. */
    private static void saved(ServerLevel level, @Nullable Player player, ItemStack stack, DroneDockBlockEntity dock, BlockPos pos, Component what) {
        moveOn(stack, dock);
        boolean more = needs(dock).stream().anyMatch(mode -> !isSet(dock, mode));
        message(player, more ? Component.translatable("message.cyberfarm.survey.then", what, nextStep(level, stack)) : what);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.8F);
    }

    /** In the air: sneak clears a half-set corner or route, a plain click moves to the next thing the drone needs (no wheel needed). */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!stack.has(ModDataComponents.SURVEY_CORNER.get()) && !stack.has(ModDataComponents.ROUTE_SOURCE.get())) {
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

    private static void clearPending(ItemStack stack) {
        stack.remove(ModDataComponents.SURVEY_CORNER.get());
        stack.remove(ModDataComponents.ROUTE_SOURCE.get());
    }

    private static void message(@Nullable Player player, Component text) {
        if (player != null) {
            player.sendOverlayMessage(text);
            LAST_MESSAGE.put(player, player.level().getGameTime());
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
        BlockPos source = stack.get(ModDataComponents.ROUTE_SOURCE.get());
        if (source != null) {
            builder.accept(Component.translatable("tooltip.cyberfarm.survey.source", source.toShortString()).withStyle(ChatFormatting.DARK_AQUA));
        }
        builder.accept(Component.translatable("tooltip.cyberfarm.survey.hint").withStyle(ChatFormatting.DARK_GRAY));
        builder.accept(Component.translatable("tooltip.cyberfarm.survey.hint_mode").withStyle(ChatFormatting.DARK_GRAY));
    }
}
