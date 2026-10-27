package com.cyberfarm.client;

import com.cyberfarm.block.entity.SupplyTerminalBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * The village's bounds, switched on and off by the Supply Terminal's Range button: a line of cyan
 * light along the ground round the square the village covers, with taller posts at its corners.
 * Only the stretch near the player is drawn; it switches itself off once the player is far away.
 */
public final class VillageRange {
    private static final DustParticleOptions LIGHT = new DustParticleOptions(0x2FE0FF, 1.5F);
    private static final int EVERY = 10;
    private static final double DRAWN = 48.0;
    private static final double FORGOTTEN = 256.0;
    private static @Nullable BlockPos shown;

    private VillageRange() {}

    public static boolean isShown(BlockPos terminal) {
        return terminal.equals(shown);
    }

    public static void toggle(BlockPos terminal) {
        shown = terminal.equals(shown) ? null : terminal.immutable();
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (shown == null || level == null || player == null || minecraft.isPaused() || level.getGameTime() % EVERY != 0) {
            return;
        }
        if (player.distanceToSqr(shown.getCenter()) > FORGOTTEN * FORGOTTEN) {
            shown = null;
            return;
        }
        int r = SupplyTerminalBlockEntity.VILLAGE_RADIUS;
        int cx = shown.getX();
        int cz = shown.getZ();
        for (int i = -r; i <= r; i += 2) {
            mark(level, player, cx + i, cz - r, 1);
            mark(level, player, cx + i, cz + r, 1);
            mark(level, player, cx - r, cz + i, 1);
            mark(level, player, cx + r, cz + i, 1);
        }
        for (int dx : new int[] {-r, r}) {
            for (int dz : new int[] {-r, r}) {
                mark(level, player, cx + dx, cz + dz, 8);
            }
        }
    }

    /** Light on the ground at (x, z), {@code height} blocks tall, if the player is near enough to see it. */
    private static void mark(ClientLevel level, Player player, int x, int z, int height) {
        double dx = x + 0.5 - player.getX();
        double dz = z + 0.5 - player.getZ();
        if (dx * dx + dz * dz > DRAWN * DRAWN) {
            return;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        for (int up = 0; up < height; up++) {
            level.addParticle(LIGHT, x + 0.5, y + 0.2 + up, z + 0.5, 0.0, 0.0, 0.0);
        }
    }
}
