package com.cyberfarm.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.cyberfarm.client.renderer.FrameDroneRenderer;
import com.cyberfarm.client.renderer.ModelParts;
import com.cyberfarm.trinket.DroneFetch;
import com.cyberfarm.trinket.PocketDroneItem;
import com.cyberfarm.trinket.Trinkets;
import com.cyberfarm.trinket.UtilityBeltItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

/**
 * The Pocket Drones out in the world: a small one of the Builder mini drones (FrameDroneRenderer.drone), waiting over
 * its player's shoulder, flying out to a drop and home with it under its belly, along the path DroneFetch keeps on
 * the player. Its lens lights while it is on an errand. Sent out, it opens on the belt's plate and flies up; called
 * back, it flies down and folds onto it.
 */
public final class PocketDroneView {
    /** Half a Builder mini drone. */
    private static final float SIZE = 0.5F;
    /** How high it arcs over the straight line between home and the drop, in blocks. */
    private static final double ARC = 0.6;
    private static final int LENS_ON = 0xFFD1FEE7;
    private static final double SEEN = 64.0;
    /** Ticks to open on the belt and fly up to the shoulder, and to fly back down and fold. */
    static final float LAUNCH = 16.0F, LAND = 14.0F;
    /** Each player's drone: out or not, and since when, as this client saw it change (no animation for one already out). */
    private static final Map<UUID, Seen> SEEN_STATE = new HashMap<>();
    /** The level SEEN_STATE belongs to: its times are ModelParts.time of that level, and your own UUID is in the next world too. */
    private static @Nullable Level seenLevel;

    private record Seen(boolean out, float since) {}

    private PocketDroneView() {}

    public static void submit(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if (level != seenLevel) {
            SEEN_STATE.clear();
            seenLevel = level;
        }
        float now = ModelParts.time(level, partial);
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        for (Player player : level.players()) {
            if (player.isInvisible() || player.distanceToSqr(minecraft.player) > SEEN * SEEN) {
                continue;
            }
            boolean out = out(player);
            float age = age(player, out, now);
            if (!out && age >= LAND) {
                continue;
            }
            DroneFetch fetch = player.getData(Trinkets.FETCH);
            float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
            Vec3 plate = plate(player, partial, yaw);
            Vec3 flying = position(fetch, DroneFetch.anchor(player, partial, now), ModelParts.since(level, fetch.since(), partial));
            // Launch: it opens on the belt plate, then flies up; landing: it flies down, then folds onto the plate
            float unfold = out ? smooth(age / (LAUNCH * 0.4F)) : 1.0F - smooth((age - LAND * 0.6F) / (LAND * 0.4F));
            float travel = out ? smooth((age - LAUNCH * 0.35F) / (LAUNCH * 0.65F)) : 1.0F - smooth(age / (LAND * 0.65F));
            Vec3 at = plate.lerp(flying, travel).add(0.0, 0.3 * Mth.sin(Mth.PI * travel), 0.0);
            draw(event.getPoseStack(), event.getSubmitNodeCollector(), minecraft, level, at, camera, yaw, now, unfold,
                    fetch.phase() != DroneFetch.IDLE, fetch.phase() == DroneFetch.BACK ? fetch.carried() : ItemStack.EMPTY, player);
        }
        SEEN_STATE.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
    }

    private static boolean out(Player player) {
        ItemStack drone = UtilityBeltItem.content(UtilityBeltItem.worn(player), UtilityBeltItem.DRONE);
        return PocketDroneItem.isActive(drone) && PocketDroneItem.energy(drone) > 0;
    }

    /** Ticks since this player's drone last went out or came in; long ago for one first seen. */
    private static float age(Player player, boolean out, float now) {
        Seen seen = SEEN_STATE.get(player.getUUID());
        if (seen == null || seen.out() != out) {
            seen = new Seen(out, seen == null ? now - 1000.0F : now);
            SEEN_STATE.put(player.getUUID(), seen);
        }
        return now - seen.since();
    }

    /** Whether this player's drone is still flying home to the belt: the belt shows its empty plate till it lands. */
    public static boolean landing(Player player) {
        Seen seen = SEEN_STATE.get(player.getUUID());
        Level level = player.level();
        return seen != null && level == seenLevel && !seen.out() && ModelParts.time(level, 0.0F) - seen.since() < LAND;
    }

    /** The belt's magnet plate, in the world: in the middle of the back, at belt height. */
    static Vec3 plate(Player player, float partial, float yaw) {
        float rad = yaw * Mth.DEG_TO_RAD;
        Vec3 at = player.getPosition(partial);
        double backX = Mth.sin(rad), backZ = -Mth.cos(rad);
        return new Vec3(at.x + backX * 0.2, at.y + player.getBbHeight() * 0.45, at.z + backZ * 0.2);
    }

    static float smooth(float x) {
        float t = Mth.clamp(x, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** Where the drone is, {@code elapsed} ticks into the leg: home, or along an arc between home and the drop, eased at both ends. */
    static Vec3 position(DroneFetch fetch, Vec3 home, float elapsed) {
        if (fetch.phase() == DroneFetch.IDLE) {
            return home;
        }
        // Over the drop, not on it
        Vec3 drop = fetch.target().add(0.0, 0.45, 0.0);
        float u = fetch.progress(elapsed);
        float eased = u * u * (3.0F - 2.0F * u);
        Vec3 from = fetch.phase() == DroneFetch.OUT ? home : drop, to = fetch.phase() == DroneFetch.OUT ? drop : home;
        return from.lerp(to, eased).add(0.0, ARC * Mth.sin(Mth.PI * u), 0.0);
    }

    private static void draw(PoseStack poseStack, SubmitNodeCollector collector, Minecraft minecraft, Level level, Vec3 at, Vec3 camera,
            float yaw, float now, float unfold, boolean busy, ItemStack carried, Player player) {
        int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(at));
        poseStack.pushPose();
        poseStack.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        // Folded it lies flat on the back, its top facing out behind; it levels out as it opens
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F * (1.0F - unfold)));
        if (!carried.isEmpty()) {
            // The drop hangs under its belly
            // A state of its own each frame: the collector draws it later, after the other drones
            ItemStackRenderState state = new ItemStackRenderState();
            minecraft.getItemModelResolver().updateForLiving(state, carried, ItemDisplayContext.GROUND, player);
            poseStack.pushPose();
            poseStack.translate(0.0F, -0.22F, 0.0F);
            poseStack.scale(0.6F, 0.6F, 0.6F);
            state.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.scale(SIZE / 16.0F, SIZE / 16.0F, SIZE / 16.0F);
        FrameDroneRenderer.drone(poseStack, collector, now, busy ? LENS_ON : FrameDroneRenderer.LENS_OFF, light, unfold);
        poseStack.popPose();
    }
}
