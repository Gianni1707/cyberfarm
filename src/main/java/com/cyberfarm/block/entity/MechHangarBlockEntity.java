package com.cyberfarm.block.entity;

import java.util.List;

import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MechHangarBlock;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.entity.MechEntity;
import com.cyberfarm.entity.MechModule;
import com.cyberfarm.item.MechModuleItem;
import com.cyberfarm.menu.MechHangarMenu;
import com.cyberfarm.multiblock.Blueprint;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import com.cyberfarm.registry.ModBlocks;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/**
 * Mech Hangar console. With the hangar complete, a mech left parked on the pad is charged from
 * the console's battery, repaired back to full integrity (a shut-down one comes back on) and can
 * have its modules fitted or removed in the console's four slots, one per mount.
 *
 * <p>The slots mirror the docked mech: they fill with its modules when it parks and every change
 * goes straight back to it. With no mech parked they are empty and take nothing; the modules
 * always travel with the mech.
 */
public class MechHangarBlockEntity extends MachineBlockEntity {
    private static final int CAPACITY = 1_000_000;
    private static final int MAX_INPUT = 8_192;
    /** FE a parked mech is charged with per tick. */
    public static final int CHARGE_RATE = 2_000;
    /** Integrity restored every {@link #REPAIR_INTERVAL} ticks, and what it costs. */
    private static final float REPAIR_AMOUNT = 1.0F;
    private static final int REPAIR_COST = 200;
    private static final int REPAIR_INTERVAL = 10;
    private static final int CHECK_INTERVAL = 20;
    /** How fast a parked mech glides to its place on the stand: the share of the way per tick, and degrees per tick. */
    private static final double ALIGN_STEP = 0.15;
    private static final float ALIGN_TURN = 6.0F;

    private final ItemStacksResourceHandler modules = new ItemStacksResourceHandler(MechModule.values().length) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return docked != null && resource.getItem() instanceof MechModuleItem module && module.module().ordinal() == index;
        }

        @Override
        public int getCapacity(int index, ItemResource resource) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            if (!loading && docked != null) {
                docked.setModules(maskOf());
            }
            setChanged();
        }
    };
    private boolean formed;
    private @Nullable MechEntity docked;
    private boolean loading;
    private int checkCooldown;
    /** What the screen shows: 0 nothing parked, 1 charging, 2 repairing, 3 ready. */
    private int status;
    /** Whether a mech is parked, as the clients last heard (the clamps hold it). */
    private boolean syncedDocked;
    // Client side: the clamps' drop, easing towards the synced state
    private boolean clientDocked;
    private float clamp;
    private long clampTime;

    public MechHangarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MECH_HANGAR.get(), pos, state, 0, CAPACITY, MAX_INPUT, 0);
    }

    public ItemStacksResourceHandler getModules() {
        return modules;
    }

    private int maskOf() {
        int mask = 0;
        for (MechModule module : MechModule.values()) {
            if (!modules.getResource(module.ordinal()).isEmpty()) {
                mask |= module.bit();
            }
        }
        return mask;
    }

    /** Fills the slots with the docked mech's modules, or empties them, without touching the mech. */
    private void mirror(@Nullable MechEntity mech) {
        loading = true;
        for (MechModule module : MechModule.values()) {
            boolean installed = mech != null && mech.hasModule(module);
            modules.set(module.ordinal(), installed ? ItemResource.of(module.item()) : ItemResource.EMPTY, installed ? 1 : 0);
        }
        loading = false;
    }

    private Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    /** The way out of the hangar, the way a mech deployed in it faces. */
    public Direction facingOut() {
        return facing();
    }

    public BlockPos padCentre() {
        return Blueprint.hangarCentre(worldPosition, facing());
    }

    /** The complete hangar whose pad {@code pad} is part of, or null. */
    public static @Nullable MechHangarBlockEntity ofPad(Level level, BlockPos pad) {
        if (!level.getBlockState(pad).is(ModBlocks.HANGAR_PAD.get())) {
            return null;
        }
        for (BlockPos at : BlockPos.betweenClosed(pad.offset(-4, 0, -4), pad.offset(4, 0, 4))) {
            if (level.getBlockEntity(at) instanceof MechHangarBlockEntity hangar && hangar.getBlockState().getValue(MechHangarBlock.FORMED)) {
                BlockPos centre = hangar.padCentre();
                if (Math.abs(centre.getX() - pad.getX()) <= 1 && Math.abs(centre.getZ() - pad.getZ()) <= 1 && centre.getY() == pad.getY()) {
                    return hangar;
                }
            }
        }
        return null;
    }

    /**
     * Right-clicking a frame or the pad of a complete hangar with an empty hand opens its console.
     * Sneaking skips it, so blocks can still be placed against the hangar.
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos clicked = event.getPos();
        BlockState state = level.getBlockState(clicked);
        if (player.isSecondaryUseActive() || !event.getItemStack().isEmpty()
                || !state.is(ModBlocks.HANGAR_FRAME.get()) && !state.is(ModBlocks.HANGAR_PAD.get())) {
            return;
        }
        int reach = Blueprint.HANGAR_SIZE - 1;
        for (BlockPos at : BlockPos.betweenClosed(clicked.offset(-reach, -Blueprint.HANGAR_TOP, -reach), clicked.offset(reach, 0, reach))) {
            if (level.getBlockEntity(at) instanceof MechHangarBlockEntity hangar && hangar.getBlockState().getValue(MechHangarBlock.FORMED)
                    && Blueprint.MECH_HANGAR.ghosts(hangar.worldPosition, hangar.facing()).stream().anyMatch(ghost -> ghost.pos().equals(clicked))) {
                if (!level.isClientSide()) {
                    player.openMenu(hangar);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
    }

    /** The console's Fold button: the parked mech folds back into its frame. */
    public void foldDocked() {
        if (docked != null) {
            docked.startFold();
        }
    }

    /** The space over the pad, where a mech parks. */
    private AABB bay() {
        BlockPos centre = Blueprint.hangarCentre(worldPosition, facing());
        return new AABB(centre.offset(-1, 1, -1).getCenter(), centre.offset(1, Blueprint.HANGAR_TOP - 1, 1).getCenter()).inflate(0.5);
    }

    private boolean isComplete(Level level) {
        return Blueprint.MECH_HANGAR.ghosts(worldPosition, facing()).stream().allMatch(ghost -> ghost.ok().test(level.getBlockState(ghost.pos())));
    }

    /** Lights up (or turns off) the hangar's frames and pad. */
    private void setPartsFormed(Level level, boolean formed) {
        for (Blueprint.Ghost ghost : Blueprint.MECH_HANGAR.ghosts(worldPosition, facing())) {
            BlockState part = level.getBlockState(ghost.pos());
            if (part.getBlock() instanceof CyberFrameBlock && part.getValue(CyberFrameBlock.FORMED) != formed) {
                level.setBlock(ghost.pos(), part.setValue(CyberFrameBlock.FORMED, formed), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--checkCooldown <= 0) {
            checkCooldown = CHECK_INTERVAL;
            formed = isComplete(level);
            if (state.getValue(MechHangarBlock.FORMED) != formed) {
                level.setBlock(pos, state.setValue(MechHangarBlock.FORMED, formed), Block.UPDATE_ALL);
                level.playSound(null, pos, (formed ? ModSounds.EXTRACTOR_ACTIVATE : ModSounds.EXTRACTOR_DEACTIVATE).get(), SoundSource.BLOCKS, 1.0F, 0.8F);
                setPartsFormed(level, formed);
            } else if (formed) {
                setPartsFormed(level, true);
            }
        }
        // Who is parked: a new mech fills the slots, one that left empties them
        MechEntity parked = null;
        if (formed) {
            List<MechEntity> mechs = level.getEntitiesOfClass(MechEntity.class, bay(), MechEntity::isParked);
            parked = mechs.isEmpty() ? null : mechs.getFirst();
        }
        if (parked != docked) {
            docked = parked;
            mirror(parked);
        }
        // A mech left on the pad straightens up on the stand: to the middle, facing out; then the clamps close
        boolean held = docked != null && align(docked);
        if (held != syncedDocked) {
            syncedDocked = held;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        status = 0;
        if (docked != null) {
            status = 3;
            int room = MechEntity.CAPACITY - docked.getEnergy();
            int charge = Math.min(CHARGE_RATE, Math.min(room, energy.getAmountAsInt()));
            if (charge > 0 && energy.consume(charge)) {
                docked.setEnergy(docked.getEnergy() + charge);
                status = 1;
            }
            if (docked.getHealth() < docked.getMaxHealth()) {
                status = 2;
                if (level.getGameTime() % REPAIR_INTERVAL == 0 && energy.consume(REPAIR_COST)) {
                    docked.repair(REPAIR_AMOUNT);
                }
            }
        }
    }

    /**
     * Moves a parked mech a step towards its place on the stand, the middle of the pad facing out of
     * the hangar. Returns whether it is there.
     */
    private boolean align(MechEntity mech) {
        Vec3 target = Vec3.atBottomCenterOf(padCentre().above());
        float yaw = facingOut().toYRot();
        Vec3 at = mech.position();
        float turn = Mth.wrapDegrees(yaw - mech.getYRot());
        if (at.distanceTo(target) < 0.02 && Math.abs(turn) < 0.5F) {
            return true;
        }
        Vec3 next = at.distanceTo(target) < 0.02 ? target : at.lerp(target, ALIGN_STEP);
        float rotation = Mth.approachDegrees(mech.getYRot(), yaw, ALIGN_TURN);
        mech.setPos(next.x, next.y, next.z);
        mech.setDeltaMovement(Vec3.ZERO);
        mech.setYRot(rotation);
        mech.setYBodyRot(rotation);
        mech.setYHeadRot(rotation);
        return false;
    }

    /** Client side: how far the clamps are down on a parked mech's shoulders, 0 up to 1 holding. */
    public float clampAmount(float time) {
        float elapsed = Math.max(0.0F, time - clampTime);
        clampTime = (long) time;
        clamp = Mth.clamp(clamp + (clientDocked ? 1 : -1) * elapsed * 0.05F, 0.0F, 1.0F);
        return clamp;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putBoolean("docked", docked != null);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        clientDocked = input.getBooleanOr("docked", false);
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> formed ? 1 : 0;
            case 1 -> status;
            case 2 -> docked != null ? EnergyUtil.low(docked.getEnergy()) : 0;
            case 3 -> docked != null ? EnergyUtil.high(docked.getEnergy()) : 0;
            case 4 -> docked != null ? Math.round(docked.getHealth() / docked.getMaxHealth() * 100) : 0;
            case 5 -> docked != null && docked.isDisabled() ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 6;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            setPartsFormed(level, false);
        }
        // The slots only mirror the mech: nothing of theirs is dropped
        mirror(null);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MechHangarMenu(containerId, inventory, modules, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
