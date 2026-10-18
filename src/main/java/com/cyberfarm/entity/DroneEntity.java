package com.cyberfarm.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.drone.DroneModules;
import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * A farming drone bound to a Drone Dock. It charges at the dock, flies to mature crops inside the
 * dock's work area, harvests and replants them, then brings the harvest back.
 */
public class DroneEntity extends PathfinderMob {
    // Status codes shown in the dock GUI (0 means no drone)
    public static final int STATUS_NONE = 0;
    public static final int STATUS_IDLE = 1;
    public static final int STATUS_CHARGING = 2;
    public static final int STATUS_WORKING = 3;
    public static final int STATUS_RETURNING = 4;

    /** Height above the dock block where the drone parks: just over the 6px pad. */
    public static final double DOCK_HEIGHT = 0.4;
    /** Within this horizontal distance of the dock the drone stops pathfinding and lands by hand. */
    private static final double LANDING_RADIUS = 2.5;
    /** The drone lines up this high above the pad before descending. */
    private static final double APPROACH_HEIGHT = 0.8;
    public static final int FLIGHT_COST = 2;
    public static final int HARVEST_COST = 40;
    public static final int CARGO_SLOTS = 9;
    private static final int SCAN_INTERVAL = 40;
    private static final int TARGET_TIMEOUT = 200;
    private static final double HARVEST_REACH = 1.8;

    private static final EntityDataAccessor<Integer> DATA_STATUS = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HARVESTER = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BOOLEAN);

    private DroneModules modules = DroneModules.EMPTY;
    private int energy;
    private @Nullable BlockPos dockPos;
    private @Nullable BlockPos target;
    private int targetTicks;
    private int scanCooldown;
    private final Set<BlockPos> unreachable = new HashSet<>();
    private final ItemStacksResourceHandler cargo = new ItemStacksResourceHandler(CARGO_SLOTS);

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    public void setup(DroneModules modules, int energy, BlockPos dockPos) {
        this.modules = modules;
        this.energy = Math.min(energy, modules.energyCapacity());
        this.dockPos = dockPos.immutable();
        this.entityData.set(DATA_HARVESTER, modules.has(ModItems.HARVESTER_MODULE.get()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATUS, STATUS_IDLE);
        builder.define(DATA_HARVESTER, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public void travel(Vec3 input) {
        this.travelFlying(input, this.getSpeed());
    }

    public boolean hasHarvester() {
        return entityData.get(DATA_HARVESTER);
    }

    public int getStatus() {
        return entityData.get(DATA_STATUS);
    }

    private void setStatus(int status) {
        entityData.set(DATA_STATUS, status);
    }

    private int capacity() {
        return Math.max(1, modules.energyCapacity());
    }

    // ---------------------------------------------------------------- AI

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (dockPos == null || !level.isLoaded(dockPos)) {
            return;
        }
        if (!(level.getBlockEntity(dockPos) instanceof DroneDockBlockEntity dock)) {
            // The dock was removed: turn back into an item so nothing is lost.
            dropAsItem(level);
            return;
        }

        int status = getStatus();
        if (status == STATUS_WORKING || status == STATUS_RETURNING) {
            energy = Math.max(0, energy - FLIGHT_COST);
        }
        switch (status) {
            case STATUS_WORKING -> tickWorking(level, dock);
            case STATUS_RETURNING -> tickReturning();
            default -> tickDocked(level, dock);
        }
        dock.updateDroneStatus(level.getGameTime(), getStatus(), energy * 100 / capacity());
    }

    private Vec3 dockHover() {
        return new Vec3(dockPos.getX() + 0.5, dockPos.getY() + DOCK_HEIGHT, dockPos.getZ() + 0.5);
    }

    /**
     * Brings the drone onto the pad: pathfinding up to a point above the dock, then a slow
     * vertical descent with speed easing off near the pad. Returns true once it is parked.
     * The vanilla flying move control always pushes at full speed, so it overshoots small
     * targets; the last metres are steered by hand instead.
     */
    private boolean flyHome() {
        Vec3 home = dockHover();
        double horizontal = home.subtract(position()).horizontalDistance();
        if (horizontal > LANDING_RADIUS) {
            if (tickCount % 10 == 0 || navigation.isDone()) {
                flyTo(home.add(0.0, APPROACH_HEIGHT, 0.0));
            }
            return false;
        }

        navigation.stop();
        holdMoveControl();
        // Line up above the pad first, then descend straight down
        Vec3 target = horizontal > 0.2 ? new Vec3(home.x, Math.max(getY(), home.y + APPROACH_HEIGHT), home.z) : home;
        Vec3 delta = target.subtract(position());
        if (horizontal <= 0.2 && Math.abs(delta.y) < 0.03) {
            setDeltaMovement(Vec3.ZERO);
            if (position().distanceToSqr(home) > 1.0E-6) {
                setPos(home.x, home.y, home.z);
            }
            return true;
        }
        double length = delta.length();
        double speed = Math.min(0.2, length * 0.35);
        setDeltaMovement(delta.scale(speed / length));
        return false;
    }

    /** Stops the vanilla move control from steering, so the velocity set by hand is kept. */
    private void holdMoveControl() {
        moveControl.setWantedPosition(getX(), getY(), getZ(), 0.0);
    }

    private void tickDocked(ServerLevel level, DroneDockBlockEntity dock) {
        flyHome();
        unloadCargo(dock);

        int charged = dock.charge(capacity() - energy);
        energy += charged;
        setStatus(charged > 0 ? STATUS_CHARGING : STATUS_IDLE);

        // Finish charging while the dock can supply power; never leave below 30%.
        if ((charged > 0 && energy < capacity()) || energy < capacity() * 3 / 10 || !hasHarvester()) {
            return;
        }
        if (--scanCooldown > 0) {
            return;
        }
        scanCooldown = SCAN_INTERVAL;
        target = findTarget(level, dock.getWorkArea());
        if (target != null) {
            targetTicks = 0;
            setStatus(STATUS_WORKING);
        }
    }

    private void tickWorking(ServerLevel level, DroneDockBlockEntity dock) {
        if (isCargoFull() || energy < capacity() / 10) {
            startReturn();
            return;
        }
        BoundingBox area = dock.getWorkArea();
        if (target == null || !isHarvestable(level, target) || !area.isInside(target)) {
            target = findTarget(level, area);
            targetTicks = 0;
            if (target == null) {
                startReturn();
                return;
            }
        }

        Vec3 crop = Vec3.atCenterOf(target);
        if (position().distanceTo(crop) <= HARVEST_REACH) {
            harvest(level, target);
            target = null;
            return;
        }
        if (++targetTicks > TARGET_TIMEOUT) {
            unreachable.add(target);
            target = null;
            return;
        }
        if (targetTicks % 10 == 1) {
            flyTo(crop.add(0.0, 1.0, 0.0));
        }
    }

    private void tickReturning() {
        if (flyHome()) {
            unreachable.clear();
            scanCooldown = SCAN_INTERVAL;
            setStatus(STATUS_IDLE);
        }
    }

    private void flyTo(Vec3 pos) {
        double speed = modules.speed();
        if (!navigation.moveTo(pos.x, pos.y, pos.z, speed)) {
            // No path found: try a straight line.
            moveControl.setWantedPosition(pos.x, pos.y, pos.z, speed);
        }
    }

    private void startReturn() {
        target = null;
        navigation.stop();
        setStatus(STATUS_RETURNING);
    }

    private @Nullable BlockPos findTarget(ServerLevel level, BoundingBox area) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
            if (!unreachable.contains(pos) && isHarvestable(level, pos)) {
                double distance = pos.distToCenterSqr(position());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    private static boolean isHarvestable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        return state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN);
    }

    private void harvest(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, null);

        // Replant with one of the seeds the crop dropped (wheat seeds, carrots, potatoes...).
        BlockState replanted = null;
        if (state.getBlock() instanceof CropBlock crop) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == crop) {
                    drop.shrink(1);
                    replanted = crop.getStateForAge(0);
                    break;
                }
            }
        }

        level.destroyBlock(pos, false, this);
        if (replanted != null) {
            level.setBlock(pos, replanted, Block.UPDATE_ALL);
        }
        for (ItemStack drop : drops) {
            ItemStack rest = storeInCargo(drop);
            if (!rest.isEmpty()) {
                Block.popResource(level, pos, rest);
            }
        }
        energy = Math.max(0, energy - HARVEST_COST);
    }

    // ---------------------------------------------------------------- cargo

    private ItemStack storeInCargo(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = ResourceHandlerUtil.insertStacking(cargo, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    private boolean isCargoFull() {
        for (int i = 0; i < cargo.size(); i++) {
            if (cargo.getResource(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void unloadCargo(DroneDockBlockEntity dock) {
        for (int i = 0; i < cargo.size(); i++) {
            ItemStack stack = ItemUtil.getStack(cargo, i);
            if (!stack.isEmpty()) {
                ItemStack rest = dock.deposit(stack);
                cargo.set(i, ItemResource.of(rest), rest.getCount());
            }
        }
    }

    private void dropCargo(ServerLevel level) {
        for (int i = 0; i < cargo.size(); i++) {
            ItemStack stack = ItemUtil.getStack(cargo, i);
            if (!stack.isEmpty()) {
                spawnAtLocation(level, stack);
                cargo.set(i, ItemResource.EMPTY, 0);
            }
        }
    }

    // ---------------------------------------------------------------- pickup and lifecycle

    public ItemStack toItem() {
        ItemStack stack = new ItemStack(ModItems.DRONE.get());
        stack.set(ModDataComponents.DRONE_MODULES.get(), modules);
        stack.set(ModDataComponents.ENERGY.get(), energy);
        return stack;
    }

    private void dropAsItem(ServerLevel level) {
        spawnAtLocation(level, toItem());
        dropCargo(level);
        discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
            return super.mobInteract(player, hand);
        }
        if (level() instanceof ServerLevel level) {
            if (dockPos != null && level.getBlockEntity(dockPos) instanceof DroneDockBlockEntity dock) {
                unloadCargo(dock);
                dock.releaseDrone(getUUID());
            }
            dropCargo(level);
            ItemStack item = toItem();
            if (!player.addItem(item)) {
                spawnAtLocation(level, item);
            }
            discard();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        spawnAtLocation(level, toItem());
        dropCargo(level);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ---------------------------------------------------------------- save data

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("modules", DroneModules.CODEC, modules);
        output.putInt("energy", energy);
        output.storeNullable("dock", BlockPos.CODEC, dockPos);
        cargo.serialize(output.child("cargo"));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        modules = input.read("modules", DroneModules.CODEC).orElse(DroneModules.EMPTY);
        energy = input.getIntOr("energy", 0);
        dockPos = input.read("dock", BlockPos.CODEC).orElse(null);
        cargo.deserialize(input.childOrEmpty("cargo"));
        entityData.set(DATA_HARVESTER, modules.has(ModItems.HARVESTER_MODULE.get()));
    }
}
