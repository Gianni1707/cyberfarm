package com.cyberfarm.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import java.util.List;
import java.util.function.Consumer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.trinket.ChipProgram;
import com.cyberfarm.trinket.GravityBootsItem;
import com.cyberfarm.trinket.NeuralChipItem;
import com.cyberfarm.trinket.TrinketKeyPayload;
import com.cyberfarm.trinket.TrinketTicker;
import com.cyberfarm.trinket.TrinketType;
import com.cyberfarm.trinket.Trinkets;
import com.cyberfarm.trinket.UtilityBeltItem;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import com.cyberfarm.port.net.ClientPacketDistributor;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;

/**
 * The client side of the trinkets: their keys, the Gravity Boots holding the hover height, the chip's zoom, and the
 * Inventory / Trinkets tabs.
 */
public final class TrinketClient {
    private static final String CATEGORY = "key.category." + CyberFarm.MODID + ".trinkets";
    public static final KeyMapping HOVER = new KeyMapping("key.cyberfarm.hover_height", InputConstants.Type.KEYSYM, InputConstants.KEY_H, CATEGORY);
    public static final KeyMapping ZOOM = new KeyMapping("key.cyberfarm.zoom", InputConstants.Type.KEYSYM, InputConstants.KEY_Z, CATEGORY);
    public static final KeyMapping NIGHT_VISION = new KeyMapping("key.cyberfarm.night_vision", InputConstants.Type.KEYSYM, InputConstants.KEY_N, CATEGORY);
    public static final KeyMapping TAXI_CALL = new KeyMapping("key.cyberfarm.taxi_call", InputConstants.Type.KEYSYM, InputConstants.KEY_J, CATEGORY);
    public static final KeyMapping BELT = new KeyMapping("key.cyberfarm.utility_belt", InputConstants.Type.KEYSYM, InputConstants.KEY_B, CATEGORY);
    public static final KeyMapping DRONE = new KeyMapping("key.cyberfarm.pocket_drone", InputConstants.Type.KEYSYM, InputConstants.KEY_K, CATEGORY);
    public static final KeyMapping PROJECTOR = new KeyMapping("key.cyberfarm.belt_projector", InputConstants.Type.KEYSYM, InputConstants.KEY_Y, CATEGORY);
    public static final KeyMapping SURVEY_TOOL = new KeyMapping("key.cyberfarm.belt_survey_tool", InputConstants.Type.KEYSYM, InputConstants.KEY_U, CATEGORY);

    /** Below the lowest hover height, how far down the boots feel for ground before letting the player fall. */
    private static final double FIELD_REACH = 4.0;
    private static final double MIN_ZOOM = 2.0, MAX_ZOOM = 8.0;
    /** The zoom the wheel picked, and the one shown, easing towards it (1 = none). */
    private static double zoom = 4.0;
    private static double shownZoom = 1.0;
    /** How fast the shown zoom closes on the wanted one, per second; and when the last frame was. */
    private static final double ZOOM_SPEED = 14.0;
    private static long lastFrame;

    private TrinketClient() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(HOVER);
        event.register(ZOOM);
        event.register(NIGHT_VISION);
        event.register(TAXI_CALL);
        event.register(BELT);
        event.register(DRONE);
        event.register(PROJECTOR);
        event.register(SURVEY_TOOL);
    }

    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (event.phase != TickEvent.Phase.END || player == null) {
            return;
        }
        while (HOVER.consumeClick()) {
            if (Trinkets.worn(player, TrinketType.FEET, GravityBootsItem.class).isEmpty()) {
                player.displayClientMessage(Component.translatable("message.cyberfarm.trinket.missing", Component.translatable("item.cyberfarm.gravity_boots")), true);
            } else {
                ClientPacketDistributor.sendToServer(new TrinketKeyPayload(TrinketKeyPayload.Action.HOVER_HEIGHT));
            }
        }
        while (NIGHT_VISION.consumeClick()) {
            sendIfInstalled(player, ChipProgram.NIGHT_VISION, TrinketKeyPayload.Action.NIGHT_VISION);
        }
        while (TAXI_CALL.consumeClick()) {
            sendIfInstalled(player, ChipProgram.TAXI_CALL, TrinketKeyPayload.Action.TAXI_CALL);
        }
        while (BELT.consumeClick()) {
            sendIfOnBelt(player, -1, TrinketKeyPayload.Action.OPEN_BELT);
        }
        while (DRONE.consumeClick()) {
            sendIfOnBelt(player, UtilityBeltItem.DRONE, TrinketKeyPayload.Action.DRONE);
        }
        while (PROJECTOR.consumeClick()) {
            sendIfOnBelt(player, UtilityBeltItem.PROJECTOR, TrinketKeyPayload.Action.PROJECTOR);
        }
        while (SURVEY_TOOL.consumeClick()) {
            sendIfOnBelt(player, UtilityBeltItem.SURVEY_TOOL, TrinketKeyPayload.Action.SURVEY_TOOL);
        }
    }

    /** Needs the Utility Belt; for a tool key, also the tool on it or in hand to put back. */
    private static void sendIfOnBelt(Player player, int holder, TrinketKeyPayload.Action action) {
        if (UtilityBeltItem.worn(player).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.cyberfarm.trinket.missing", Component.translatable("item.cyberfarm.utility_belt")), true);
        } else if (holder >= 0 && UtilityBeltItem.content(player, holder).isEmpty() && !UtilityBeltItem.fits(holder, player.getMainHandItem())) {
            player.displayClientMessage(Component.translatable("message.cyberfarm.utility_belt.empty_holder"), true);
        } else {
            ClientPacketDistributor.sendToServer(new TrinketKeyPayload(action));
        }
    }

    private static void sendIfInstalled(Player player, ChipProgram program, TrinketKeyPayload.Action action) {
        if (NeuralChipItem.has(NeuralChipItem.worn(player), program)) {
            ClientPacketDistributor.sendToServer(new TrinketKeyPayload(action));
        } else {
            player.displayClientMessage(Component.translatable("message.cyberfarm.trinket.missing", Component.translatable(program.translationKey())), true);
        }
    }

    // ---------------------------------------------------------------- zoom

    private static boolean zooming(Player player) {
        return ZOOM.isDown() && Minecraft.getInstance().screen == null && NeuralChipItem.runs(player, ChipProgram.ZOOM);
    }

    /**
     * Eases the zoom shown towards the one wanted every frame, not every tick, so it glides at any frame rate; in
     * log steps, so 2x to 4x feels as long as 4x to 8x.
     */
    public static void zoomFov(ViewportEvent.ComputeFov event) {
        Player player = Minecraft.getInstance().player;
        long now = System.nanoTime();
        float seconds = lastFrame == 0 ? 0.0F : Math.min((now - lastFrame) / 1.0E9F, 0.25F);
        lastFrame = now;
        double wanted = player != null && zooming(player) ? zoom : 1.0;
        double step = 1.0 - Math.exp(-seconds * ZOOM_SPEED);
        shownZoom = Math.exp(Math.log(shownZoom) + (Math.log(wanted) - Math.log(shownZoom)) * step);
        if (shownZoom > 1.01) {
            event.setFOV((float) (event.getFOV() / shownZoom));
        }
    }

    /** The view's turn as the last frame left it, to scale the mouse's next turn from. */
    private static float lastYaw, lastPitch;
    private static boolean turnKnown;

    /**
     * Zoomed in, the mouse turns the view as much slower as it is zoomed, like the spyglass, so aiming stays steady.
     * 1.20.1 has no turn event: the mouse turns the player just before a frame starts, so that turn is scaled down
     * then, from where the last frame left the view.
     */
    public static void zoomTurn(TickEvent.RenderTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            turnKnown = false;
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            if (turnKnown && shownZoom > 1.01) {
                float keep = (float) (1.0 / shownZoom);
                float yaw = (player.getYRot() - lastYaw) * (1.0F - keep);
                float pitch = Mth.clamp(lastPitch + (player.getXRot() - lastPitch) * keep, -90.0F, 90.0F) - player.getXRot();
                player.setYRot(player.getYRot() - yaw);
                player.yRotO -= yaw;
                player.setXRot(player.getXRot() + pitch);
                player.xRotO += pitch;
            }
        } else {
            lastYaw = player.getYRot();
            lastPitch = player.getXRot();
            turnKnown = true;
        }
    }

    /** While zooming the wheel changes the zoom instead of the hotbar slot. */
    public static void zoomWheel(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && zooming(player) && event.getScrollDelta() != 0) {
            zoom = Mth.clamp(zoom * (event.getScrollDelta() > 0 ? 1.25 : 0.8), MIN_ZOOM, MAX_ZOOM);
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- Gravity Boots

    /**
     * Holds the local player at the boots' height over the ground (or a liquid). The server switched gravity off, so
     * this decides all the vertical motion: a spring up to the height, a fall that slows down as the ground comes, and
     * "on ground" near the height so walking, jumping and friction feel the same as on foot. Sneak lands.
     */
    public static void hover(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof LocalPlayer player)) {
            return;
        }
        // The server's modifier, synced with the player's attributes
        AttributeInstance gravity = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        ItemStack boots = Trinkets.worn(player, TrinketType.FEET, GravityBootsItem.class);
        if (gravity == null || gravity.getModifier(TrinketTicker.HOVER) == null || boots.isEmpty()) {
            return;
        }
        double height = player.isShiftKeyDown() ? 0.0 : GravityBootsItem.height(boots);
        double ground = groundBelow(player, GravityBootsItem.height(boots) + FIELD_REACH);
        Vec3 motion = player.getDeltaMovement();
        double vy = motion.y;
        if (Double.isNaN(ground)) {
            // Nothing under the field: fall as usual
            vy = (vy - 0.08) * 0.98;
        } else {
            double offset = ground + height - player.getY();
            if (offset > 0) {
                vy = Math.max(vy, Math.min(0.5, offset * 0.35 + 0.02));
            } else {
                // Above the height, after a jump or off a ledge: fall, braking as the height comes
                vy = Math.max(vy - 0.08, offset * 0.45 - 0.02);
            }
            if (Math.abs(offset) < 0.1 && vy <= 0.1) {
                vy = offset * 0.5;
                player.setOnGround(true);
            }
        }
        player.setDeltaMovement(motion.x, vy, motion.z);
        startX = motion.x;
        startZ = motion.z;
        sliding = height > 0;
    }

    /** Hovering, the field barely grips: a block's friction a little over the usual 0.6, like a touch of ice. */
    static final double SLIDE_FRICTION = 0.7;
    /** The horizontal motion the local player started this tick with, for {@link #slide(TickEvent.PlayerTickEvent)}. */
    private static double startX, startZ;
    private static boolean sliding;

    /**
     * After the tick's move: while hovering, a little of the motion the player had is kept, so they drift on a bit when
     * they stop or turn. Their top speed stays the same.
     */
    public static void slide(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof LocalPlayer player) || !sliding) {
            return;
        }
        sliding = false;
        if (player.onGround() && !player.isShiftKeyDown()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(slide(startX, motion.x), motion.y, slide(startZ, motion.z));
        }
    }

    /**
     * One axis of the move redone on a slicker block: from the speed at the tick's start and the one vanilla left
     * (pushed by the keys, then times 0.546 for friction), the same push scaled so the top speed is kept, times the
     * slicker friction. Hitting a wall (vanilla left 0) stops it too.
     */
    static double slide(double start, double after) {
        if (after == 0.0) {
            return 0.0;
        }
        double usual = 0.6 * 0.91, slick = SLIDE_FRICTION * 0.91;
        double push = after / usual - start;
        double scale = usual / (1 - usual) * (1 - slick) / slick;
        return (start + push * scale) * slick;
    }

    /**
     * The highest ground or liquid surface under the player's feet, within {@code reach} below them; NaN if none.
     * Every block column the player stands over counts, so the field rides over a ledge until the player is off it.
     */
    static double groundBelow(Player player, double reach) {
        Level level = player.level();
        AABB box = player.getBoundingBox();
        double top = player.getY() + 0.25;
        double best = Double.NaN;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX - 1.0E-7); x++) {
            for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ - 1.0E-7); z++) {
                for (int y = Mth.floor(top); y >= Mth.floor(player.getY() - reach); y--) {
                    pos.set(x, y, z);
                    double surface = surface(level, pos);
                    if (!Double.isNaN(surface)) {
                        if (surface <= top && !(surface <= best)) {
                            best = surface;
                        }
                        break;
                    }
                }
            }
        }
        return best;
    }

    private static double surface(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        double surface = Double.NaN;
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            surface = pos.getY() + shape.max(Direction.Axis.Y);
        }
        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty()) {
            double liquid = pos.getY() + fluid.getHeight(level, pos);
            surface = Double.isNaN(surface) ? liquid : Math.max(surface, liquid);
        }
        return surface;
    }

    // ---------------------------------------------------------------- the Inventory / Trinkets tabs

    /** The tabs on the inventory screens; with Curios the trinkets live in its own screen instead. */
    public static void addInventoryTabs(ScreenEvent.Init.Post event) {
        if (Trinkets.ownSlots() && (event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof CreativeModeInventoryScreen)) {
            addTabs((AbstractContainerScreen<?>) event.getScreen(), event::addListener, false);
        }
    }

    /** Where the two tabs stand down the creative inventory's right side, for JEI and REI to keep their lists off them. */
    public static List<Rect2i> creativeTabArea(Screen screen) {
        if (!Trinkets.ownSlots() || !(screen instanceof CreativeModeInventoryScreen creative) || !creative.isInventoryOpen()) {
            return List.of();
        }
        return List.of(new Rect2i(creative.getGuiLeft() + creative.getXSize() - 4, creative.getGuiTop() + 8, 32, 2 * 28));
    }

    /** Two tabs: the inventory and the trinkets, the open one drawn joined to the screen. */
    public static void addTabs(AbstractContainerScreen<?> screen, Consumer<AbstractWidget> adder, boolean trinketsOpen) {
        adder.accept(new Tab(screen, 0, new ItemStack(Items.CHEST), Component.translatable("gui.cyberfarm.tab.inventory"), !trinketsOpen, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.player.closeContainer();
            // In creative the inventory screen hands over to the creative one by itself
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        }));
        adder.accept(new Tab(screen, 1, new ItemStack(ModItems.GRAVITY_BOOTS.get()), Component.translatable("gui.cyberfarm.tab.trinkets"), trinketsOpen,
                () -> ClientPacketDistributor.sendToServer(new TrinketKeyPayload(TrinketKeyPayload.Action.OPEN_TRINKETS))));
    }

    /**
     * A tab with an item on it, the game's own creative-inventory tab: above the screen's top left, the open one joined to
     * the screen and the other just behind its edge. On the creative inventory, whose top has its own tabs, down its
     * right side in the same grey. Placed at every frame: the recipe book moves the screen.
     */
    private static final class Tab extends AbstractButton {
        private static final int WIDTH = 26, HEIGHT = 32, BEHIND = 28;
        private static final ResourceLocation TABS = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
        private final AbstractContainerScreen<?> screen;
        private final int index;
        private final ItemStack icon;
        private final boolean selected;
        private final Runnable action;

        Tab(AbstractContainerScreen<?> screen, int index, ItemStack icon, Component name, boolean selected, Runnable action) {
            super(0, 0, WIDTH, BEHIND, name);
            this.screen = screen;
            this.index = index;
            this.icon = icon;
            this.selected = selected;
            this.action = action;
            setTooltip(Tooltip.create(name));
        }

        @Override
        public void onPress() {
            if (!selected) {
                action.run();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
            // In creative only on the inventory's own page. Never through `visible`: a hidden widget is not drawn again,
            // so it could not come back when the inventory page is picked; out of the way and inactive instead
            active = !(screen instanceof CreativeModeInventoryScreen tabs) || tabs.isInventoryOpen();
            if (!active) {
                setX(-10_000);
                return;
            }
            if (screen instanceof CreativeModeInventoryScreen) {
                side(graphics);
                return;
            }
            int x = screen.getGuiLeft() + index * (WIDTH + 1);
            int y = screen.getGuiTop() - BEHIND;
            setX(x);
            setY(y);
            // The open tab overlaps the screen's edge; the other stops at it, as if behind (1.20.1: the tabs sheet)
            RenderSystem.enableBlend();
            graphics.blit(TABS, x, y, index * WIDTH, selected ? HEIGHT : 0, WIDTH, selected ? HEIGHT : BEHIND);
            graphics.renderItem(icon, x + 5, y + 9 + (selected ? 1 : 0));
        }

        /** Down the creative inventory's right side: the game's grey, its light and dark edges, joined when open. */
        private void side(GuiGraphics graphics) {
            int x = screen.getGuiLeft() + screen.getXSize() - (selected ? 4 : 1);
            int y = screen.getGuiTop() + 8 + index * (WIDTH + 2);
            setX(x);
            setY(y);
            int w = BEHIND + (selected ? 3 : 0), h = WIDTH;
            graphics.fill(x, y, x + w, y + h, 0xFF000000);
            graphics.fill(x, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
            graphics.fill(x, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
            graphics.fill(x, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
            graphics.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
            if (!selected) {
                graphics.fill(x, y + 1, x + 1, y + h - 1, 0xFF555555);
            }
            graphics.renderItem(icon, x + (w - 16) / 2, y + (h - 16) / 2);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
