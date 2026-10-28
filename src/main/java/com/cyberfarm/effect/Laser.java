package com.cyberfarm.effect;

import com.cyberfarm.CyberFarm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The red laser of the Guard Module and the Laser Turret: what it may shoot, and the shot itself. */
public final class Laser {
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, CyberFarm.id("laser"));
    /** Every laser costs the same per point of damage: the Guard drone and the Laser Turret only differ in reach and rate. */
    public static final int ENERGY_PER_DAMAGE = 25;

    private Laser() {}

    /**
     * Hostile mobs only: never players, villagers, golems or animals, neutral mobs only once they are
     * angry, and never a named mob or one riding something (kept on purpose, in a farm or a cart).
     */
    public static boolean isTarget(LivingEntity entity) {
        return entity instanceof Enemy && entity.isAlive() && !entity.isInvulnerable() && !entity.hasCustomName() && !entity.isPassenger()
                && !(entity instanceof NeutralMob neutral && !neutral.isAngry());
    }

    /** Hits {@code target}, drawing the beam from the drone {@code shooter} or, without one, from the turret at {@code origin}. */
    public static void fire(ServerLevel level, LivingEntity target, float damage, @Nullable Entity shooter, BlockPos origin) {
        Holder<DamageType> type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE);
        DamageSource source = shooter != null ? new DamageSource(type, shooter) : new DamageSource(type, Vec3.atCenterOf(origin));
        target.hurtServer(level, source, damage);
        Effects.laser(level, BlockPos.containing(target.getBoundingBox().getCenter()), origin, shooter);
        level.playSound(null, origin, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.5F, 1.9F);
    }
}
