package com.cyberfarm.client;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.ReactorControllerBlock;
import com.cyberfarm.block.entity.ReactorControllerBlockEntity;
import com.cyberfarm.multiblock.TowerSize;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Keeps you out of a formed cooling tower. Its smooth shell is drawn over stepped rings of blocks and bulges past them at the
 * base and the waist, where the blocks alone would let you walk into the drawing: there you are pushed back to its surface.
 */
public final class TowerClearance {
    private static final double DIAGONAL = Math.sqrt(0.5);
    /** Closer to the axis than this inside the drawn surface, you are in the tower's hollow, which the blocks already bound. */
    private static final double HOLLOW = 1.2;

    private TowerClearance() {}

    public static void tick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || player.noPhysics) {
            return;
        }
        Level level = player.level();
        int chunkX = player.chunkPosition().x(), chunkZ = player.chunkPosition().z();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof ReactorControllerBlockEntity reactor && reactor.isFormed()) {
                        pushOut(player, reactor);
                    }
                }
            }
        }
    }

    private static void pushOut(LocalPlayer player, ReactorControllerBlockEntity reactor) {
        BlockState state = reactor.getBlockState();
        TowerSize size = TowerSize.byOrdinal(state.getValue(ReactorControllerBlock.SIZE));
        BlockPos floor = size.floorCentre(reactor.getBlockPos(), state.getValue(MachineBlock.FACING));
        double low = Math.max(0.0, player.getY() - floor.getY());
        double high = Math.min(size.height(), player.getY() + player.getBbHeight() - floor.getY());
        if (high <= low) {
            return;
        }
        // The widest the drawn shell gets over your height
        double outer = 0.0;
        for (double h = low; h < high; h += 0.25) {
            outer = Math.max(outer, size.radius((float) h));
        }
        outer = Math.max(outer, size.radius((float) high));
        double x = player.getX() - (floor.getX() + 0.5), z = player.getZ() - (floor.getZ() + 0.5);
        double ax = Math.abs(x), az = Math.abs(z), diagonal = (ax + az) * DIAGONAL;
        // How far out you are, measured square to the octagon's nearest face
        double reach = Math.max(Math.max(ax, az), diagonal);
        double clear = outer + player.getBbWidth() / 2.0;
        if (reach >= clear || reach < outer - HOLLOW) {
            return;
        }
        double nx, nz;
        if (diagonal >= Math.max(ax, az)) {
            nx = Math.signum(x) * DIAGONAL;
            nz = Math.signum(z) * DIAGONAL;
        } else if (ax >= az) {
            nx = Math.signum(x);
            nz = 0.0;
        } else {
            nx = 0.0;
            nz = Math.signum(z);
        }
        double push = clear - reach;
        if (!player.level().noCollision(player, player.getBoundingBox().move(nx * push, 0.0, nz * push))) {
            return;
        }
        player.setPos(player.getX() + nx * push, player.getY(), player.getZ() + nz * push);
        Vec3 motion = player.getDeltaMovement();
        double inward = motion.x * nx + motion.z * nz;
        if (inward < 0.0) {
            player.setDeltaMovement(motion.x - nx * inward, motion.y, motion.z - nz * inward);
        }
    }
}
