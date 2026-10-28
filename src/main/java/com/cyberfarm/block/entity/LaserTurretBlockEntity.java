package com.cyberfarm.block.entity;

import com.cyberfarm.effect.Laser;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * Laser Turret: on FE, shoots the hostile mobs it can see within {@link #RANGE} blocks (see
 * {@link Laser#isTarget}), the nearest first. The mob it aims at is synced to the clients, which
 * turn the head toward it.
 */
public class LaserTurretBlockEntity extends MachineBlockEntity {
    public static final int RANGE = 16;
    public static final float DAMAGE = 8.0F;
    public static final int ENERGY_PER_SHOT = 200;
    public static final int SHOT_INTERVAL = 20;
    private static final int CAPACITY = 20_000;
    private static final int MAX_INPUT = 500;
    private static final int SCAN_INTERVAL = 10;
    /** Height of the middle of the head above the block's foot, where the beam leaves. */
    public static final double MUZZLE_HEIGHT = 0.66;

    private @Nullable LivingEntity target;
    private int cooldown;
    private int scanCooldown;
    /** The aimed mob's entity id, -1 for none: synced to the clients. */
    private int targetId = -1;

    /** Client side, for the renderer: where the head looks now, eased toward the target. */
    public float headYaw;
    public float headPitch;
    public float headTime;

    public LaserTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_TURRET.get(), pos, state, 0, CAPACITY, MAX_INPUT, 0);
    }

    public int getTargetId() {
        return targetId;
    }

    private Vec3 muzzle() {
        return Vec3.atBottomCenterOf(worldPosition).add(0.0, MUZZLE_HEIGHT, 0.0);
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        cooldown--;
        if (target != null && !canShoot(level, target)) {
            target = null;
        }
        // Without the power for a shot it does not look for one
        if (target == null && --scanCooldown <= 0 && energy.getEnergy() >= ENERGY_PER_SHOT) {
            scanCooldown = SCAN_INTERVAL;
            target = findTarget(level);
        }
        if (target != null && cooldown <= 0 && energy.consume(ENERGY_PER_SHOT)) {
            cooldown = SHOT_INTERVAL;
            Laser.fire(level, target, DAMAGE, null, pos);
        }
        int id = target != null && energy.getEnergy() >= ENERGY_PER_SHOT ? target.getId() : -1;
        if (id != targetId) {
            targetId = id;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private @Nullable LivingEntity findTarget(ServerLevel level) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(RANGE), Laser::isTarget)) {
            double distance = entity.distanceToSqr(muzzle());
            if (distance < bestDistance && canShoot(level, entity)) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    /** A live hostile mob in range, with no block between the head and its eyes. */
    private boolean canShoot(ServerLevel level, LivingEntity entity) {
        Vec3 eyes = entity.getEyePosition();
        return entity.level() == level && Laser.isTarget(entity) && eyes.distanceToSqr(muzzle()) <= RANGE * RANGE
                && level.clip(new ClipContext(muzzle(), eyes, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()))
                        .getType() == HitResult.Type.MISS;
    }

    @Override
    protected boolean isWorking() {
        return targetId >= 0;
    }

    /** Shown when the turret is right-clicked: it has no GUI. */
    public Component status() {
        if (energy.getEnergy() < ENERGY_PER_SHOT) {
            return Component.translatable("message.cyberfarm.turret.no_power", ENERGY_PER_SHOT);
        }
        if (target != null) {
            return Component.translatable("message.cyberfarm.turret.firing", target.getDisplayName());
        }
        return Component.translatable("message.cyberfarm.turret.watching", RANGE);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("target", targetId);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Only in update packets; a saved turret starts with no target
        targetId = input.getIntOr("target", -1);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return null;
    }
}
