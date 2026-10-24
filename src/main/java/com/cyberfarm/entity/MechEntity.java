package com.cyberfarm.entity;

import java.util.UUID;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModItems;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * A pilotable mech, about 3.5 blocks tall. Right-click it and the front of its trunk opens like a
 * flower, the player backs into the bay and it closes around them; the exit key lets them out. Piloted, it
 * walks, jumps high and punches hard, all on its own battery. It never dies: at no integrity left it
 * shuts down and drops its pilot, to be repaired.
 *
 * <p>Like a horse, it moves on its pilot's client; the server keeps the battery, the integrity and
 * the boarding sequence, which every client sees through synced data.
 */
public class MechEntity extends Mob {
    public static final int CAPACITY = 500_000;
    /** A freshly built mech comes with this much charge. */
    public static final int FACTORY_CHARGE = 100_000;
    /** Battery drain per block moved, and the little the systems use while piloted. */
    private static final int FE_PER_BLOCK = 100;
    private static final int FE_IDLE = 1;
    public static final int FE_PER_PUNCH = 50;
    private static final double JUMP_VELOCITY = 0.95;
    private static final float WALK_SPEED = 0.22F;
    /** Reach the pilot gains, in blocks: the mech's arms are long. */
    private static final double EXTRA_REACH = 2.5;
    private static final Identifier REACH = CyberFarm.id("mech_reach");

    /**
     * Boarding sequence. The mech crouches a little and the front of its trunk opens like a flower;
     * the pilot walks up, turns round and backs into the bay, up a step, against the harness; the
     * petals close and the mech straightens. Getting out, the pilot steps forward out of the bay and
     * walks off. The pilot rides from BOARDING until UNBOARDING ends.
     */
    public enum State {
        IDLE(0), OPENING(30), BOARDING(44), CLOSING(30), PILOTED(0), OPENING_OUT(30), UNBOARDING(36), CLOSING_OUT(30),
        /** Folding up into its frame, to drop as an item; unfolding out of it once deployed. */
        FOLDING(60), UNFOLDING(50);

        public final int duration;

        State(int duration) {
            this.duration = duration;
        }

        /** How open the petals are: 0 shut, 1 wide open. */
        public float open(float progress) {
            return switch (this) {
                case OPENING, OPENING_OUT -> smooth(range(progress, 0.25F, 1.0F));
                case BOARDING, UNBOARDING -> 1.0F;
                case CLOSING, CLOSING_OUT -> 1.0F - smooth(range(progress, 0.0F, 0.55F));
                default -> 0.0F;
            };
        }

        /** How far down the mech crouches: 0 standing, 1 with the bay a step above the ground. */
        public float crouch(float progress) {
            return switch (this) {
                case OPENING, OPENING_OUT -> smooth(range(progress, 0.0F, 0.5F));
                case BOARDING, UNBOARDING -> 1.0F;
                case CLOSING, CLOSING_OUT -> 1.0F - smooth(range(progress, 0.45F, 1.0F));
                case FOLDING -> smooth(range(progress, 0.0F, 0.3F));
                case UNFOLDING -> 1.0F - smooth(range(progress, 0.65F, 1.0F));
                default -> 0.0F;
            };
        }

        /** How far folded up the mech is: 0 standing as usual, 1 packed into its frame. */
        public float fold(float progress) {
            return switch (this) {
                case FOLDING -> progress;
                case UNFOLDING -> 1.0F - progress;
                default -> 0.0F;
            };
        }
    }

    /** The bay's floor above the ground, standing and crouched (MechModel's floor and crouch); the pilot stands a bit back in it. */
    private static final double FLOOR = 1.875;
    private static final double FLOOR_CROUCHED = FLOOR - 14.5 / 16.0;
    /** Where the pilot stands in front of the open bay, and where they walk off to when getting out. */
    private static final Vec3 FRONT = new Vec3(0.0, 0.0, 1.5);
    private static final Vec3 EXIT = new Vec3(0.0, 0.0, 2.4);

    private static float range(float value, float from, float to) {
        return Mth.clamp((value - from) / (to - from), 0.0F, 1.0F);
    }

    private static final EntityDataAccessor<Integer> DATA_ENERGY = SynchedEntityData.defineId(MechEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(MechEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_STATE_TIME = SynchedEntityData.defineId(MechEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_DISABLED = SynchedEntityData.defineId(MechEntity.class, EntityDataSerializers.BOOLEAN);
    /** Installed modules, as a bit mask of {@link MechModule#bit()}. */
    private static final EntityDataAccessor<Integer> DATA_MODULES = SynchedEntityData.defineId(MechEntity.class, EntityDataSerializers.INT);
    /** The tools the pilot has switched on (drill, blade), as a bit mask of {@link MechModule#bit()}. */
    private static final EntityDataAccessor<Integer> DATA_TOOLS = SynchedEntityData.defineId(MechEntity.class, EntityDataSerializers.INT);
    /** Extra drain while the jet keeps the mech in the air, and how much of the damage the armour module stops. */
    private static final int FE_JET = 60;
    private static final float ARMOR_BLOCKS = 0.4F;
    /** Lift while the jump key is held in the air, and the fastest climb and glide. */
    private static final double JET_THRUST = 0.1;
    private static final double JET_CLIMB = 0.5;
    private static final double JET_GLIDE = -0.18;
    private static final double JET_DIVE = -0.6;
    /** How fast the modules' parts come out or go back, per tick. */
    private static final float DEPLOY_SPEED = 1.0F / 16.0F;
    /** Where the boarding pilot stood, in the mech's frame, so they walk up from there rather than jump to it. */
    private static final EntityDataAccessor<Vector3fc> DATA_BOARD_FROM = SynchedEntityData.defineId(MechEntity.class, EntityDataSerializers.VECTOR3);

    /**
     * The pilot's jump key, set by the client every tick. Movement runs on the pilot's client, so this
     * is only read there; a dedicated server never sets it.
     */
    public static boolean clientJump;
    /** The pilot's sneak key, likewise: with the jet it means going down. */
    public static boolean clientSneak;
    // Client side: how far each module's parts are out, eased towards what they should be
    private final float[] deploy = new float[MechModule.values().length];
    private final float[] deployO = new float[MechModule.values().length];

    private @Nullable UUID boardingPlayer;
    /** Set while the mech itself lets its pilot out, so the dismount is not blocked. */
    private boolean releasing;
    private @Nullable Vec3 lastPosition;

    public MechEntity(EntityType<? extends MechEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.1)
                .add(Attributes.SAFE_FALL_DISTANCE, 64.0)
                .add(Attributes.CAMERA_DISTANCE, 9.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ENERGY, 0);
        builder.define(DATA_STATE, State.IDLE.ordinal());
        builder.define(DATA_STATE_TIME, 0L);
        builder.define(DATA_DISABLED, false);
        builder.define(DATA_MODULES, 0);
        builder.define(DATA_TOOLS, 0);
        builder.define(DATA_BOARD_FROM, new Vector3f(0.0F, 0.0F, 1.5F));
    }

    // ------------------------------------------------------------------ battery and integrity

    public int getEnergy() {
        return entityData.get(DATA_ENERGY);
    }

    public void setEnergy(int energy) {
        entityData.set(DATA_ENERGY, Mth.clamp(energy, 0, CAPACITY));
    }

    /** Takes {@code amount} FE if the battery has it all. */
    public boolean consume(int amount) {
        if (getEnergy() < amount) {
            return false;
        }
        setEnergy(getEnergy() - amount);
        return true;
    }

    public boolean isDisabled() {
        return entityData.get(DATA_DISABLED);
    }

    public int getModules() {
        return entityData.get(DATA_MODULES);
    }

    public void setModules(int modules) {
        entityData.set(DATA_MODULES, modules);
    }

    public boolean hasModule(MechModule module) {
        return (getModules() & module.bit()) != 0;
    }

    /** Whether the pilot has the drill or the blade switched on (and it is fitted). */
    public boolean isToolOn(MechModule module) {
        return hasModule(module) && (entityData.get(DATA_TOOLS) & module.bit()) != 0;
    }

    /** The pilot's key for the drill or the blade: out it comes, or back it goes. */
    public void toggleTool(MechModule module) {
        if ((module == MechModule.DRILL || module == MechModule.BLADE) && hasModule(module) && isPiloted()
                && level() instanceof ServerLevel level) {
            entityData.set(DATA_TOOLS, entityData.get(DATA_TOOLS) ^ module.bit());
            boolean on = isToolOn(module);
            level.playSound(null, getX(), getY() + 2, getZ(), on ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                    SoundSource.NEUTRAL, 0.7F, module == MechModule.DRILL ? 0.8F : 1.4F);
        }
    }

    /** Whether a module's parts should be out: with the pilot inside, the jet and armour always, the tools when on. */
    private boolean shouldDeploy(MechModule module) {
        if (!hasModule(module) || getState() != State.PILOTED) {
            return false;
        }
        return module == MechModule.JET || module == MechModule.ARMOR || isToolOn(module);
    }

    /** Client side: how far a module's parts are out, from 0 stowed to 1 out. */
    public float deployAmount(MechModule module, float partialTick) {
        int i = module.ordinal();
        return Mth.lerp(partialTick, deployO[i], deploy[i]);
    }

    /** Whether the jet can fly the mech now: fitted, its wings out, power in the battery. */
    private boolean jetReady() {
        return hasModule(MechModule.JET) && getState() == State.PILOTED && getEnergy() > 0;
    }

    /** A hangar's repair: back to full integrity it comes back on. */
    public void repair(float amount) {
        setHealth(Math.min(getMaxHealth(), getHealth() + amount));
        if (isDisabled() && getHealth() >= getMaxHealth()) {
            entityData.set(DATA_DISABLED, false);
            level().playSound(null, getX(), getY() + 2, getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8F, 1.2F);
        }
    }

    /** Parked and empty: what a hangar can charge, repair and fit modules to. */
    public boolean isParked() {
        return getState() == State.IDLE && !isVehicle() && isAlive();
    }

    /** Taking damage never kills it: whatever would, shuts it down and drops the pilot instead. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableTo(level, source) || isDisabled()) {
            return false;
        }
        if (hasModule(MechModule.ARMOR)) {
            amount *= 1.0F - ARMOR_BLOCKS;
        }
        if (getHealth() - amount > 1.0F) {
            return super.hurtServer(level, source, amount);
        }
        setHealth(1.0F);
        entityData.set(DATA_DISABLED, true);
        level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 1.0F, 0.6F);
        if (getFirstPassenger() instanceof Player pilot) {
            release(level, pilot);
        }
        setState(State.IDLE);
        return true;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    // ------------------------------------------------------------------ boarding

    public State getState() {
        return State.values()[Mth.clamp(entityData.get(DATA_STATE), 0, State.values().length - 1)];
    }

    private void setState(State state) {
        entityData.set(DATA_STATE, state.ordinal());
        entityData.set(DATA_STATE_TIME, level().getGameTime());
    }

    /** How far into the current state, from 0 to 1 (always 1 for states without a duration). */
    public float stateProgress(float partialTick) {
        State state = getState();
        if (state.duration <= 0) {
            return 1.0F;
        }
        float elapsed = level().getGameTime() - entityData.get(DATA_STATE_TIME) + partialTick;
        return Mth.clamp(elapsed / state.duration, 0.0F, 1.0F);
    }

    /** How open the chest is now, for the renderer. */
    public float openAmount(float partialTick) {
        return getState().open(stateProgress(partialTick));
    }

    /** How far folded up the mech is now, for the renderer. */
    public float foldAmount(float partialTick) {
        return getState().fold(stateProgress(partialTick));
    }

    /** Starts folding a parked mech back into its frame; it drops as an item when done. */
    public void startFold() {
        if (isParked() && level() instanceof ServerLevel level) {
            setState(State.FOLDING);
            level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 1.0F, 0.5F);
            level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.6F, 1.4F);
        }
    }

    /** A mech just deployed from its frame unfolds before it can be used. */
    public void startUnfold() {
        setState(State.UNFOLDING);
        level().playSound(null, getX(), getY() + 2, getZ(), SoundEvents.PISTON_EXTEND, SoundSource.NEUTRAL, 1.0F, 0.5F);
    }

    /** The frame a folded mech drops as: battery charge and modules kept. */
    public ItemStack asFrame() {
        ItemStack frame = new ItemStack(ModItems.MECH_FRAME.get());
        frame.set(ModDataComponents.ENERGY.get(), getEnergy());
        frame.set(ModDataComponents.MECH_MODULES.get(), getModules());
        return frame;
    }

    /** How far down the mech crouches now, for the renderer. */
    public float crouchAmount(float partialTick) {
        return getState().crouch(stateProgress(partialTick));
    }

    /** The pilot's feet in the bay, which rise as the mech straightens up. */
    private Vec3 bay(float progress) {
        return new Vec3(0.0, Mth.lerp(getState().crouch(progress), FLOOR, FLOOR_CROUCHED), -0.08);
    }

    /**
     * Where the pilot's feet are, in the mech's frame (+z in front), at {@code progress} through the
     * state: walking up to it, backing into the bay up a step, in the bay, and stepping back out.
     */
    public Vec3 pilotOffset(float progress) {
        State state = getState();
        Vec3 bay = bay(progress);
        if (state == State.OPENING) {
            // The rider can arrive on a client a tick before the BOARDING state does
            Vector3fc from = entityData.get(DATA_BOARD_FROM);
            return new Vec3(from.x(), 0.0, from.z());
        }
        if (state == State.BOARDING) {
            if (progress < 0.35F) {
                Vector3fc from = entityData.get(DATA_BOARD_FROM);
                return new Vec3(from.x(), 0.0, from.z()).lerp(FRONT, smooth(range(progress, 0.0F, 0.35F)));
            }
            if (progress < 0.5F) {
                return FRONT;
            }
            // Backing in: the step up comes in the middle of the move
            float back = range(progress, 0.5F, 0.92F);
            Vec3 across = FRONT.lerp(bay, smooth(back));
            return new Vec3(across.x, bay.y * smooth(range(back, 0.3F, 0.75F)), across.z);
        }
        if (state == State.UNBOARDING) {
            // Off the harness, a stride forward down the step, then walking off
            if (progress < 0.15F) {
                return bay.lerp(bay.add(0.0, 0.0, 0.25), smooth(range(progress, 0.0F, 0.15F)));
            }
            if (progress < 0.55F) {
                float out = range(progress, 0.15F, 0.55F);
                Vec3 edge = bay.add(0.0, 0.0, 0.25);
                Vec3 across = edge.lerp(FRONT, out);
                // A stride: up a little as the foot leaves the edge, then down onto the ground
                double y = bay.y * (1.0 - smooth(range(out, 0.2F, 0.85F))) + 0.12 * Mth.sin(out * Mth.PI);
                return new Vec3(across.x, y, across.z);
            }
            return FRONT.lerp(EXIT, range(progress, 0.55F, 1.0F));
        }
        return bay;
    }

    /**
     * Which way the pilot faces, in degrees from the way the mech faces: 180 while walking up to
     * it, turning round in front of the bay, 0 once they back in and while they get out.
     */
    public float pilotFacing(float progress) {
        if (getState() == State.BOARDING) {
            return 180.0F * (1.0F - smooth(range(progress, 0.35F, 0.5F)));
        }
        return 0.0F;
    }

    /** Whether the pilot is walking at {@code progress}: up to the mech, backing in, stepping out. */
    public boolean isPilotWalking(float progress) {
        return switch (getState()) {
            case BOARDING -> progress < 0.35F || progress > 0.5F && progress < 0.92F;
            case UNBOARDING -> progress > 0.15F;
            default -> false;
        };
    }

    /**
     * Whether the pilot can be seen: from the moment the petals start to open until they shut again
     * (shut, they hide the pilot standing in the bay).
     */
    public boolean isPilotVisible(float progress) {
        State state = getState();
        return switch (state) {
            case OPENING, BOARDING, UNBOARDING -> true;
            case CLOSING, OPENING_OUT -> state.open(progress) > 0.02F;
            default -> false;
        };
    }

    /** Whether the pilot's eyes are inside the mech's head: the first-person view then leaves the head and trunk out. */
    public boolean isPilotInside(float progress) {
        return switch (getState()) {
            case BOARDING -> progress > 0.8F;
            case UNBOARDING -> progress < 0.25F;
            case CLOSING, PILOTED, OPENING_OUT -> true;
            default -> false;
        };
    }

    public boolean isPiloted() {
        return getState() == State.PILOTED && getFirstPassenger() instanceof Player;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (getState() != State.IDLE || isVehicle() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (isDisabled()) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.mech.disabled"));
            return InteractionResult.SUCCESS;
        }
        if (level() instanceof ServerLevel level) {
            boardingPlayer = player.getUUID();
            setState(State.OPENING);
            level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 1.0F, 0.6F);
            level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.PISTON_EXTEND, SoundSource.NEUTRAL, 0.7F, 0.7F);
        }
        return InteractionResult.SUCCESS;
    }

    /** The pilot asked to get out (the exit key). */
    public void requestExit(Player player) {
        if (getFirstPassenger() == player && getState() == State.PILOTED && level() instanceof ServerLevel level) {
            setState(State.OPENING_OUT);
            level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 1.0F, 0.6F);
            level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.PISTON_EXTEND, SoundSource.NEUTRAL, 0.7F, 0.7F);
        }
    }

    /** Whether the pilot's dismount is the mech letting them out (otherwise sneaking does not get you out). */
    public boolean isReleasing() {
        return releasing;
    }

    private void release(ServerLevel level, Player pilot) {
        releasing = true;
        pilot.stopRiding();
        releasing = false;
        Vec3 front = frontGround();
        pilot.teleportTo(front.x, front.y, front.z);
    }

    /** The ground in front of the mech, where the pilot walks off to. */
    private Vec3 frontGround() {
        return position().add(Vec3.directionFromRotation(0.0F, getYRot()).scale(EXIT.z));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            // Tools go back when nobody is at the controls
            if (getState() != State.PILOTED && entityData.get(DATA_TOOLS) != 0) {
                entityData.set(DATA_TOOLS, 0);
            }
        } else {
            for (MechModule module : MechModule.values()) {
                int i = module.ordinal();
                deployO[i] = deploy[i];
                deploy[i] = Mth.clamp(deploy[i] + (shouldDeploy(module) ? DEPLOY_SPEED : -DEPLOY_SPEED), 0.0F, 1.0F);
            }
        }
        if (level() instanceof ServerLevel level) {
            serverTick(level);
        } else if (getState() == State.OPENING || getState() == State.OPENING_OUT) {
            // A puff of steam from the seams as the petals unlock
            long elapsed = level().getGameTime() - entityData.get(DATA_STATE_TIME);
            if (elapsed == 8) {
                Vec3 front = Vec3.directionFromRotation(0.0F, getYRot());
                for (int i = 0; i < 14; i++) {
                    double side = (random.nextDouble() - 0.5) * 1.0;
                    double up = 1.2 + random.nextDouble() * 1.5;
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD,
                            getX() + front.x * 0.4 + front.z * side, getY() + up, getZ() + front.z * 0.4 - front.x * side,
                            front.x * 0.05, 0.02, front.z * 0.05);
                }
            }
        }
    }

    private void serverTick(ServerLevel level) {
        State state = getState();
        boolean done = state.duration > 0 && level.getGameTime() - entityData.get(DATA_STATE_TIME) >= state.duration;
        Player pilot = getFirstPassenger() instanceof Player player ? player : null;
        switch (state) {
            case OPENING -> {
                if (done) {
                    Player player = boardingPlayer != null ? level.getPlayerByUUID(boardingPlayer) : null;
                    if (player != null && player.isAlive() && player.distanceTo(this) < 6.0F && !player.isPassenger()) {
                        // In the mech's frame: +z in front, the way pilotOffset reads it
                        Vec3 local = player.position().subtract(position()).yRot(getYRot() * Mth.DEG_TO_RAD);
                        entityData.set(DATA_BOARD_FROM, new Vector3f((float) local.x, 0.0F, (float) local.z));
                    }
                    if (player != null && player.isAlive() && player.distanceTo(this) < 6.0F && !player.isPassenger()
                            && player.startRiding(this, true, true)) {
                        setState(State.BOARDING);
                    } else {
                        setState(State.CLOSING_OUT);
                    }
                    boardingPlayer = null;
                }
            }
            case BOARDING -> {
                if (done) {
                    setState(State.CLOSING);
                    level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 1.0F, 0.6F);
                    level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 0.7F, 0.7F);
                }
            }
            case CLOSING -> {
                if (done) {
                    setState(pilot != null ? State.PILOTED : State.IDLE);
                    if (pilot != null) {
                        level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8F, 1.4F);
                    }
                }
            }
            case OPENING_OUT -> {
                if (done) {
                    setState(State.UNBOARDING);
                }
            }
            case UNBOARDING -> {
                if (done) {
                    if (pilot != null) {
                        release(level, pilot);
                    }
                    setState(State.CLOSING_OUT);
                    level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 1.0F, 0.6F);
                }
            }
            case CLOSING_OUT, UNFOLDING -> {
                if (done) {
                    setState(State.IDLE);
                }
            }
            case FOLDING -> {
                if (done) {
                    // Packed: it drops as its frame, with a puff where it stood
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, getX(), getY() + 0.6, getZ(), 16, 0.5, 0.3, 0.5, 0.02);
                    level.playSound(null, getX(), getY() + 0.5, getZ(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.NEUTRAL, 1.0F, 0.6F);
                    spawnAtLocation(level, asFrame(), 0.5F);
                    discard();
                    return;
                }
            }
            case PILOTED -> {
                // The pilot got out some other way (death, a command): close up behind them
                if (pilot == null) {
                    setState(State.CLOSING_OUT);
                }
            }
            default -> {
                // Loaded from a save with its pilot still inside
                if (pilot != null) {
                    setState(State.PILOTED);
                }
            }
        }
        // Battery: what the systems use, plus every block the pilot walks or jumps it
        Vec3 now = position();
        if (isPiloted()) {
            double moved = lastPosition != null ? now.distanceTo(lastPosition) : 0.0;
            int cost = FE_IDLE + (int) Math.ceil(moved * FE_PER_BLOCK) + (jetReady() && !onGround() ? FE_JET : 0);
            setEnergy(getEnergy() - cost);
        }
        lastPosition = now;
    }

    // ------------------------------------------------------------------ piloting

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return isPiloted() && getFirstPassenger() instanceof Player pilot ? pilot : null;
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        setRot(controller.getYRot(), controller.getXRot() * 0.5F);
        yRotO = yBodyRot = yHeadRot = getYRot();
        // Movement is simulated on the pilot's client: that is where the jump and sneak keys are read
        if (level().isClientSide() && getEnergy() > 0) {
            Vec3 motion = getDeltaMovement();
            if (clientJump && onGround()) {
                setDeltaMovement(motion.x, JUMP_VELOCITY, motion.z);
            } else if (!onGround() && jetReady()) {
                // The jet: hold jump to climb, sneak to dive, neither to glide
                double y = clientJump ? Math.min(motion.y + JET_THRUST, JET_CLIMB)
                        : clientSneak ? Math.max(motion.y, JET_DIVE) : Math.max(motion.y, JET_GLIDE);
                setDeltaMovement(motion.x, y, motion.z);
                resetFallDistance();
            }
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float strafe = controller.xxa * 0.6F;
        float forward = controller.zza;
        return new Vec3(strafe, 0.0, forward < 0 ? forward * 0.5F : forward);
    }

    /** In the air the jet keeps the mech moving as fast as it walks; without it, it barely steers while it falls. */
    @Override
    protected float getFlyingSpeed() {
        return jetReady() ? 0.035F : super.getFlyingSpeed();
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return getEnergy() > 0 ? (float) getAttributeValue(Attributes.MOVEMENT_SPEED) : 0.0F;
    }

    /**
     * The bay, or wherever the pilot is on their way in or out. A rider sits lower than the point it
     * is attached to (by its vehicle attachment): that is added back, so their feet are where
     * pilotOffset says.
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        Vec3 feet = pilotOffset(stateProgress(0.0F)).yRot(-getYRot() * Mth.DEG_TO_RAD);
        return feet.add(0.0, passenger.getVehicleAttachmentPoint(this).y, 0.0);
    }

    /** Its pilot looks out from inside it: the crosshair must not land on the mech itself. */
    @Override
    public boolean isPickable() {
        return super.isPickable() && !isVehicle();
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (passenger instanceof Player pilot && !level().isClientSide()) {
            reach(pilot, Attributes.BLOCK_INTERACTION_RANGE, true);
            reach(pilot, Attributes.ENTITY_INTERACTION_RANGE, true);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player pilot && !level().isClientSide()) {
            reach(pilot, Attributes.BLOCK_INTERACTION_RANGE, false);
            reach(pilot, Attributes.ENTITY_INTERACTION_RANGE, false);
        }
    }

    private static void reach(Player pilot, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, boolean add) {
        AttributeInstance instance = pilot.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (add) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(REACH, EXTRA_REACH, AttributeModifier.Operation.ADD_VALUE));
        } else {
            instance.removeModifier(REACH);
        }
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("energy", getEnergy());
        output.putBoolean("disabled", isDisabled());
        output.putInt("modules", getModules());
        if (boardingPlayer != null) {
            output.store("boarding", UUIDUtil.CODEC, boardingPlayer);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setEnergy(input.getIntOr("energy", 0));
        entityData.set(DATA_DISABLED, input.getBooleanOr("disabled", false));
        setModules(input.getIntOr("modules", 0));
        // A mech saved halfway through boarding comes back closed and empty, or with its pilot inside
        entityData.set(DATA_STATE, State.IDLE.ordinal());
    }
}
