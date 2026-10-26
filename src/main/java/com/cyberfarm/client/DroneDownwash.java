package com.cyberfarm.client;

import com.cyberfarm.entity.DroneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The rotors' downwash: a drone flying low kicks up dust from the ground under it, or spray from
 * water, blown outwards; the lower it flies, the more.
 */
public final class DroneDownwash {
    /** Higher than this over the ground, nothing reaches it. */
    private static final int REACH = 3;

    private DroneDownwash() {}

    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof DroneEntity drone) || !drone.level().isClientSide()) {
            return;
        }
        int status = drone.getStatus();
        if (status != DroneEntity.STATUS_WORKING && status != DroneEntity.STATUS_RETURNING) {
            return;
        }
        Level level = drone.level();
        BlockPos.MutableBlockPos pos = drone.blockPosition().mutable();
        for (int depth = 0; depth <= REACH; depth++, pos.move(0, -1, 0)) {
            BlockState ground = level.getBlockState(pos);
            boolean water = !ground.getFluidState().isEmpty();
            if (ground.isAir() || !(water || ground.isFaceSturdy(level, pos, Direction.UP))) {
                continue;
            }
            double surface = pos.getY() + (water ? 0.9 : 1.0);
            double height = drone.getY() - surface;
            RandomSource random = level.getRandom();
            // The closer, the likelier, up to about one puff a tick right over the ground
            if (height > REACH || random.nextFloat() > 1.0F - (float) height / REACH) {
                return;
            }
            float angle = random.nextFloat() * Mth.TWO_PI;
            double x = drone.getX() + Mth.cos(angle) * 0.5;
            double z = drone.getZ() + Mth.sin(angle) * 0.5;
            level.addParticle(water ? ParticleTypes.SPLASH : ParticleTypes.DUST_PLUME, x, surface + 0.05, z,
                    Mth.cos(angle) * 0.08, 0.01, Mth.sin(angle) * 0.08);
            return;
        }
    }
}
