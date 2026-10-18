package com.cyberfarm.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.drone.DroneModules;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.drone.job.DroneJob;
import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
 * give it (harvest, quarry, transport), taking turns between modules, and brings everything back.
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
            flyTowards(home.add(0.0, APPROACH_HEIGHT, 0.0));
            return false;
        }

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
        flyHome();
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
        if (!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
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
