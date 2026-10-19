package com.cyberfarm.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
    /** Within this horizontal distance of the dock the drone stops pathfinding and lands by hand. */
    private static final double LANDING_RADIUS = 2.5;
    /** The drone lines up this high above the pad before descending. */
    private static final double APPROACH_HEIGHT = 0.8;
    /** A Taxi drone waits for passengers this high above its parking spot. */
    private static final double TAXI_WAIT_HEIGHT = 2.7;
    /** How far below the drone a hanging passenger's feet are. */
    public static final double PASSENGER_DROP = 2.5;
    /** Beyond this horizontal distance the drone climbs over the terrain before heading home. */
    private static final double LONG_TRIP = 16.0;
    public static final int CARGO_SLOTS = 9;
    private static final int SCAN_INTERVAL = 40;
    /** Cruise speed in blocks per tick of a 100% frame (3 blocks/s); the Basic frame flies at 80%. */
    private static final double BASE_SPEED = 0.15;
    /** Ticks pushing against a block before falling back to pathfinding... */
    private static final int STUCK_LIMIT = 20;
    /** ...and how long to pathfind before trying a straight line again. */
    private static final int PATHFIND_TICKS = 60;

    private static final EntityDataAccessor<Integer> DATA_STATUS = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TASKS = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);

    private DroneModules modules = DroneModules.EMPTY;
    private int energy;
    private @Nullable BlockPos dockPos;
    private @Nullable DroneJob job;
    private @Nullable DroneTask jobTask;
    /** Round robin over the modules, so one busy module does not starve the others. */
    private int nextTask;
    private int scanCooldown;
    private int stuckTicks;
    private double returnAltitude = Double.NaN;
    private int pathfindTicks;
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
                // Only used by the pathfinding fallback; normal flight is steered by hand.
                .add(Attributes.FLYING_SPEED, 0.3)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    public void setup(DroneModules modules, int energy, BlockPos dockPos) {
        this.modules = modules;
        this.energy = Math.min(energy, modules.energyCapacity());
        this.dockPos = dockPos.immutable();
        this.entityData.set(DATA_TASKS, modules.taskMask());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATUS, STATUS_IDLE);
        builder.define(DATA_TASKS, 0);
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

    // ---------------------------------------------------------------- API for jobs

    public ItemStacksResourceHandler getCargo() {
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

    public boolean isCargoFull() {
        for (int i = 0; i < cargo.size(); i++) {
            if (cargo.getResource(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int getCargoCount() {
        int count = 0;
        for (int i = 0; i < cargo.size(); i++) {
            count += cargo.getAmountAsInt(i);
        }
        return count;
    }

    public boolean isCargoEmpty() {
        return ResourceHandlerUtil.isEmpty(cargo);
    }

    /** True while the drone is parked on (or right above) its dock. */
    public boolean isAtDock() {
        if (dockPos == null) {
            return false;
        }
        Vec3 home = dockHover();
        return home.subtract(position()).horizontalDistance() < 1.0 && Math.abs(getY() - home.y) < TAXI_WAIT_HEIGHT + 0.5;
    }

    /** Removes and returns one item of the first cargo stack matching {@code filter}, or EMPTY. */
    public ItemStack takeOneFromCargo(Predicate<ItemStack> filter) {
        for (int i = 0; i < cargo.size(); i++) {
            ItemStack stack = ItemUtil.getStack(cargo, i);
            if (!stack.isEmpty() && filter.test(stack)) {
                cargo.set(i, ItemResource.of(stack), stack.getCount() - 1);
                return stack.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean cargoContains(Predicate<ItemStack> filter) {
        for (int i = 0; i < cargo.size(); i++) {
            ItemStack stack = ItemUtil.getStack(cargo, i);
            if (!stack.isEmpty() && filter.test(stack)) {
                return true;
            }
        }
        return false;
    }

    /** Puts drops in the cargo; whatever does not fit falls on the ground at {@code pos}. */
    public void collect(ServerLevel level, BlockPos pos, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack rest = storeInCargo(drop);
            if (!rest.isEmpty()) {
                Block.popResource(level, pos, rest);
            }
        }
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
        return BASE_SPEED * modules.speed();
    }

    /**
     * Flies in a straight line toward {@code target}, accelerating smoothly, easing off near it and
     * turning gradually. Call it every tick. Vanilla pathfinding re-plans between grid nodes and
     * zig-zags, so it is only used for a few seconds after the drone gets stuck against a block.
     */
    public void flyTowards(Vec3 target) {
        if (pathfindTicks > 0) {
            pathfindTicks--;
            if (tickCount % 10 == 0 || navigation.isDone()) {
                navigation.moveTo(target.x, target.y, target.z, modules.speed());
            }
            return;
        }
        navigation.stop();
        holdMoveControl();

        Vec3 delta = target.subtract(position());
        double distance = delta.length();
        if (distance < 0.02) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        double speed = Math.min(cruiseSpeed(), distance * 0.3);
        Vec3 desired = delta.scale(speed / distance);
        setDeltaMovement(getDeltaMovement().lerp(desired, 0.5));
        if (desired.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(desired.z, desired.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 8.0F));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }

        if (horizontalCollision || (verticalCollision && Math.abs(delta.y) > 0.5)) {
            if (++stuckTicks > STUCK_LIMIT) {
                stuckTicks = 0;
                pathfindTicks = PATHFIND_TICKS;
            }
        } else if (stuckTicks > 0) {
            stuckTicks--;
        }
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
            useEnergy(modules.flightCost());
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
            Vec3 approach = home.add(0.0, APPROACH_HEIGHT, 0.0);
            if (horizontal > LONG_TRIP && level() instanceof ServerLevel level) {
                // Long way home (e.g. after a taxi trip): climb over the terrain first, then cruise
                if (Double.isNaN(returnAltitude)) {
                    returnAltitude = TaxiJob.cruiseAltitude(level, position(), approach) - PASSENGER_DROP;
                }
                flyTowards(getY() < returnAltitude - 0.5
                        ? new Vec3(getX(), returnAltitude, getZ())
                        : new Vec3(home.x, returnAltitude, home.z));
            } else {
                flyTowards(approach);
            }
            return false;
        }

        returnAltitude = Double.NaN;
        pathfindTicks = 0;
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
        if (isWaitingForPassenger(dock)) {
            flyTowards(dockHover().add(0.0, TAXI_WAIT_HEIGHT, 0.0));
        } else {
            flyHome();
        }
        unloadCargo(dock);

        int charged = dock.charge(capacity() - energy);
        energy += charged;
        setStatus(charged > 0 ? STATUS_CHARGING : STATUS_IDLE);

        // Finish charging while the dock can supply power; never leave below 30%,
        // and wait while the dock is too full to take the cargo.
        if ((charged > 0 && energy < capacity()) || energy < capacity() * 3 / 10 || !isCargoEmpty()) {
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
        return modules.tasks().contains(DroneTask.TAXI) && dock.getTaxiDestination() != null && energy >= capacity() * 3 / 10;
    }

    /** A player right-clicked the waiting Taxi drone: check the trip, then let them grab the hook. */
    private void startTaxi(ServerLevel level, Player player) {
        if (job != null || isVehicle() || dockPos == null || !(level.getBlockEntity(dockPos) instanceof DroneDockBlockEntity dock)) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.taxi.busy"));
            return;
        }
        BlockPos destination = dock.getTaxiDestination();
        if (destination == null) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.taxi.no_destination"));
            return;
        }
        Vec3 dropOff = TaxiJob.dropOffPoint(destination);
        double cruiseY = TaxiJob.cruiseAltitude(level, position(), dropOff);
        // Up, across and down, there and back again
        double route = (cruiseY - getY()) + dropOff.subtract(position()).horizontalDistance() + (cruiseY - dropOff.y);
        int needed = (int) Math.ceil(2 * route / (cruiseSpeed() * 0.9) * modules.flightCost()) + TaxiJob.TRIP_COST;
        if (energy < needed) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.taxi.low_energy", String.format("%,d", needed)));
            return;
        }
        if (player.startRiding(this)) {
            job = new TaxiJob(destination, player.getUUID(), cruiseY);
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
        if (flyHome()) {
            unreachable.clear();
            scanCooldown = SCAN_INTERVAL;
            setStatus(STATUS_IDLE);
        }
    }

    private void startReturn(ServerLevel level) {
        if (job != null) {
            job.stop(this, level);
        }
        job = null;
        jobTask = null;
        navigation.stop();
        setStatus(STATUS_RETURNING);
    }

    // ---------------------------------------------------------------- cargo

    public ItemStack storeInCargo(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = ResourceHandlerUtil.insertStacking(cargo, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return stack.copyWithCount(stack.getCount() - inserted);
        }
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
        if (job != null) {
            job.stop(this, level);
        }
        spawnAtLocation(level, toItem());
        dropCargo(level);
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
            if (job != null) {
                job.stop(this, level);
            }
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

    // ---------------------------------------------------------------- taxi passenger

    /**
     * The passenger hangs from the hook below the drone instead of sitting on it. Vanilla lowers
     * every passenger by its own vehicle attachment (0.6 for players, made for sitting), so that
     * offset is added back to keep the hand right on the hook.
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, -PASSENGER_DROP + passenger.getVehicleAttachmentPoint(this).y, 0.0);
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
        cargo.serialize(output.child("cargo"));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        modules = input.read("modules", DroneModules.CODEC).orElse(DroneModules.EMPTY);
        energy = input.getIntOr("energy", 0);
        dockPos = input.read("dock", BlockPos.CODEC).orElse(null);
        cargo.deserialize(input.childOrEmpty("cargo"));
        entityData.set(DATA_TASKS, modules.taskMask());
        // Jobs are not saved: a reloaded drone flies home and starts over.
        setStatus(STATUS_RETURNING);
    }
}
