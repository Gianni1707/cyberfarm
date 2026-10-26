package com.cyberfarm.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.block.entity.SwarmDockBlockEntity;
import com.cyberfarm.drone.AddonType;
import com.cyberfarm.drone.DroneCargo;
import com.cyberfarm.drone.DroneFlight;
import com.cyberfarm.drone.DroneModules;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.drone.job.DroneJob;
import com.cyberfarm.drone.job.TaxiJob;
import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A drone bound to a Drone Dock. It charges at the dock, then does the jobs its function modules
 * give it (harvest, quarry, transport, plant, log), taking turns between modules, and brings
 * everything back. With a Taxi Module it also carries players hanging from its hook.
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
    /** A Mini Drone is half the size of the others; four of them park on the corners of one pad. */
    private static final double MINI_SCALE = 0.5;
    /** Close enough to the middle that a Mini Drone (0.3 wide) clears the pad's corner beacons (3/16 in from each edge). */
    public static final double CORNER_OFFSET = 0.15;
    /** A Taxi drone waits for passengers this high above its parking spot. */
    private static final double TAXI_WAIT_HEIGHT = 2.7;
    /**
     * How far below the drone a hanging passenger's feet are: the raised fist of a player (1.88
     * blocks over the feet, model scaled to 15/16) closes around the hook bar, 0.37-0.44 below the drone.
     */
    public static final double PASSENGER_DROP = 2.25;
    /** How far the raised arm's hand sits to the side of a player's centre. */
    private static final double HAND_SIDE = 0.34;
    private static final int SCAN_INTERVAL = 40;

    private static final EntityDataAccessor<Integer> DATA_STATUS = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TASKS = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    /** One item of the first cargo stack, so clients can draw it hanging from the Transport claw. */
    private static final EntityDataAccessor<ItemStack> DATA_CARRIED = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.ITEM_STACK);
    /** Game time the deploy sequence started, 0 when there is none: the clients animate from it. */
    private static final EntityDataAccessor<Long> DATA_STARTUP = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.LONG);

    // Deploy sequence, in ticks from the moment it lands on the dock: the folded arms open, the
    // rotors spin up from a standstill, then it lifts off, hovers a moment and goes about its work
    public static final int UNFOLD_TICKS = 20;
    public static final int SPIN_START = 15;
    public static final int SPIN_TICKS = 30;
    public static final int LIFT_START = 45;
    public static final int STARTUP_TICKS = 85;
    private static final double LIFT_HEIGHT = 1.5;
    /** Game time the pickup sequence started (after landing), 0 when there is none. */
    private static final EntityDataAccessor<Long> DATA_SHUTDOWN = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.LONG);
    // Pickup sequence, in ticks from touching down: the rotors spin down, the arms fold, and it
    // goes into the inventory of the player who picked it up
    public static final int SPIN_DOWN_TICKS = 30;
    public static final int FOLD_START = 20;
    public static final int FOLD_TICKS = 20;
    private static final int SHUTDOWN_TICKS = 50;
    /** Ticks it may take to land before it gives up and folds where it is. */
    private static final int LANDING_LIMIT = 100;

    private DroneModules modules = DroneModules.EMPTY;
    private int energy;
    private @Nullable BlockPos dockPos;
    private @Nullable DroneJob job;
    private @Nullable DroneTask jobTask;
    /** Round robin over the modules, so one busy module does not starve the others. */
    private int nextTask;
    private int scanCooldown;
    private final DroneFlight flight = new DroneFlight(this, () -> modules.speed());
    private final Set<BlockPos> unreachable = new HashSet<>();
    private final DroneCargo cargo = new DroneCargo();
    /** Where a drone being picked up lands, and who gets it; null when nobody is picking it up. */
    private @Nullable Vec3 landingSpot;
    private @Nullable UUID pickupBy;
    private int landingTicks;

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
                // Only used by the pathfinding fallback; normal flight is steered by hand.
                .add(Attributes.FLYING_SPEED, 0.3)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    public void setup(DroneModules modules, int energy, BlockPos dockPos) {
        this.modules = modules;
        this.energy = Math.min(energy, modules.energyCapacity());
        this.dockPos = dockPos.immutable();
        this.entityData.set(DATA_TASKS, modules.taskMask());
        getAttribute(Attributes.SCALE).setBaseValue(modules.isMini() ? MINI_SCALE : 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATUS, STATUS_IDLE);
        builder.define(DATA_TASKS, 0);
        builder.define(DATA_CARRIED, ItemStack.EMPTY);
        builder.define(DATA_STARTUP, 0L);
        builder.define(DATA_SHUTDOWN, 0L);
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

    /** Installed modules as a bit mask of {@link DroneTask#bit()}, synced to the client. */
    public int getTaskMask() {
        return entityData.get(DATA_TASKS);
    }

    public int getStatus() {
        return entityData.get(DATA_STATUS);
    }

    /** Starts the deploy sequence; called once the drone is placed on its dock. */
    public void beginStartup() {
        entityData.set(DATA_STARTUP, level().getGameTime());
        playSound(ModSounds.DRONE_UNFOLD.get(), 1.0F, 1.0F);
    }

    /** Ticks into the deploy sequence, with the partial tick; -1 once it is over (or never ran). */
    public float startupTicks(float partialTick) {
        long start = entityData.get(DATA_STARTUP);
        if (start == 0) {
            return -1.0F;
        }
        float elapsed = level().getGameTime() - start + partialTick;
        return elapsed >= STARTUP_TICKS ? -1.0F : Math.max(0.0F, elapsed);
    }

    /** Ticks since a picked-up drone touched down, with the partial tick; -1 when it is not being picked up. */
    public float shutdownTicks(float partialTick) {
        long start = entityData.get(DATA_SHUTDOWN);
        return start == 0 ? -1.0F : Math.max(0.0F, level().getGameTime() - start + partialTick);
    }

    /** How far the arms are open, from 0 (folded, deploying or picked up) to 1. */
    public float unfold(float partialTick) {
        float shutdown = shutdownTicks(partialTick);
        if (shutdown >= 0) {
            return 1.0F - Mth.clamp((shutdown - FOLD_START) / FOLD_TICKS, 0.0F, 1.0F);
        }
        float ticks = startupTicks(partialTick);
        return ticks < 0 ? 1.0F : Mth.clamp(ticks / UNFOLD_TICKS, 0.0F, 1.0F);
    }

    /** How fast the rotors turn, from 0 (still) to 1: spinning up on deploy, down when picked up. */
    public float rotorPower(float partialTick) {
        float shutdown = shutdownTicks(partialTick);
        if (shutdown >= 0) {
            return 1.0F - Mth.clamp(shutdown / SPIN_DOWN_TICKS, 0.0F, 1.0F);
        }
        float ticks = startupTicks(partialTick);
        return ticks < 0 ? 1.0F : Mth.clamp((ticks - SPIN_START) / SPIN_TICKS, 0.0F, 1.0F);
    }

    /**
     * Sneak + right-click: instead of vanishing on the spot the drone lands (on its pad if it is
     * there, else on the ground below), spins down, folds, and then goes to the player.
     */
    private void beginPickup(ServerLevel level, Player player) {
        if (landingSpot != null || entityData.get(DATA_SHUTDOWN) != 0) {
            return;
        }
        stopJob(level);
        job = null;
        jobTask = null;
        navigation.stop();
        entityData.set(DATA_STARTUP, 0L);
        pickupBy = player.getUUID();
        landingTicks = 0;
        if (dockPos != null && dockHover().subtract(position()).horizontalDistance() < DroneFlight.LANDING_RADIUS) {
            landingSpot = dockHover();
        } else {
            HitResult ground = level.clip(new ClipContext(position(), position().subtract(0.0, 64.0, 0.0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
            landingSpot = ground.getType() == HitResult.Type.MISS ? position() : ground.getLocation();
        }
    }

    /** Lands, then counts down the spin-down and the fold the clients draw, then hands itself over. */
    private void tickPickup(ServerLevel level) {
        if (landingSpot != null) {
            flyTowards(landingSpot);
            if (position().distanceTo(landingSpot) < 0.08 || ++landingTicks > LANDING_LIMIT) {
                setPos(landingSpot.x, landingSpot.y, landingSpot.z);
                landingSpot = null;
                entityData.set(DATA_SHUTDOWN, level.getGameTime());
                playSound(ModSounds.DRONE_LAND.get(), 1.0F, 1.0F);
            }
            return;
        }
        flight.holdMoveControl();
        setDeltaMovement(Vec3.ZERO);
        int elapsed = (int) shutdownTicks(0.0F);
        if (elapsed == FOLD_START) {
            playSound(ModSounds.DRONE_UNFOLD.get(), 1.0F, 0.8F);
        }
        if (elapsed >= SHUTDOWN_TICKS) {
            finishPickup(level);
        }
    }

    private void finishPickup(ServerLevel level) {
        if (dockPos != null && level.getBlockEntity(dockPos) instanceof DroneDockBlockEntity dock) {
            cargo.unloadInto(dock);
            dock.releaseDrone(getUUID());
        }
        cargo.dropAll(stack -> spawnAtLocation(level, stack));
        ItemStack item = toItem();
        Player player = pickupBy != null ? level.getPlayerByUUID(pickupBy) : null;
        // A player who walked far away finds it where it landed
        if (player == null || player.distanceToSqr(this) > 32 * 32 || !player.addItem(item)) {
            spawnAtLocation(level, item);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 1.4F);
        }
        discard();
    }

    /** Waits on the pad while it unfolds and spins up, then rises above the dock and hovers until the end. */
    private void tickStartup(int elapsed) {
        if (elapsed < LIFT_START) {
            flight.holdMoveControl();
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (elapsed == LIFT_START) {
            playSound(ModSounds.DRONE_TAKEOFF.get(), 1.0F, 1.0F);
        }
        flyTowards(dockHover().add(0.0, LIFT_HEIGHT, 0.0));
    }

    private void setStatus(int status) {
        int previous = getStatus();
        if (status == STATUS_WORKING && (previous == STATUS_IDLE || previous == STATUS_CHARGING)) {
            playSound(ModSounds.DRONE_TAKEOFF.get(), 1.0F, 1.0F);
        } else if (status == STATUS_IDLE && previous == STATUS_RETURNING) {
            playSound(ModSounds.DRONE_LAND.get(), 1.0F, 1.0F);
        }
        entityData.set(DATA_STATUS, status);
    }

    private int capacity() {
        return Math.max(1, modules.energyCapacity());
    }

    /** The drone never leaves its dock with less than 30% charge. */
    private int departureCharge() {
        return capacity() * 3 / 10;
    }

    // ---------------------------------------------------------------- API for jobs

    public DroneCargo cargo() {
        return cargo;
    }

    /** Uses the energy of one job (harvest, mined block), raised by the Fortune / Silk Touch addons. */
    public void useJobEnergy(int baseAmount) {
        useEnergy((int) Math.ceil(baseAmount * modules.jobCostMultiplier()));
    }

    public void useEnergy(int amount) {
        energy = Math.max(0, energy - amount);
    }

    public void markUnreachable(BlockPos pos) {
        unreachable.add(pos.immutable());
    }

    public boolean isUnreachable(BlockPos pos) {
        return unreachable.contains(pos);
    }

    /** True while the drone is parked on (or right above) its dock. */
    public boolean isAtDock() {
        if (dockPos == null) {
            return false;
        }
        Vec3 home = dockHover();
        return home.subtract(position()).horizontalDistance() < 1.0 && Math.abs(getY() - home.y) < TAXI_WAIT_HEIGHT + 0.5;
    }

    /** Sent only when it changes: ItemStack has no equals, so a plain set would resend it every tick. */
    private void syncCarriedItem() {
        ItemStack shown = cargo.firstItem();
        if (!ItemStack.matches(shown, entityData.get(DATA_CARRIED))) {
            entityData.set(DATA_CARRIED, shown);
        }
    }

    public ItemStack getCarriedItem() {
        return entityData.get(DATA_CARRIED);
    }

    /** Whether an Anchor Addon is fitted: its dock keeps the chunks it works in loaded. */
    public boolean isAnchored() {
        return modules.addonCount(AddonType.ANCHOR) > 0;
    }

    /** Items carried per Transport trip, from the Stack Addons. */
    public int transportCapacity() {
        return modules.transportCapacity();
    }

    /**
     * The virtual tool used to work out drops: a diamond pickaxe with Silk Touch or Fortune
     * from the installed addons. Crops use Fortune through their loot tables too.
     */
    public ItemStack miningTool(ServerLevel level) {
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (modules.hasSilkTouch()) {
            tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        } else if (modules.fortuneLevel() > 0) {
            tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), modules.fortuneLevel());
        }
        return tool;
    }

    public double cruiseSpeed() {
        return flight.cruiseSpeed();
    }

    /** Flies straight toward {@code target}; see {@link DroneFlight#flyTowards}. Call it every tick. */
    public void flyTowards(Vec3 target) {
        flight.flyTowards(target);
    }

    /** Whether no block stands on the straight line from the drone to {@code target}. */
    public boolean canFlyStraightTo(Vec3 target) {
        return flight.canFlyStraightTo(target);
    }

    // ---------------------------------------------------------------- AI

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        syncCarriedItem();
        if (landingSpot != null || entityData.get(DATA_SHUTDOWN) != 0) {
            tickPickup(level);
            return;
        }
        if (dockPos == null || !level.isLoaded(dockPos)) {
            return;
        }
        if (!(level.getBlockEntity(dockPos) instanceof DroneDockBlockEntity dock)) {
            // The dock was removed: turn back into an item so nothing is lost.
            dropAsItem(level);
            return;
        }

        float startup = startupTicks(0.0F);
        if (startup >= 0) {
            tickStartup((int) startup);
            dock.updateDroneStatus(getUUID(), level.getGameTime(), getStatus(), energy, capacity(), 0, modules.taskMask(), blockPosition());
            return;
        }
        int status = getStatus();
        // Return button: come home, but never drop a taxi passenger mid-air
        if (status == STATUS_WORKING && dock.isPaused() && !isVehicle()) {
            startReturn(level);
            status = STATUS_RETURNING;
        }
        if (status == STATUS_WORKING || status == STATUS_RETURNING) {
            useEnergy(modules.flightCost());
        }
        switch (status) {
            case STATUS_WORKING -> tickWorking(level, dock);
            case STATUS_RETURNING -> tickReturning();
            default -> tickDocked(level, dock);
        }
        dock.updateDroneStatus(getUUID(), level.getGameTime(), getStatus(), energy, capacity(),
                jobTask != null ? jobTask.ordinal() + 1 : 0, modules.taskMask(), blockPosition());
    }

    /** Where it parks: the middle of the pad, or on a swarm dock the corner of the pad it was given. */
    private Vec3 dockHover() {
        Vec3 pad = new Vec3(dockPos.getX() + 0.5, dockPos.getY() + DOCK_HEIGHT, dockPos.getZ() + 0.5);
        int corner = level().getBlockEntity(dockPos) instanceof SwarmDockBlockEntity swarm ? swarm.parkingSpot(getUUID()) : -1;
        return corner < 0 ? pad : pad.add((corner & 1) == 0 ? -CORNER_OFFSET : CORNER_OFFSET, 0.0, (corner & 2) == 0 ? -CORNER_OFFSET : CORNER_OFFSET);
    }

    private void tickDocked(ServerLevel level, DroneDockBlockEntity dock) {
        if (isWaitingForPassenger(dock)) {
            flyTowards(dockHover().add(0.0, TAXI_WAIT_HEIGHT, 0.0));
        } else {
            flight.flyHome(dockHover());
        }
        cargo.unloadInto(dock);

        int charged = dock.charge(capacity() - energy);
        energy += charged;
        setStatus(charged > 0 ? STATUS_CHARGING : STATUS_IDLE);

        // Finish charging while the dock can supply power, and wait while it is too full to take the cargo
        if (dock.isPaused() || (charged > 0 && energy < capacity()) || energy < departureCharge() || !cargo.isEmpty()) {
            return;
        }
        if (--scanCooldown > 0) {
            return;
        }
        scanCooldown = SCAN_INTERVAL;

        List<DroneTask> tasks = modules.tasks();
        for (int i = 0; i < tasks.size(); i++) {
            DroneTask task = tasks.get((nextTask + i) % tasks.size());
            DroneJob found = task.findJob(this, level, dock);
            if (found != null) {
                job = found;
                jobTask = task;
                nextTask = (tasks.indexOf(task) + 1) % tasks.size();
                setStatus(STATUS_WORKING);
                return;
            }
        }
    }

    private boolean isWaitingForPassenger(DroneDockBlockEntity dock) {
        return modules.tasks().contains(DroneTask.TAXI) && !dock.isPaused() && dock.getTaxiDestination() != null && energy >= departureCharge();
    }

    /** A player right-clicked the waiting Taxi drone: check the trip, then let them grab the hook. */
    private void startTaxi(ServerLevel level, Player player) {
        if (job != null || isVehicle() || dockPos == null || !(level.getBlockEntity(dockPos) instanceof DroneDockBlockEntity dock) || dock.isPaused()) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.taxi.busy"));
            return;
        }
        TaxiJob taxi = TaxiJob.plan(this, level, player, dock.getTaxiDestination(), energy, modules.flightCost());
        if (taxi != null && player.startRiding(this)) {
            job = taxi;
            jobTask = DroneTask.TAXI;
            setStatus(STATUS_WORKING);
            level.playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0F, 1.2F);
        }
    }

    private void tickWorking(ServerLevel level, DroneDockBlockEntity dock) {
        if (job == null || jobTask == null || energy < capacity() / 10) {
            startReturn(level);
            return;
        }
        switch (job.tick(this, level, dock)) {
            case CONTINUE -> {}
            case RETURN -> startReturn(level);
            case DONE -> {
                // Chain the next job of the same kind without flying home in between.
                job = jobTask.findJob(this, level, dock);
                if (job == null) {
                    startReturn(level);
                }
            }
        }
    }

    private void tickReturning() {
        if (flight.flyHome(dockHover())) {
            unreachable.clear();
            scanCooldown = SCAN_INTERVAL;
            setStatus(STATUS_IDLE);
        }
    }

    private void stopJob(ServerLevel level) {
        if (job != null) {
            job.stop(this, level);
        }
    }

    private void startReturn(ServerLevel level) {
        stopJob(level);
        job = null;
        jobTask = null;
        navigation.stop();
        setStatus(STATUS_RETURNING);
    }

    // ---------------------------------------------------------------- pickup and lifecycle

    public ItemStack toItem() {
        ItemStack stack = new ItemStack(ModItems.DRONE.get());
        stack.set(ModDataComponents.DRONE_MODULES.get(), modules);
        stack.set(ModDataComponents.ENERGY.get(), energy);
        return stack;
    }

    private void dropAsItem(ServerLevel level) {
        stopJob(level);
        spawnAtLocation(level, toItem());
        cargo.dropAll(stack -> spawnAtLocation(level, stack));
        discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown() && modules.tasks().contains(DroneTask.TAXI) && !isVehicle()) {
            if (level() instanceof ServerLevel level) {
                startTaxi(level, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty() || isVehicle()) {
            return super.mobInteract(player, hand);
        }
        if (level() instanceof ServerLevel level) {
            beginPickup(level, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        spawnAtLocation(level, toItem());
        cargo.dropAll(stack -> spawnAtLocation(level, stack));
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    /**
     * Nothing hurts a drone, creative players included (the vanilla invulnerable flag lets them
     * through). Only /kill and the void do; it then drops as an item, so nothing is lost.
     */
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /** A left-click goes through it: no hit, no knockback. Sneak + right-click picks it up. */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ---------------------------------------------------------------- taxi passenger

    /**
     * The passenger hangs from the hook below the drone instead of sitting on it. Vanilla lowers
     * every passenger by its own vehicle attachment (0.6 for players, made for sitting), so that
     * offset is added back to keep the hand right on the hook. The player is also moved sideways,
     * following where they turn, so the raised hand (not their head) is right under the hook.
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double sideX = 0.0;
        double sideZ = 0.0;
        if (passenger instanceof Player player) {
            // The player's right is (-cos, -sin) of their body yaw; the hand is on their main arm's side
            float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
            double side = player.getMainArm() == HumanoidArm.RIGHT ? HAND_SIDE : -HAND_SIDE;
            sideX = Mth.cos(yaw) * side;
            sideZ = Mth.sin(yaw) * side;
        }
        return new Vec3(sideX, -PASSENGER_DROP + passenger.getVehicleAttachmentPoint(this).y, sideZ);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().isEmpty();
    }

    // ---------------------------------------------------------------- save data

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("modules", DroneModules.CODEC, modules);
        output.putInt("energy", energy);
        output.storeNullable("dock", BlockPos.CODEC, dockPos);
        cargo.save(output.child("cargo"));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        modules = input.read("modules", DroneModules.CODEC).orElse(DroneModules.EMPTY);
        energy = input.getIntOr("energy", 0);
        dockPos = input.read("dock", BlockPos.CODEC).orElse(null);
        cargo.load(input.childOrEmpty("cargo"));
        entityData.set(DATA_TASKS, modules.taskMask());
        // Jobs are not saved: a reloaded drone flies home and starts over.
        setStatus(STATUS_RETURNING);
    }
}
