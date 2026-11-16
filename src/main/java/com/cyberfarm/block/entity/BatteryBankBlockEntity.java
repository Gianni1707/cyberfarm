package com.cyberfarm.block.entity;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.menu.BatteryBankMenu;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.LimitingEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Battery Bank: an FE store that merges like the fluid tanks. Blocks that fill a box, up to {@link #MAX_SIDE} a side,
 * are one battery: each block keeps its share, the block at the low corner (the runner) evens the shares out, charges
 * the item in its slot, and the blocks give power to whatever touches them outside the box, such as a Human Dock.
 * Smaller than the Resonance Core on purpose: the bank is the base's pocket battery.
 */
public class BatteryBankBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 150_000;
    public static final int TRANSFER = 4_000;
    /** A bank's longest side: 27 blocks at most, 4.05 million FE. */
    public static final int MAX_SIDE = 3;
    private static final int GROW_INTERVAL = 40;
    public static final int CHARGE_SLOT = 0;

    /**
     * What a face of the bank does, set with the Cable Swapper for each side of the whole bank: In takes FE and gives none, Out gives
     * FE to the block there and takes none, Off shows nothing to cables (no socket either).
     */
    public enum FaceMode {
        IN, OUT, OFF;

        FaceMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** The faces' modes by direction; the runner's are the bank's. */
    private final FaceMode[] modes = {FaceMode.IN, FaceMode.IN, FaceMode.IN, FaceMode.IN, FaceMode.IN, FaceMode.IN};
    private final EnergyHandler inOnly = new LimitingEnergyHandler(() -> energy, TRANSFER, 0);
    private final EnergyHandler outOnly = new LimitingEnergyHandler(() -> energy, 0, TRANSFER);

    private BlockPos boxMin;
    private BlockPos boxMax;
    private boolean growSoon = true;
    /** FE this block pushed out since the runner last counted. */
    private int pushed;
    /** On the runner: the bank's total after its last tick, and the FE/t flowing in and out, smoothed. */
    private long lastTotal = -1;
    private float inRate;
    private float outRate;
    /** On the runner, sent to the clients for the screen: charge and flows as last synced. */
    private long shownEnergy;
    private long shownCapacity;
    private int shownIn;
    private int shownOut;

    public BatteryBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_BANK.get(), pos, state, 1, CAPACITY, TRANSFER, TRANSFER);
        boxMin = pos;
        boxMax = pos;
    }

    public BlockPos boxMin() {
        return boxMin;
    }

    public BlockPos boxMax() {
        return boxMax;
    }

    private boolean inBox(BlockPos pos) {
        return pos.getX() >= boxMin.getX() && pos.getX() <= boxMax.getX() && pos.getY() >= boxMin.getY() && pos.getY() <= boxMax.getY()
                && pos.getZ() >= boxMin.getZ() && pos.getZ() <= boxMax.getZ();
    }

    /** Whether the neighbour on this side is a block of the same bank. */
    public boolean joined(Direction side) {
        return inBox(worldPosition.relative(side));
    }

    /** The bank's loaded blocks, this one included. */
    private List<BatteryBankBlockEntity> cells() {
        List<BatteryBankBlockEntity> cells = new ArrayList<>();
        if (level != null) {
            for (BlockPos at : BlockPos.betweenClosed(boxMin, boxMax)) {
                if (level.isLoaded(at) && level.getBlockEntity(at) instanceof BatteryBankBlockEntity cell) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }

    /** The block running the bank, which holds the GUI's slot; this one if it is alone. */
    public BatteryBankBlockEntity runner() {
        return level != null && level.getBlockEntity(boxMin) instanceof BatteryBankBlockEntity runner ? runner : this;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        // Every block gives power out of the faces set to Out
        for (Direction side : Direction.values()) {
            if (!joined(side) && energy.getEnergy() > 0 && mode(side) == FaceMode.OUT) {
                pushed += EnergyUtil.pushTo(level, pos.relative(side), side.getOpposite(), energy, TRANSFER);
            }
        }
        if (!pos.equals(boxMin)) {
            return;
        }
        if (growSoon || level.getGameTime() % GROW_INTERVAL == 0) {
            growSoon = false;
            grow(level);
        }
        List<BatteryBankBlockEntity> cells = cells();
        long total = 0;
        int out = 0;
        for (BatteryBankBlockEntity cell : cells) {
            total += cell.energy.getEnergy();
            out += cell.pushed;
            cell.pushed = 0;
        }
        int charged = chargeItem(cells, total);
        // What came in: the change since last tick, plus what went out meanwhile
        long in = lastTotal < 0 ? 0 : Math.max(0, total - lastTotal + out);
        total -= charged;
        out += charged;
        inRate += (in - inRate) * 0.1F;
        outRate += (out - outRate) * 0.1F;
        lastTotal = total;
        sync(level, pos, state, total, (long) cells.size() * CAPACITY);
        // Even shares, so the bank fills and empties as one
        int share = (int) (total / cells.size());
        long rest = total - (long) share * cells.size();
        for (BatteryBankBlockEntity cell : cells) {
            int wanted = share + (rest-- > 0 ? 1 : 0);
            if (cell.energy.getEnergy() != wanted) {
                cell.energy.set(wanted);
            }
        }
    }

    public FaceMode mode(Direction side) {
        return runner().modes[side.get3DDataValue()];
    }

    /** What a cable or a machine on {@code side} is handed: the face's mode decides; null for Off, so nothing connects. */
    public @Nullable EnergyHandler sideHandler(@Nullable Direction side) {
        if (side == null || joined(side)) {
            return energy;
        }
        return switch (mode(side)) {
            case IN -> inOnly;
            case OUT -> outOnly;
            case OFF -> null;
        };
    }

    /** The Cable Swapper on one side of the bank: the next mode, and the cables around told to look again. */
    public void cycle(Direction side) {
        BatteryBankBlockEntity runner = runner();
        runner.modes[side.get3DDataValue()] = runner.modes[side.get3DDataValue()].next();
        runner.setChanged();
        if (level instanceof ServerLevel server) {
            for (BatteryBankBlockEntity cell : runner.cells()) {
                server.invalidateCapabilities(cell.worldPosition);
                server.updateNeighborsAt(cell.worldPosition, cell.getBlockState().getBlock());
            }
            server.sendBlockUpdated(runner.worldPosition, runner.getBlockState(), runner.getBlockState(), 2);
        }
    }

    /** The item in the slot takes what it can from the whole bank. Returns the FE given. */
    private int chargeItem(List<BatteryBankBlockEntity> cells, long total) {
        if (getStack(CHARGE_SLOT).isEmpty() || total <= 0) {
            return 0;
        }
        EnergyHandler handler = ItemAccess.forHandlerIndexStrict(items, CHARGE_SLOT).getCapability(Capabilities.Energy.ITEM);
        if (handler == null) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int given = handler.insert((int) Math.min(TRANSFER, total), transaction);
            transaction.commit();
            return given;
        }
    }

    /** Every half second, if anything shown changed, the runner tells the clients for the screen. */
    private void sync(ServerLevel level, BlockPos pos, BlockState state, long total, long capacity) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        int in = Math.round(inRate);
        int out = Math.round(outRate);
        if (total != shownEnergy || capacity != shownCapacity || in != shownIn || out != shownOut) {
            shownEnergy = total;
            shownCapacity = capacity;
            shownIn = in;
            shownOut = out;
            setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    /** Client side, on the runner: the bank's charge and its FE/t in and out, as last synced. */
    public long shownEnergy() {
        return shownEnergy;
    }

    public long shownCapacity() {
        return shownCapacity;
    }

    public int shownIn() {
        return shownIn;
    }

    public int shownOut() {
        return shownOut;
    }

    /** Totals for the GUI: what the whole bank holds and can hold. */
    public long bankEnergy() {
        return cells().stream().mapToLong(cell -> cell.energy.getEnergy()).sum();
    }

    public long bankCapacity() {
        return (long) cells().size() * CAPACITY;
    }

    /** Takes in whole layers of single blocks or whole banks lying wholly in the new box, up to the size limit. */
    private void grow(ServerLevel level) {
        BlockPos min = boxMin;
        BlockPos max = boxMax;
        boolean grown = true;
        while (grown) {
            grown = false;
            for (Direction side : Direction.values()) {
                BlockPos newMin = side.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? min.relative(side) : min;
                BlockPos newMax = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? max.relative(side) : max;
                if (Math.max(newMax.getX() - newMin.getX(), Math.max(newMax.getY() - newMin.getY(), newMax.getZ() - newMin.getZ())) >= MAX_SIDE) {
                    continue;
                }
                if (layerJoins(level, side, newMin, newMax)) {
                    min = newMin;
                    max = newMax;
                    grown = true;
                }
            }
        }
        if (!min.equals(boxMin) || !max.equals(boxMax)) {
            for (BlockPos at : BlockPos.betweenClosed(min, max)) {
                if (level.getBlockEntity(at) instanceof BatteryBankBlockEntity cell) {
                    cell.adopt(level, min.immutable(), max.immutable());
                }
            }
        }
    }

    /** Whether the new layer on {@code side} is all bank blocks whose banks lie wholly in the new box. */
    private static boolean layerJoins(ServerLevel level, Direction side, BlockPos newMin, BlockPos newMax) {
        BlockPos layerMin = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? new BlockPos(
                side.getStepX() != 0 ? newMax.getX() : newMin.getX(), side.getStepY() != 0 ? newMax.getY() : newMin.getY(),
                side.getStepZ() != 0 ? newMax.getZ() : newMin.getZ()) : newMin;
        BlockPos layerMax = side.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? new BlockPos(
                side.getStepX() != 0 ? newMin.getX() : newMax.getX(), side.getStepY() != 0 ? newMin.getY() : newMax.getY(),
                side.getStepZ() != 0 ? newMin.getZ() : newMax.getZ()) : newMax;
        for (BlockPos at : BlockPos.betweenClosed(layerMin, layerMax)) {
            if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof BatteryBankBlockEntity cell)) {
                return false;
            }
            if (cell.boxMin.getX() < newMin.getX() || cell.boxMin.getY() < newMin.getY() || cell.boxMin.getZ() < newMin.getZ()
                    || cell.boxMax.getX() > newMax.getX() || cell.boxMax.getY() > newMax.getY() || cell.boxMax.getZ() > newMax.getZ()) {
                return false;
            }
        }
        return true;
    }

    /** This block now belongs to the bank in this box. */
    private void adopt(ServerLevel level, BlockPos min, BlockPos max) {
        boxMin = min;
        boxMax = max;
        growSoon = true;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        level.invalidateCapabilities(worldPosition);
    }

    /** Broken: the other blocks go back to being alone, keeping their share, and join up again as they can. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && !boxMin.equals(boxMax)) {
            for (BlockPos at : BlockPos.betweenClosed(boxMin, boxMax)) {
                if (!at.equals(pos) && server.getBlockEntity(at) instanceof BatteryBankBlockEntity cell) {
                    cell.adopt(server, at.immutable(), at.immutable());
                }
            }
        }
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return index == CHARGE_SLOT && ItemAccess.forStack(resource.toStack()).getCapability(Capabilities.Energy.ITEM) != null;
    }

    @Override
    protected int getExtraData(int id) {
        BatteryBankBlockEntity runner = runner();
        long value = switch (id) {
            case 0, 1 -> runner.bankEnergy();
            case 2, 3 -> runner.bankCapacity();
            case 4, 5 -> Math.round(runner.inRate);
            case 6, 7 -> Math.round(runner.outRate);
            default -> 0;
        };
        // Two halves of a long, low then high, as the menu data slots are ints
        return id % 2 == 0 ? EnergyUtil.low((int) Math.min(Integer.MAX_VALUE, value)) : EnergyUtil.high((int) Math.min(Integer.MAX_VALUE, value));
    }

    @Override
    protected int getExtraDataCount() {
        return 8;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("box_min", BlockPos.CODEC, boxMin);
        output.store("box_max", BlockPos.CODEC, boxMax);
        output.putLong("shown_energy", shownEnergy);
        output.putLong("shown_capacity", shownCapacity);
        output.putInt("shown_in", shownIn);
        output.putInt("shown_out", shownOut);
        output.putIntArray("faces", java.util.Arrays.stream(modes).mapToInt(FaceMode::ordinal).toArray());
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        boxMin = input.read("box_min", BlockPos.CODEC).orElse(worldPosition);
        boxMax = input.read("box_max", BlockPos.CODEC).orElse(worldPosition);
        shownEnergy = input.getLongOr("shown_energy", 0);
        shownCapacity = input.getLongOr("shown_capacity", 0);
        shownIn = input.getIntOr("shown_in", 0);
        shownOut = input.getIntOr("shown_out", 0);
        input.getIntArray("faces").ifPresent(faces -> {
            for (int i = 0; i < Math.min(faces.length, modes.length); i++) {
                modes[i] = FaceMode.values()[Math.floorMod(faces[i], FaceMode.values().length)];
            }
        });
    }

    /** Every block opens the bank's GUI, run by the low corner block, which holds the charging slot. */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        BatteryBankBlockEntity runner = runner();
        return new BatteryBankMenu(containerId, inventory, runner.items, runner.data, ContainerLevelAccess.create(level, runner.worldPosition));
    }
}
