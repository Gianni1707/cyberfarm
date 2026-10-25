package com.cyberfarm.block.entity;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.IntakeGateBlock;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.item.MobCapsuleItem;
import com.cyberfarm.menu.MethaneExtractorMenu;
import com.cyberfarm.multiblock.Blueprint;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;

import com.cyberfarm.fluid.TankGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

/**
 * Methane Extractor: the "shower head" on top of a 3x3x4 chamber. It sprays Acid on the mob shut
 * inside and draws Methane while the mob slowly dissolves: zombies give the most, other monsters
 * less, animals little. Without acid nothing happens; once dissolved, the mob is gone. The chamber is
 *
 * <pre>
 *   roof  (y)    : 8 Cyber Frames around this extractor
 *   walls (y-1, y-2): Cyber Frame corners, Containment Glass in the middle of each side, empty centre
 *   floor (y-3)  : 3x3 Cyber Frames
 * </pre>
 *
 * Any Cyber Frame may be an Acid Inlet (acid in) or a Methane Outlet (methane pushed out), the only
 * ways fluids reach the chamber. Any glass of the lower wall may be an Intake Gate: while the chamber
 * is empty, the first monster that walks up to a gate is pulled in.
 */
public class MethaneExtractorBlockEntity extends MachineBlockEntity {
    public static final int TANK_CAPACITY = 8_000;
    public static final int ZOMBIE_RATE = 25;
    public static final int MONSTER_RATE = 12;
    public static final int ANIMAL_RATE = 4;
    private static final int OUTPUT_PER_SIDE = 200;
    private static final int CHECK_INTERVAL = 20;
    private static final int WORK_SOUND_INTERVAL = 50;
    /** Acid sprayed per tick while a mob is dissolving. */
    public static final int ACID_PER_TICK = 1;
    /** Ticks per point of health the acid eats: a zombie (20) lasts 5 minutes. */
    public static final int DISSOLVE_INTERVAL = 300;
    private static final int SYNC_INTERVAL = 10;
    /** Ticks between two puffs of the mob's slow decay: acid fumes and bits of it falling off. */
    private static final int DECAY_INTERVAL = 5;
    private static final DustParticleOptions ACID_FUMES = new DustParticleOptions(0x9BE04A, 1.0F);
    private static final ItemParticleOption FLESH = new ItemParticleOption(ParticleTypes.ITEM, Items.ROTTEN_FLESH);
    /** Clients hear the acid level at least this often, even when it does not change. */
    private static final int RESYNC_INTERVAL = 100;

    private final GasTank tank = new GasTank(TANK_CAPACITY, resource -> false, this::setChanged);
    private final GasTank acid = new GasTank(TANK_CAPACITY, resource -> resource.is(ModFluids.ACID_TYPE.get()), this::setChanged);
    private final TankGroup acidInlet = TankGroup.of(new GasTank[] {acid});
    private final TankGroup methaneOutlet = TankGroup.of(new GasTank[0], tank);
    /** Methane Outlets and Intake Gates of the formed chamber, found at every structure check. */
    private List<BlockPos> outlets = List.of();
    private List<BlockPos> gates = List.of();
    /** Network id of the mob inside, for the GUI to show it; 0 when empty. */
    private int mobId;
    /** Acid level the clients last heard, so the pool inside the chamber stays up to date. */
    private int syncedAcid = -1;
    /**
     * Acid at the last sample: more now means a pipe or bucket is filling the chamber. Sampled every
     * {@link #SYNC_INTERVAL} ticks, so a pipe only topping up what the mob uses does not count.
     */
    private int sampledAcid;
    /** Shown on clients as acid pouring from the extractor into the pool. */
    private boolean filling;
    /** Whether a mob is being dissolved into methane right now. */
    private boolean extracting;
    // Client side: the pool level drawn, easing towards the real one
    private float shownAcid = -1;
    private long shownTime;
    private boolean formed;
    private int mobCount;
    private int rate;
    private int checkCooldown;

    public MethaneExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METHANE_EXTRACTOR.get(), pos, state, 0, 0, 0, 0);
    }

    public GasTank getTank() {
        return tank;
    }

    /** What an Acid Inlet of the chamber gives pipes and buckets: acid in only. */
    public TankGroup getAcidInlet() {
        return acidInlet;
    }

    /** What a Methane Outlet of the chamber gives pipes and buckets: methane out only. */
    public TankGroup getMethaneOutlet() {
        return methaneOutlet;
    }

    /** The formed extractor whose chamber {@code pos} belongs to, or null. */
    public static @Nullable MethaneExtractorBlockEntity ofChamberBlock(Level level, BlockPos pos) {
        // The chamber spans one block around the extractor and three below it
        for (BlockPos top : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 3, 1))) {
            BlockState state = level.getBlockState(top);
            if (state.getBlock() instanceof MethaneExtractorBlock && state.getValue(MethaneExtractorBlock.FORMED)
                    && level.getBlockEntity(top) instanceof MethaneExtractorBlockEntity extractor) {
                return extractor;
            }
        }
        return null;
    }

    /** Every block of the chamber's shell under {@code top}, the extractor included. */
    public static Iterable<BlockPos> chamberBlocks(BlockPos top) {
        return BlockPos.betweenClosed(top.offset(-1, -3, -1), top.offset(1, 0, 1));
    }

    public GasTank getAcid() {
        return acid;
    }

    /** Checks the 3x3x4 chamber below an extractor sitting at {@code top}, as laid out by its {@link Blueprint}. */
    public static boolean isStructureValid(Level level, BlockPos top) {
        return Blueprint.METHANE_CHAMBER.ghosts(top, Direction.NORTH).stream().allMatch(ghost -> ghost.ok().test(level.getBlockState(ghost.pos())));
    }

    /** Hides (or shows again) the shell of the chamber under {@code top}, drawn whole by its renderer while formed. */
    private static void setFramesFormed(Level level, BlockPos top, boolean formed) {
        for (BlockPos pos : chamberBlocks(top)) {
            BlockState part = level.getBlockState(pos);
            if (!(part.getBlock() instanceof MethaneExtractorBlock) && part.hasProperty(CyberFrameBlock.FORMED)
                    && part.getValue(CyberFrameBlock.FORMED) != formed) {
                level.setBlock(pos, part.setValue(CyberFrameBlock.FORMED, formed), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Points the way out of the chamber from a block of its wall, seen from the extractor at {@code top}. */
    public static Direction outward(BlockPos top, BlockPos wall) {
        int dx = wall.getX() - top.getX();
        int dz = wall.getZ() - top.getZ();
        return dx != 0 ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    /** Pulls the first monster standing in front of an Intake Gate into the empty chamber. */
    private void intake(ServerLevel level) {
        for (BlockPos gate : gates) {
            BlockPos front = gate.relative(outward(worldPosition, gate));
            for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(front).inflate(0.4, 0.0, 0.4).expandTowards(0.0, 1.0, 0.0),
                    MethaneExtractorBlockEntity::canPullIn)) {
                level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.5, mob.getZ(), 8, 0.2, 0.4, 0.2, 0.02);
                mob.getNavigation().stop();
                mob.teleportTo(worldPosition.getX() + 0.5, worldPosition.getY() - 2, worldPosition.getZ() + 0.5);
                mob.setDeltaMovement(Vec3.ZERO);
                mob.setPersistenceRequired();
                IntakeGateBlock.open(level, gate, level.getBlockState(gate));
                level.playSound(null, gate, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
                level.playSound(null, gate, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6F, 1.2F);
                return;
            }
        }
    }

    /** Monsters only, and never bosses: the gate is meant for zombies and their kind. */
    private static boolean canPullIn(Mob mob) {
        return mob.isAlive() && mob instanceof Enemy && !mob.isPassenger() && !mob.isVehicle()
                && !mob.getType().builtInRegistryHolder().is(Tags.EntityTypes.BOSSES);
    }

    /** The 1x1x2 space in the middle of the chamber. */
    private AABB chamber() {
        return new AABB(worldPosition.getX(), worldPosition.getY() - 2, worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() + 1);
    }

    private List<Mob> mobsInside(Level level) {
        return level.getEntitiesOfClass(Mob.class, chamber(), Entity::isAlive);
    }

    public static int rateOf(Mob mob) {
        if (mob.getType().builtInRegistryHolder().is(EntityTypeTags.ZOMBIES)) {
            return ZOMBIE_RATE;
        }
        if (mob instanceof Enemy) {
            return MONSTER_RATE;
        }
        return mob instanceof Animal ? ANIMAL_RATE : 0;
    }

    public boolean isFilling() {
        return filling;
    }

    /** Client side: the acid level to draw, from 0 to 1, rising and falling smoothly. */
    public float shownAcid(float time) {
        float target = Math.min(1.0F, (float) acid.amount() / TANK_CAPACITY);
        if (shownAcid < 0) {
            shownAcid = target;
        }
        float elapsed = Math.max(0.0F, time - shownTime);
        shownTime = (long) time;
        shownAcid += (target - shownAcid) * Math.min(1.0F, elapsed * 0.08F);
        return shownAcid;
    }

    @Override
    protected boolean isWorking() {
        return extracting;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean wasFilling = filling;
        if (level.getGameTime() % SYNC_INTERVAL == 0) {
            filling = formed && acid.amount() > sampledAcid;
            sampledAcid = acid.amount();
        }
        if (--checkCooldown <= 0) {
            checkCooldown = CHECK_INTERVAL;
            formed = isStructureValid(level, pos);
            boolean changed = state.getValue(MethaneExtractorBlock.FORMED) != formed;
            if (changed) {
                level.setBlock(pos, state.setValue(MethaneExtractorBlock.FORMED, formed), Block.UPDATE_ALL);
                level.playSound(null, pos, (formed ? ModSounds.EXTRACTOR_ACTIVATE : ModSounds.EXTRACTOR_DEACTIVATE).get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            // Re-applied while formed, so a frame shared with a broken neighbour chamber lights up again
            if (formed || changed) {
                setFramesFormed(level, pos, formed);
            }
            if (changed) {
                // The ports' pipes must look their capabilities up again
                for (BlockPos part : chamberBlocks(pos)) {
                    level.invalidateCapabilities(part.immutable());
                }
            }
            findPorts(level);
            List<Mob> mobs = formed ? mobsInside(level) : List.of();
            if (formed && mobs.isEmpty()) {
                intake(level);
                mobs = mobsInside(level);
            }
            mobCount = mobs.size();
            mobId = mobs.isEmpty() ? 0 : mobs.getFirst().getId();
            rate = mobs.stream().mapToInt(MethaneExtractorBlockEntity::rateOf).sum();
        }
        // The acid eats the mob away while its gas is drawn; a full methane tank stops it all
        extracting = rate > 0 && acid.amount() >= ACID_PER_TICK && tank.amount() + rate <= tank.capacity();
        if (extracting) {
            acid.drain(ACID_PER_TICK);
            if (tank.fill(ModFluids.METHANE.get(), rate) > 0 && level.getGameTime() % WORK_SOUND_INTERVAL == 0) {
                level.playSound(null, pos.below(2), ModSounds.EXTRACTOR_WORK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            if (level.getGameTime() % DISSOLVE_INTERVAL == 0) {
                dissolve(level);
            }
            if (level.getGameTime() % DECAY_INTERVAL == 0) {
                decay(level);
            }
        }
        for (BlockPos outlet : outlets) {
            tank.pushToNeighbors(level, outlet, OUTPUT_PER_SIDE);
        }
        if (filling != wasFilling || level.getGameTime() % SYNC_INTERVAL == 0 && acid.amount() != syncedAcid
                || level.getGameTime() % RESYNC_INTERVAL == 0) {
            syncedAcid = acid.amount();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void findPorts(Level level) {
        List<BlockPos> foundOutlets = new ArrayList<>();
        List<BlockPos> foundGates = new ArrayList<>();
        if (formed) {
            for (BlockPos part : chamberBlocks(worldPosition)) {
                BlockState state = level.getBlockState(part);
                if (state.is(ModBlocks.METHANE_OUTLET.get())) {
                    foundOutlets.add(part.immutable());
                } else if (state.is(ModBlocks.INTAKE_GATE.get())) {
                    foundGates.add(part.immutable());
                }
            }
        }
        outlets = foundOutlets;
        gates = foundGates;
    }

    /**
     * How far the acid has eaten the mob, from 0 (just in) to 1 (gone): its lost health, plus the
     * part of the current point already eaten, so it moves on smoothly between two points.
     */
    public static float decayOf(Mob mob, long gameTime) {
        float eaten = (float) (gameTime % DISSOLVE_INTERVAL) / DISSOLVE_INTERVAL;
        return Mth.clamp(1.0F - (mob.getHealth() - eaten) / Math.max(1.0F, mob.getMaxHealth()), 0.0F, 1.0F);
    }

    /**
     * The acid eats the mob away: green fumes, scraps of skin and goo dropping off it, more the less
     * is left. Its skin falling apart is drawn on clients by DissolvingMobRenderer.
     */
    private void decay(ServerLevel level) {
        for (Mob mob : mobsInside(level)) {
            float decay = decayOf(mob, level.getGameTime());
            int fumes = 1 + Math.round(decay * 4);
            level.sendParticles(ACID_FUMES, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), fumes,
                    mob.getBbWidth() * 0.4, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.4, 0.0);
            if (level.getRandom().nextFloat() < 0.15F + decay * 0.5F) {
                level.sendParticles(FLESH, mob.getX(), mob.getY() + mob.getBbHeight() * (0.3 + 0.6 * level.getRandom().nextFloat()), mob.getZ(),
                        1, mob.getBbWidth() * 0.3, 0.1, mob.getBbWidth() * 0.3, 0.02);
                level.sendParticles(ParticleTypes.ITEM_SLIME, mob.getX(), mob.getY() + mob.getBbHeight() * 0.4, mob.getZ(),
                        1, mob.getBbWidth() * 0.3, mob.getBbHeight() * 0.3, mob.getBbWidth() * 0.3, 0.0);
            }
            if (level.getRandom().nextFloat() < 0.2F + decay * 0.5F) {
                level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 1, 0.1, 0.05, 0.1, 0.01);
            }
        }
    }

    /** One more point of health gone for every mob inside; the last one dissolves it, leaving nothing. */
    private void dissolve(ServerLevel level) {
        for (Mob mob : mobsInside(level)) {
            level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 6, 0.2, 0.3, 0.2, 0.01);
            if (mob.getHealth() <= 1.0F) {
                level.playSound(null, mob.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.2F);
                mob.discard();
                checkCooldown = 0;
            } else {
                mob.setHealth(mob.getHealth() - 1.0F);
                level.playSound(null, mob.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.5F);
            }
        }
    }

    /** Lets the mob in a Mob Capsule out inside the chamber. Returns a message key on failure, null on success. */
    public String releaseFromCapsule(ServerLevel level, ItemStack capsule) {
        if (!isStructureValid(level, worldPosition)) {
            return "message.cyberfarm.extractor.incomplete";
        }
        if (!mobsInside(level).isEmpty()) {
            return "message.cyberfarm.extractor.occupied";
        }
        if (MobCapsuleItem.release(level, capsule, Vec3.atBottomCenterOf(worldPosition.below(2))) == null) {
            return "message.cyberfarm.capsule.refused";
        }
        checkCooldown = 0;
        return null;
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> formed ? 1 : 0;
            case 1 -> mobCount;
            case 2 -> rate;
            case 3 -> tank.amount();
            case 4 -> tank.fluidCode();
            case 5 -> acid.amount();
            case 6 -> acid.fluidCode();
            case 7 -> EnergyUtil.low(mobId);
            case 8 -> EnergyUtil.high(mobId);
            case 9 -> filling ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 10;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putBoolean("filling", filling);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            setFramesFormed(level, pos, false);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        acid.serialize(output.child("acid"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
        acid.deserialize(input.childOrEmpty("acid"));
        sampledAcid = acid.amount();
        filling = input.getBooleanOr("filling", false);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MethaneExtractorMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
