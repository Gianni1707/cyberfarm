package com.cyberfarm.entity;

import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** What piloting a mech changes for its pilot: getting out, getting hurt, punching and digging. */
public final class MechEvents {
    /** Added to a bare-handed punch from the cockpit, and to a blow of the energy blade. */
    private static final float PUNCH_DAMAGE = 10.0F;
    private static final float BLADE_DAMAGE = 18.0F;
    /** The blade's sweep: what it does to everything else close to the target, and how close. */
    private static final float SWEEP_DAMAGE = 8.0F;
    private static final double SWEEP_RANGE = 2.5;
    private static final int FE_PER_SLASH = 80;
    /** With the drill: digging speed, and what every extra block of the 3x3 costs. */
    private static final float DRILL_SPEED = 12.0F;
    private static final int FE_PER_DRILLED = 20;
    /** Set while the drill breaks the blocks around the one dug, so those do not dig 3x3 in turn. */
    private static boolean drilling;
    /** Digging speed from the cockpit, an iron pickaxe's (the rider's not-on-ground penalty comes before this). */
    private static final float DIG_SPEED = 6.0F;

    /** Set while the blade's sweep hurts the mobs around its target, so those hits do not sweep in turn. */
    private static boolean sweeping;

    private MechEvents() {}

    private static MechEntity pilotedMech(Entity entity) {
        return entity instanceof Player player && player.getVehicle() instanceof MechEntity mech && mech.isPiloted() ? mech : null;
    }

    /** Sneaking does not get you out of a mech: the exit key does, with the chest opening first. */
    public static void onDismount(EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityBeingMounted() instanceof MechEntity mech && !mech.isReleasing()
                && event.getEntityMounting() instanceof Player pilot && pilot.isAlive() && pilot.isShiftKeyDown() && mech.isAlive()) {
            event.setCanceled(true);
        }
    }

    /** The pilot is shielded by the armour: what would hit them hits the mech. A bare-handed punch from inside hits hard. */
    public static void onDamage(LivingIncomingDamageEvent event) {
        MechEntity shell = pilotedMech(event.getEntity());
        if (shell != null && event.getEntity().level() instanceof ServerLevel level && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            shell.hurtServer(level, event.getSource(), event.getAmount());
            event.setCanceled(true);
            return;
        }
        Entity attacker = event.getSource().getDirectEntity();
        MechEntity mech = pilotedMech(attacker);
        if (mech == null || !((Player) attacker).getMainHandItem().isEmpty() || event.getEntity() == mech) {
            return;
        }
        if (mech.isToolOn(MechModule.BLADE) && mech.consume(FE_PER_SLASH)) {
            // The blade: a heavy cut, and a sweep through everything around the target
            event.setAmount(event.getAmount() + BLADE_DAMAGE);
            if (!sweeping && event.getEntity().level() instanceof ServerLevel level) {
                sweeping = true;
                LivingEntity target = event.getEntity();
                for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(SWEEP_RANGE),
                        e -> e != target && e != mech && e != attacker && e.isAlive())) {
                    near.hurtServer(level, event.getSource(), SWEEP_DAMAGE);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 3, 0.6, 0.2, 0.6, 0.0);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.MECH_SLASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                sweeping = false;
            }
        } else if (mech.consume(MechEntity.FE_PER_PUNCH)) {
            event.setAmount(event.getAmount() + PUNCH_DAMAGE);
            LivingEntity target = event.getEntity();
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.MECH_PUNCH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** The mech's fists dig like an iron pickaxe, whatever the pilot holds; the drill twice as fast. */
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        MechEntity mech = pilotedMech(event.getEntity());
        if (mech != null && mech.getEnergy() > 0) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), mech.isToolOn(MechModule.DRILL) ? DRILL_SPEED : DIG_SPEED));
        }
    }

    /** The drill digs 3x3: the blocks around the one dug, across the way the pilot looks. */
    public static void onBlockBreak(BreakBlockEvent event) {
        MechEntity mech = pilotedMech(event.getPlayer());
        if (drilling || mech == null || !mech.isToolOn(MechModule.DRILL) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Player pilot = event.getPlayer();
        Direction facing = Direction.getApproximateNearest(pilot.getLookAngle());
        BlockPos centre = event.getPos();
        drilling = true;
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) {
                        continue;
                    }
                    BlockPos pos = switch (facing.getAxis()) {
                        case X -> centre.offset(0, a, b);
                        case Y -> centre.offset(a, 0, b);
                        case Z -> centre.offset(a, b, 0);
                    };
                    BlockState state = level.getBlockState(pos);
                    float hardness = state.getDestroySpeed(level, pos);
                    if (state.isAir() || hardness < 0 || hardness > 50 || !mech.consume(FE_PER_DRILLED)) {
                        continue;
                    }
                    level.destroyBlock(pos, true, pilot);
                }
            }
        } finally {
            drilling = false;
        }
    }

    /** ...and get the drops a pickaxe, axe or shovel would, up to iron's tier; with the drill, any tier. */
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        MechEntity mech = pilotedMech(event.getEntity());
        if (mech != null && (mech.isToolOn(MechModule.DRILL) || !event.getTargetBlock().is(BlockTags.NEEDS_DIAMOND_TOOL))) {
            event.setCanHarvest(true);
        }
    }
}
