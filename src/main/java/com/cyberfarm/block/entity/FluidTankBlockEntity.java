package com.cyberfarm.block.entity;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.block.FluidTankBlock;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * One block of a Liquid Tank or Gas Tank: 32 buckets of one fluid. Tanks of a kind next to each other join into one tank when they
 * fill a box, of any size: {@link #boxMin} and {@link #boxMax} say which, and the block at the low corner runs it. Each block keeps its
 * own share in its own {@link GasTank}, so breaking one loses only what it held; pipes and buckets see them all as one.
 */
public class FluidTankBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 32_000;
    /** A tank's longest side, so one placed block can't make the server walk a whole base. */
    private static final int MAX_SIDE = 16;
    /** Ticks between two updates of the clients' copy, which the bar shows. */
    private static final int SYNC_INTERVAL = 5;
    /** Ticks between looks for neighbours to join: tanks can become mergeable by being emptied. */
    private static final int GROW_INTERVAL = 40;

    private final GasTank tank = new GasTank(CAPACITY, this::accepts, this::setChanged);
    private BlockPos boxMin;
    private BlockPos boxMax;
    private boolean growSoon = true;
    /** On the block at the low corner, what the whole tank holds: the renderer's view, kept for the clients. */
    private Fluid shownFluid = Fluids.EMPTY;
    private int shownAmount;
    private @Nullable Handler handler;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state, 0, 0, 0, 0);
        boxMin = pos;
        boxMax = pos;
    }

    private boolean gasKind() {
        return getBlockState().getBlock() instanceof FluidTankBlock block && block.gas();
    }

    /** A Gas Tank holds what is lighter than air, a Liquid Tank the rest; all its blocks hold the same. */
    private boolean accepts(FluidResource resource) {
        if (resource.getFluidType().isLighterThanAir() != gasKind()) {
            return false;
        }
        Fluid held = heldFluid();
        return held == Fluids.EMPTY || held == resource.getFluid();
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

    /** Whether the neighbour on this side is a block of the same tank. */
    public boolean joined(Direction side) {
        return inBox(worldPosition.relative(side));
    }

    private int blocks() {
        return (boxMax.getX() - boxMin.getX() + 1) * (boxMax.getY() - boxMin.getY() + 1) * (boxMax.getZ() - boxMin.getZ() + 1);
    }

    /** The tank's blocks, from the bottom up, so it fills like a tank. */
    private List<FluidTankBlockEntity> cells() {
        List<FluidTankBlockEntity> cells = new ArrayList<>();
        if (level != null) {
            for (BlockPos at : BlockPos.betweenClosed(boxMin, boxMax)) {
                if (level.getBlockEntity(at) instanceof FluidTankBlockEntity cell) {
                    cells.add(cell);
                }
            }
            cells.sort((a, b) -> Integer.compare(a.worldPosition.getY(), b.worldPosition.getY()));
        }
        return cells;
    }

    private Fluid heldFluid() {
        for (GasTank cell : handler().tanks()) {
            if (cell.amount() > 0) {
                return cell.fluid();
            }
        }
        return Fluids.EMPTY;
    }

    /**
     * What pipes and buckets deal with: all the tank's blocks as one. Kept, since every transfer asks what the tank holds.
     * shortcut: a tank across a chunk border can keep missing the blocks that weren't loaded when this was built, until it next changes or reloads; rebuild on chunk load if it shows.
     */
    public Handler handler() {
        if (handler == null) {
            handler = new Handler(cells().stream().map(cell -> cell.tank).toArray(GasTank[]::new));
        }
        return handler;
    }

    /** Client side, for the renderer: how full the whole tank is, 0 to 1, and with what, from the block running it. */
    public @Nullable FluidTankBlockEntity runner() {
        return level != null && level.getBlockEntity(boxMin) instanceof FluidTankBlockEntity runner ? runner : null;
    }

    public Fluid shownFluid() {
        return shownFluid;
    }

    public float shownFill() {
        return (float) shownAmount / (blocks() * CAPACITY);
    }

    /** Only the block running the tank does anything: pipes and buckets do the moving. */
    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!pos.equals(boxMin)) {
            return;
        }
        if (growSoon || level.getGameTime() % GROW_INTERVAL == 0) {
            growSoon = false;
            grow(level);
        }
        if (level.getGameTime() % SYNC_INTERVAL == 0) {
            int amount = 0;
            for (GasTank cell : handler().tanks()) {
                amount += cell.amount();
            }
            Fluid fluid = heldFluid();
            if (amount != shownAmount || fluid != shownFluid) {
                shownAmount = amount;
                shownFluid = fluid;
                setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Takes in whole layers of tanks on any side, as long as what comes in has the same fluid and lies wholly in the new box. */
    private void grow(ServerLevel level) {
        BlockPos min = boxMin;
        BlockPos max = boxMax;
        Fluid fluid = heldFluid();
        boolean grown = true;
        while (grown) {
            grown = false;
            for (Direction side : Direction.values()) {
                BlockPos newMin = side.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? min.relative(side) : min;
                BlockPos newMax = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? max.relative(side) : max;
                if (Math.max(newMax.getX() - newMin.getX(), Math.max(newMax.getY() - newMin.getY(), newMax.getZ() - newMin.getZ())) >= MAX_SIDE) {
                    continue;
                }
                BlockPos layerMin = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? new BlockPos(
                        side.getStepX() != 0 ? newMax.getX() : newMin.getX(), side.getStepY() != 0 ? newMax.getY() : newMin.getY(), side.getStepZ() != 0 ? newMax.getZ() : newMin.getZ()) : newMin;
                BlockPos layerMax = side.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? new BlockPos(
                        side.getStepX() != 0 ? newMin.getX() : newMax.getX(), side.getStepY() != 0 ? newMin.getY() : newMax.getY(), side.getStepZ() != 0 ? newMin.getZ() : newMax.getZ()) : newMax;
                Fluid layerFluid = layerFluid(level, layerMin, layerMax, newMin, newMax, fluid);
                if (layerFluid != null) {
                    min = newMin;
                    max = newMax;
                    fluid = layerFluid;
                    grown = true;
                }
            }
        }
        if (!min.equals(boxMin) || !max.equals(boxMax)) {
            for (BlockPos at : BlockPos.betweenClosed(min, max)) {
                if (level.getBlockEntity(at) instanceof FluidTankBlockEntity cell) {
                    cell.adopt(level, min.immutable(), max.immutable());
                }
            }
        }
    }

    /** The fluid of a layer that may join, the given one if it is empty; null if it may not. */
    private @Nullable Fluid layerFluid(ServerLevel level, BlockPos layerMin, BlockPos layerMax, BlockPos newMin, BlockPos newMax, Fluid fluid) {
        Fluid found = fluid;
        for (BlockPos at : BlockPos.betweenClosed(layerMin, layerMax)) {
            if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof FluidTankBlockEntity cell) || cell.gasKind() != gasKind()) {
                return null;
            }
            // Another tank partly outside the new box stays as it is
            if (cell.boxMin.getX() < newMin.getX() || cell.boxMin.getY() < newMin.getY() || cell.boxMin.getZ() < newMin.getZ()
                    || cell.boxMax.getX() > newMax.getX() || cell.boxMax.getY() > newMax.getY() || cell.boxMax.getZ() > newMax.getZ()) {
                return null;
            }
            Fluid held = cell.heldFluid();
            if (held != Fluids.EMPTY) {
                if (found != Fluids.EMPTY && found != held) {
                    return null;
                }
                found = held;
            }
        }
        return found;
    }

    /** This block now belongs to the tank in this box. */
    private void adopt(ServerLevel level, BlockPos min, BlockPos max) {
        boxMin = min;
        boxMax = max;
        handler = null;
        growSoon = true;
        // Told again from the block running it, which may be a different one now
        shownAmount = -1;
        setChanged();
        BlockState state = getBlockState();
        if (state.getBlock() instanceof FluidTankBlock block) {
            BlockState now = block.withSides(state, level, worldPosition, this);
            if (now != state) {
                level.setBlock(worldPosition, now, Block.UPDATE_CLIENTS);
            }
            level.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_CLIENTS);
        }
        level.invalidateCapabilities(worldPosition);
    }

    /** Broken: the other blocks go back to being alone, keeping what they hold, and join up again as they can. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && !boxMin.equals(boxMax)) {
            for (BlockPos at : BlockPos.betweenClosed(boxMin, boxMax)) {
                if (!at.equals(pos) && server.getBlockEntity(at) instanceof FluidTankBlockEntity cell) {
                    cell.adopt(server, at.immutable(), at.immutable());
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public Component status() {
        int amount = 0;
        Fluid fluid = Fluids.EMPTY;
        for (FluidTankBlockEntity cell : cells()) {
            amount += cell.tank.amount();
            fluid = cell.tank.amount() > 0 ? cell.tank.fluid() : fluid;
        }
        int capacity = blocks() * CAPACITY;
        if (amount == 0) {
            return Component.translatable("message.cyberfarm.tank.empty", String.format("%,d", capacity));
        }
        return Component.translatable("message.cyberfarm.tank.contents", fluid.getFluidType().getDescription(),
                String.format("%,d", amount), String.format("%,d", capacity));
    }

    /** Why a bucket didn't go in: the wrong kind of fluid, a different one from what's inside, or no room. */
    public Component refusal() {
        return Component.translatable(gasKind() ? "message.cyberfarm.tank.refused_gas" : "message.cyberfarm.tank.refused_liquid", status());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        output.putLong("box_min", boxMin.asLong());
        output.putLong("box_max", boxMax.asLong());
        output.putString("shown_fluid", BuiltInRegistries.FLUID.getKey(shownFluid).toString());
        output.putInt("shown_amount", shownAmount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
        boxMin = BlockPos.of(input.getLongOr("box_min", worldPosition.asLong()));
        boxMax = BlockPos.of(input.getLongOr("box_max", worldPosition.asLong()));
        Identifier fluid = Identifier.tryParse(input.getStringOr("shown_fluid", "minecraft:empty"));
        shownFluid = fluid != null ? BuiltInRegistries.FLUID.getValue(fluid) : Fluids.EMPTY;
        shownAmount = input.getIntOr("shown_amount", 0);
        handler = null;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return null;
    }

    /** All a tank's blocks as one handler. */
    public record Handler(GasTank[] tanks) implements ResourceHandler<FluidResource> {
        @Override
        public int size() {
            return tanks.length;
        }

        @Override
        public FluidResource getResource(int index) {
            return tanks[index].getResource(0);
        }

        @Override
        public long getAmountAsLong(int index) {
            return tanks[index].amount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return tanks[index].capacity();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return tanks[index].isValid(0, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return tanks[index].insert(0, resource, amount, transaction);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return tanks[index].extract(0, resource, amount, transaction);
        }
    }
}
