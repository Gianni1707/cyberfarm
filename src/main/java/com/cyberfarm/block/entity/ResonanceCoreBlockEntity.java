package com.cyberfarm.block.entity;

import com.cyberfarm.block.ResonanceCellBlock;
import com.cyberfarm.menu.ResonanceCoreMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Resonance Core: a buildable FE store. Alone it is tier 1; a complete cube shell of Resonance Cells
 * around the crystal (3x3x3, 5x5x5, 7x7x7) makes it tier 2 to 4, bigger in every way. Stabilizer
 * Pylon columns lined up on its sides hold the crystal steady and are its only way in and out:
 * as many columns as the tier, each as tall as the tier plus one so its emitter faces the crystal.
 * With too few it shuts down: nothing goes in or out, nothing stored is lost.
 */
public class ResonanceCoreBlockEntity extends MachineBlockEntity {
    public static final int MAX_TIER = 4;
    /** By tier: FE stored, and FE per tick each pylon lets in or out. */
    private static final int[] CAPACITY = {1_000_000, 32_000_000, 256_000_000, 2_000_000_000};
    private static final int[] TRANSFER = {16_000, 128_000, 512_000, 2_000_000};
    public static final int STATUS_UNSTABLE = 0;
    public static final int STATUS_ONLINE = 1;
    /** What the core found on one side, shown on its screen so the player knows what to fix. */
    public static final int SIDE_NONE = 0;
    public static final int SIDE_LINKED = 1;
    public static final int SIDE_TOO_CLOSE = 2;
    public static final int SIDE_TOO_FAR = 3;
    public static final int SIDE_TOO_SHORT = 4;
    public static final int SIDE_BLOCKED = 5;
    private static final int SCAN_INTERVAL = 20;
    /** Data slots after the energy ones: tier, linked pylons, status, then one per side. */
    public static final int EXTRA_DATA = 7;
    private static final int SYNC_INTERVAL = 10;

    /** Half-size of the formed shell: 0 for tier 1 (no shell) up to 3 for tier 4. */
    private int radius;
    /** Linked pylon on each horizontal side, by 2D data value: distance from the core and height; 0 for none. */
    private final int[] pylonDistance = new int[4];
    private final int[] pylonHeight = new int[4];
    /** Per side: {@code SIDE_*} code, plus the column's height and distance, packed by {@link #packSide}. */
    private final int[] sides = new int[4];
    private int status;
    /** What the clients last heard, so the crystal and beams update without a packet every tick. */
    private long syncedState = -1;

    public ResonanceCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESONANCE_CORE.get(), pos, state, 0, CAPACITY[0], 0, 0);
    }

    public static int capacity(int tier) {
        return CAPACITY[tier - 1];
    }

    public static int transfer(int tier) {
        return TRANSFER[tier - 1];
    }

    /** Pylon columns a core of this tier needs, and how many blocks tall each must be. */
    public static int requiredPylons(int tier) {
        return tier;
    }

    public static int requiredHeight(int tier) {
        return tier + 1;
    }

    /** How far from the core a pylon may stand: clear of the shell, and not too far. */
    public static int minDistance(int tier) {
        return tier + 2;
    }

    public static int maxDistance(int tier) {
        return 6 + 2 * tier;
    }

    /** Height of the crystal's centre over the pedestal block, the centre of the shell cube. */
    public static float crystalHeight(int radius) {
        return radius + 1.5F;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % SCAN_INTERVAL == 0) {
            int found = findShell(level, pos);
            if (found != radius) {
                setShellFormed(level, pos, radius, false);
                setShellFormed(level, pos, found, true);
                radius = found;
                setChanged();
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                findPylon(level, pos, side);
            }
        }
        int tier = tier();
        status = linkedPylons() >= requiredPylons(tier) ? STATUS_ONLINE : STATUS_UNSTABLE;
        energy.setCapacity(capacity(tier));
        int transfer = status == STATUS_ONLINE ? transfer(tier) : 0;
        energy.setTransferLimits(transfer, transfer);

        if (level.getGameTime() % SYNC_INTERVAL == 0) {
            long synced = (long) packedPylons() << 32 | radius << 16 | status << 8 | fillPercent();
            if (synced != syncedState) {
                syncedState = synced;
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    public int tier() {
        return radius + 1;
    }

    // ---------------------------------------------------------------- shell

    /** The biggest complete shell: every block of the cube's surface a cell, its inside empty. */
    private static int findShell(Level level, BlockPos core) {
        for (int r = MAX_TIER - 1; r > 0; r--) {
            if (isShell(level, core, r)) {
                return r;
            }
        }
        return 0;
    }

    private static boolean isShell(Level level, BlockPos core, int r) {
        BlockPos centre = core.above(1 + r);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
            boolean surface = Math.max(Math.abs(pos.getX() - centre.getX()), Math.max(Math.abs(pos.getY() - centre.getY()),
                    Math.abs(pos.getZ() - centre.getZ()))) == r;
            BlockState state = level.getBlockState(pos);
            if (surface ? !state.is(ModBlocks.RESONANCE_CELL.get()) : !state.isAir()) {
                return false;
            }
        }
        return true;
    }

    /** Hides the cells of a shell inside the crystal, or shows them again. */
    private static void setShellFormed(Level level, BlockPos core, int r, boolean formed) {
        if (r <= 0) {
            return;
        }
        BlockPos centre = core.above(1 + r);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.RESONANCE_CELL.get()) && state.getValue(ResonanceCellBlock.FORMED) != formed) {
                level.setBlock(pos, state.setValue(ResonanceCellBlock.FORMED, formed), Block.UPDATE_ALL);
            }
        }
    }

    /**
     * A cell was right-clicked, finished shell or not: open the core it is being built around, the
     * closest one with the cell inside its biggest possible shell.
     */
    public static InteractionResult openFromShell(Level level, BlockPos cell, Player player) {
        int r = MAX_TIER - 1;
        ResonanceCoreBlockEntity found = null;
        for (BlockPos pos : BlockPos.betweenClosed(cell.offset(-r, -(1 + 2 * r), -r), cell.below())) {
            if (level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity core
                    && (found == null || pos.distSqr(cell) < found.getBlockPos().distSqr(cell))) {
                found = core;
            }
        }
        return open(level, found, player);
    }

    /** A pylon was right-clicked: open the core it is lined up with, linked or not yet. */
    public static InteractionResult openFromPylon(Level level, BlockPos pylon, Player player) {
        return open(level, nearbyCore(level, pylon, false), player);
    }

    private static InteractionResult open(Level level, @Nullable ResonanceCoreBlockEntity core, Player player) {
        if (core == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.openMenu(core);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            setShellFormed(level, pos, radius, false);
        }
    }

    // ---------------------------------------------------------------- pylons

    /**
     * The first pylon column along {@code side} at the core's level, outside the shell: it links
     * when it stands within the tier's distance, is tall enough, and nothing blocks its beam at the
     * crystal's height. Otherwise the side remembers why not, for the screen.
     */
    private void findPylon(Level level, BlockPos core, Direction side) {
        int index = side.get2DDataValue();
        pylonDistance[index] = 0;
        pylonHeight[index] = 0;
        sides[index] = SIDE_NONE;
        int tier = tier();
        boolean blocked = false;
        for (int distance = radius + 1; distance <= maxDistance(MAX_TIER); distance++) {
            BlockPos ground = core.relative(side, distance);
            if (level.getBlockState(ground).is(ModBlocks.STABILIZER_PYLON.get())) {
                int height = 1;
                while (height < 8 && level.getBlockState(ground.above(height)).is(ModBlocks.STABILIZER_PYLON.get())) {
                    height++;
                }
                int code = blocked ? SIDE_BLOCKED
                        : distance < minDistance(tier) ? SIDE_TOO_CLOSE
                        : distance > maxDistance(tier) ? SIDE_TOO_FAR
                        : height < requiredHeight(tier) ? SIDE_TOO_SHORT
                        : SIDE_LINKED;
                sides[index] = packSide(code, height, distance);
                if (code == SIDE_LINKED) {
                    pylonDistance[index] = distance;
                    pylonHeight[index] = height;
                }
                return;
            }
            BlockPos beam = ground.above(1 + radius);
            if (!level.getBlockState(beam).getCollisionShape(level, beam).isEmpty()) {
                blocked = true;
            }
        }
    }

    /** One side's findings in a data slot (synced as a short): code, column height, distance. */
    private static int packSide(int code, int height, int distance) {
        return code | height << 3 | distance << 7;
    }

    public static int sideCode(int packed) {
        return packed & 0x7;
    }

    public static int sideHeight(int packed) {
        return (packed >> 3) & 0xF;
    }

    public static int sideDistance(int packed) {
        return (packed >> 7) & 0xF;
    }

    public int linkedPylons() {
        int count = 0;
        for (int distance : pylonDistance) {
            if (distance > 0) {
                count++;
            }
        }
        return count;
    }

    /** Distance and height of the pylon linked on {@code side}, 0 for none; also valid on the client. */
    public int pylonDistance(Direction side) {
        return pylonDistance[side.get2DDataValue()];
    }

    public int pylonHeight(Direction side) {
        return pylonHeight[side.get2DDataValue()];
    }

    /** The core a pylon column is linked to, if any. */
    public static @Nullable ResonanceCoreBlockEntity linkedCore(Level level, BlockPos pylon) {
        return nearbyCore(level, pylon, true);
    }

    /**
     * A core lined up with the pylon column, looked for along the four sides of its bottom block;
     * with {@code linked} only one that counts this column among its pylons.
     */
    private static @Nullable ResonanceCoreBlockEntity nearbyCore(Level level, BlockPos pylon, boolean linked) {
        BlockPos base = pylon;
        while (level.getBlockState(base.below()).is(ModBlocks.STABILIZER_PYLON.get())) {
            base = base.below();
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int distance = 1; distance <= maxDistance(MAX_TIER); distance++) {
                BlockPos pos = base.relative(side, distance);
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity core
                        && (!linked || core.pylonDistance(side.getOpposite()) == distance)) {
                    return core;
                }
            }
        }
        return null;
    }

    private int packedPylons() {
        int packed = 0;
        for (int i = 0; i < 4; i++) {
            packed |= (pylonDistance[i] | pylonHeight[i] << 4) << (i * 8);
        }
        return packed;
    }

    // ---------------------------------------------------------------- state

    public int radius() {
        return radius;
    }

    public int status() {
        return status;
    }

    public int fillPercent() {
        int capacity = energy.getCapacity();
        return capacity <= 0 ? 0 : (int) ((long) energy.getEnergy() * 100 / capacity);
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> tier();
            case 1 -> linkedPylons();
            case 2 -> status;
            case 3, 4, 5, 6 -> sides[id - 3];
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return EXTRA_DATA;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("radius", radius);
        output.putInt("pylons", packedPylons());
        output.putInt("status", status);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        radius = input.getIntOr("radius", 0);
        int packed = input.getIntOr("pylons", 0);
        for (int i = 0; i < 4; i++) {
            pylonDistance[i] = (packed >> (i * 8)) & 0xF;
            pylonHeight[i] = (packed >> (i * 8 + 4)) & 0xF;
        }
        status = input.getIntOr("status", STATUS_UNSTABLE);
        // Size the buffer for the saved tier right away, so a full core is not cut down before its first tick
        energy.setCapacity(capacity(tier()));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ResonanceCoreMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
