package com.cyberfarm.client;

import java.util.Set;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.entity.MechEntity;
import com.cyberfarm.entity.MechExitPayload;
import com.cyberfarm.entity.MechModule;
import com.cyberfarm.entity.MechTogglePayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Piloting a mech on the client: the exit key, the jump key, hiding the pilot and the vanilla bars,
 * and the cockpit HUD drawn over the view: corner brackets, a compass, energy, integrity, speed and
 * altitude, with a boot sequence each time the cockpit closes.
 */
public final class MechClient {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(CyberFarm.id("mech"));
    public static final KeyMapping EXIT = new KeyMapping("key.cyberfarm.mech_exit", InputConstants.Type.KEYSYM, InputConstants.KEY_V, CATEGORY);
    public static final KeyMapping DRILL = new KeyMapping("key.cyberfarm.mech_drill", InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY);
    public static final KeyMapping BLADE = new KeyMapping("key.cyberfarm.mech_blade", InputConstants.Type.KEYSYM, InputConstants.KEY_G, CATEGORY);
    private static final Identifier HUD = CyberFarm.id("mech_hud");
    /** Vanilla HUD parts the cockpit replaces. */
    private static final Set<Identifier> HIDDEN = Set.of(VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH, VanillaGuiLayers.AIR_LEVEL, VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR, VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND);
    private static final int BOOT_TICKS = 40;

    private static final int CYAN = 0xFF2FE0FF;
    private static final int CYAN_DIM = 0x802FE0FF;
    private static final int CYAN_FAINT = 0x302FE0FF;
    private static final int WARN = 0xFFFF5040;
    private static final int TEXT = 0xFFD8F8FF;
    private static final String[] HEADINGS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    /** Ticks since the cockpit last closed around the pilot, for the boot sequence. */
    private static int bootTicks;
    private static boolean wasPiloting;
    private static boolean wasAboard;
    /** The third-person camera's distance, eased towards the one the game asks for. */
    private static float cameraDistance = -1.0F;

    private MechClient() {}

    /** The mech the local player sits in with the cockpit shut, or null. */
    private static @Nullable MechEntity piloted() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getVehicle() instanceof MechEntity mech && mech.getState() == MechEntity.State.PILOTED ? mech : null;
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(EXIT);
        event.register(DRILL);
        event.register(BLADE);
    }

    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(HUD, MechClient::renderHud);
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        MechEntity mech = piloted();
        MechEntity.clientJump = mech != null && minecraft.options.keyJump.isDown();
        MechEntity.clientSneak = mech != null && minecraft.options.keyShift.isDown();
        while (DRILL.consumeClick()) {
            if (mech != null) {
                ClientPacketDistributor.sendToServer(new MechTogglePayload(MechModule.DRILL.ordinal()));
            }
        }
        while (BLADE.consumeClick()) {
            if (mech != null) {
                ClientPacketDistributor.sendToServer(new MechTogglePayload(MechModule.BLADE.ordinal()));
            }
        }
        while (EXIT.consumeClick()) {
            if (minecraft.player != null && minecraft.player.getVehicle() instanceof MechEntity) {
                ClientPacketDistributor.sendToServer(MechExitPayload.INSTANCE);
            }
        }
        if (mech != null && !wasPiloting) {
            bootTicks = 0;
            minecraft.gui.setOverlayMessage(Component.translatable("hud.cyberfarm.mech.exit_hint", EXIT.getTranslatedKeyMessage()), false);
        }
        wasPiloting = mech != null;
        // The game says to sneak to get off; in a mech that does nothing, so the line is replaced
        boolean aboard = minecraft.player != null && minecraft.player.getVehicle() instanceof MechEntity;
        if (aboard && !wasAboard && mech == null) {
            minecraft.gui.setOverlayMessage(Component.translatable("hud.cyberfarm.mech.boarding"), false);
        }
        wasAboard = aboard;
        if (mech != null) {
            bootTicks++;
        }
    }

    /**
     * The third-person camera moves out to the mech's distance when you board and back in when you
     * get out: eased over a moment instead of jumping.
     */
    public static void easeCamera(CalculateDetachedCameraDistanceEvent event) {
        float target = event.getDistance();
        if (cameraDistance < 0.0F) {
            cameraDistance = target;
        }
        cameraDistance += (target - cameraDistance) * 0.06F;
        if (Math.abs(target - cameraDistance) < 0.01F) {
            cameraDistance = target;
        }
        event.setDistance(cameraDistance);
    }

    /** The cockpit's display replaces hearts, hunger, armour, air and the experience bar. */
    public static void hideVanillaHud(RenderGuiLayerEvent.Pre event) {
        if (HIDDEN.contains(event.getName()) && piloted() != null) {
            event.setCanceled(true);
        }
    }

    /** No arm in front of the view: the pilot's hands are on the controls. */
    public static void hideHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof MechEntity) {
            event.setCanceled(true);
        }
    }

    /**
     * Render state modifier for players: shut in the mech the pilot is hidden. On their way in or out
     * they stand rather than sit as riders do, face the way the boarding sequence turns them and walk.
     */
    public static void hidePilot(Entity avatar, AvatarRenderState state) {
        if (!(avatar.getVehicle() instanceof MechEntity mech)) {
            return;
        }
        float progress = mech.stateProgress(state.partialTick);
        if (mech.isPilotVisible(progress)) {
            state.isPassenger = false;
            state.bodyRot = mech.getYRot() + mech.pilotFacing(progress);
            state.yRot = 0.0F;
            if (mech.isPilotWalking(progress)) {
                state.walkAnimationPos = (mech.tickCount + state.partialTick) * 0.6F;
                state.walkAnimationSpeed = 0.7F;
            }
        } else {
            state.isInvisible = true;
            state.isInvisibleToPlayer = true;
        }
    }

    // ------------------------------------------------------------------ HUD

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        MechEntity mech = piloted();
        if (mech == null || minecraft.options.hideGui) {
            return;
        }
        Font font = minecraft.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float boot = Mth.clamp((bootTicks + partial) / BOOT_TICKS, 0.0F, 1.0F);
        int time = (int) (minecraft.level != null ? minecraft.level.getGameTime() : 0);

        corners(graphics, width, height, boot);
        if (boot < 1.0F) {
            // Boot: a scan line sweeps down and the systems report in
            int scan = (int) (height * boot);
            graphics.fill(0, scan, width, scan + 1, CYAN_DIM);
            graphics.fill(0, scan - 6, width, scan, CYAN_FAINT);
            Component online = Component.translatable("hud.cyberfarm.mech.boot");
            graphics.text(font, online, (width - font.width(online)) / 2, height / 2 - 30, (time / 3) % 2 == 0 ? CYAN : CYAN_DIM, true);
            if (boot < 0.5F) {
                return;
            }
        }
        compass(graphics, font, width, mech.getYRot());
        reticle(graphics, width / 2, height / 2);

        // Left: energy and integrity, just above the hotbar
        int left = 12;
        int bottom = height - 48;
        float energy = (float) mech.getEnergy() / MechEntity.CAPACITY;
        float integrity = mech.getHealth() / mech.getMaxHealth();
        bar(graphics, font, left, bottom - 22, Component.translatable("hud.cyberfarm.mech.energy"), energy,
                energy < 0.1F ? WARN : CYAN, String.format("%,d FE", mech.getEnergy()));
        bar(graphics, font, left, bottom, Component.translatable("hud.cyberfarm.mech.integrity"), integrity,
                integrity < 0.3F ? WARN : 0xFF6CFF8C, Math.round(integrity * 100) + "%");
        if (energy < 0.1F && (time / 10) % 2 == 0) {
            Component low = Component.translatable(mech.getEnergy() <= 0 ? "hud.cyberfarm.mech.no_power" : "hud.cyberfarm.mech.low_power");
            graphics.text(font, low, (width - font.width(low)) / 2, height / 2 + 24, WARN, true);
        }

        // Right: speed and altitude, then the modules
        double speed = mech.getDeltaMovement().horizontalDistance() * 20.0;
        int right = width - 12;
        readout(graphics, font, right, bottom - 22, Component.translatable("hud.cyberfarm.mech.speed"), String.format("%.1f m/s", speed));
        readout(graphics, font, right, bottom - 11, Component.translatable("hud.cyberfarm.mech.altitude"), String.valueOf(Mth.floor(mech.getY())));
        boolean any = false;
        int line = bottom - 34;
        for (MechModule module : MechModule.values()) {
            if (!mech.hasModule(module)) {
                continue;
            }
            any = true;
            String state = switch (module) {
                case JET -> Component.translatable(mech.onGround() ? "hud.cyberfarm.mech.jet.ready" : "hud.cyberfarm.mech.jet.flying").getString();
                case DRILL -> Component.translatable(mech.isToolOn(module) ? "hud.cyberfarm.mech.on" : "hud.cyberfarm.mech.off").getString()
                        + " [" + DRILL.getTranslatedKeyMessage().getString() + "]";
                case BLADE -> Component.translatable(mech.isToolOn(module) ? "hud.cyberfarm.mech.on" : "hud.cyberfarm.mech.off").getString()
                        + " [" + BLADE.getTranslatedKeyMessage().getString() + "]";
                case ARMOR -> Component.translatable("hud.cyberfarm.mech.armor.plated").getString();
            };
            readout(graphics, font, right, line, Component.translatable(module.hudKey()), state);
            line -= 11;
        }
        if (!any) {
            readout(graphics, font, right, bottom - 34, Component.translatable("hud.cyberfarm.mech.modules"),
                    Component.translatable("hud.cyberfarm.mech.modules.none").getString());
        }
    }

    /** Brackets in the four corners, growing out as the systems boot. */
    private static void corners(GuiGraphicsExtractor graphics, int width, int height, float boot) {
        int length = (int) (28 * Math.min(1.0F, boot * 2));
        int m = 8;
        int[][] corners = {{m, m, 1, 1}, {width - m - 1, m, -1, 1}, {m, height - m - 1, 1, -1}, {width - m - 1, height - m - 1, -1, -1}};
        for (int[] c : corners) {
            int x = c[0];
            int y = c[1];
            graphics.fill(Math.min(x, x + c[2] * length), y, Math.max(x, x + c[2] * length) + 1, y + 1, CYAN);
            graphics.fill(x, Math.min(y, y + c[3] * length), x + 1, Math.max(y, y + c[3] * length) + 1, CYAN);
            graphics.fill(Math.min(x + c[2] * 3, x + c[2] * 12), y + c[3] * 3, Math.max(x + c[2] * 3, x + c[2] * 12) + 1, y + c[3] * 3 + 1, CYAN_DIM);
        }
    }

    /** A strip along the top: ticks every 15 degrees, the eight directions named, the heading marked. */
    private static void compass(GuiGraphicsExtractor graphics, Font font, int width, float yaw) {
        int centre = width / 2;
        int y = 14;
        int half = 90;
        float heading = Mth.wrapDegrees(yaw);
        graphics.fill(centre - half, y + 8, centre + half + 1, y + 9, CYAN_FAINT);
        for (int deg = -180; deg < 180; deg += 15) {
            float offset = Mth.wrapDegrees(deg - heading);
            if (Math.abs(offset) > 90) {
                continue;
            }
            int x = centre + Math.round(offset * half / 90.0F);
            boolean major = Math.floorMod(deg, 45) == 0;
            graphics.fill(x, y + (major ? 3 : 5), x + 1, y + 8, major ? CYAN : CYAN_DIM);
            if (major) {
                String name = HEADINGS[Math.floorMod(Math.round(deg / 45.0F), 8)];
                graphics.text(font, name, x - font.width(name) / 2, y - 7, TEXT, true);
            }
        }
        graphics.fill(centre, y + 9, centre + 1, y + 13, 0xFFFFFFFF);
        String degrees = String.valueOf(Math.floorMod(Math.round(heading), 360));
        graphics.text(font, degrees, centre - font.width(degrees) / 2, y + 14, CYAN, true);
    }

    /** Four short arcs around the crosshair. */
    private static void reticle(GuiGraphicsExtractor graphics, int cx, int cy) {
        int r = 12;
        for (int i = 0; i < 4; i++) {
            float base = i * Mth.HALF_PI + Mth.HALF_PI / 2;
            for (int s = -3; s <= 3; s++) {
                float a = base + s * 0.08F;
                int x = cx + Math.round(Mth.cos(a) * r);
                int y = cy + Math.round(Mth.sin(a) * r);
                graphics.fill(x, y, x + 1, y + 1, CYAN_DIM);
            }
        }
    }

    private static void bar(GuiGraphicsExtractor graphics, Font font, int x, int y, Component label, float fill, int color, String value) {
        int width = 100;
        graphics.text(font, label, x, y, TEXT, true);
        graphics.text(font, value, x + width - font.width(value), y, color, true);
        graphics.fill(x, y + 10, x + width, y + 14, CYAN_FAINT);
        graphics.fill(x, y + 10, x + Math.round(width * Mth.clamp(fill, 0.0F, 1.0F)), y + 14, color);
        for (int i = 1; i < 10; i++) {
            graphics.fill(x + i * width / 10, y + 10, x + i * width / 10 + 1, y + 14, 0x60000000);
        }
    }

    private static void readout(GuiGraphicsExtractor graphics, Font font, int right, int y, Component label, String value) {
        graphics.text(font, value, right - font.width(value), y, CYAN, true);
        graphics.text(font, label, right - font.width(value) - 6 - font.width(label), y, TEXT, true);
    }
}
