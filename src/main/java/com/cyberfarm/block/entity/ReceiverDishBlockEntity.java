package com.cyberfarm.block.entity;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.cyberfarm.energy.DishNetworks;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.menu.ReceiverDishMenu;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Receiver Dish: links to one dish network and gives the FE its transmitter beams in to the blocks around it. While FE
 * arrives the rings of its dish light up from the rim to the feed; it turns to its transmitter, or to the sky when the
 * transmitter is in another dimension.
 */
public class ReceiverDishBlockEntity extends SmallMultiblockBlockEntity {
    private static final int CAPACITY = 2_000_000;
    /** Ticks after the last FE arrived that it still counts as receiving, so the rings don't flicker between turns. */
    private static final int RECEIVING_TICKS = 10;
    private static final int CHECK_INTERVAL = 20;
    private static final int OPEN_TICKS = 30;

    private @Nullable UUID networkId;
    /** Synced to clients: where the transmitter is, the network's colour and whether FE is arriving. */
    private @Nullable GlobalPos transmitter;
    private int colour = -1;
    private boolean receiving;
    private long lastReceived = Long.MIN_VALUE / 2;
    /** FE that arrived this tick so far, and over the whole last tick, for the GUI. */
    private int receivedNow;
    private int receivedLast;
    private boolean open;

    public ReceiverDishBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECEIVER_DISH.get(), pos, state, 0, CAPACITY, 0, Integer.MAX_VALUE);
    }

    public @Nullable UUID networkId() {
        return networkId;
    }

    /** Client side: the transmitter it points at, or null when unlinked. */
    public @Nullable GlobalPos transmitter() {
        return transmitter;
    }

    /** Client side: the network's colour as 0xRRGGBB, or -1 when unlinked. */
    public int colour() {
        return colour;
    }

    public boolean receiving() {
        return receiving;
    }

    @Override
    public int openTicks() {
        return OPEN_TICKS;
    }

    /** Whether it is assembled and its dish open, so it may receive. */
    public boolean isOpen() {
        return open;
    }

    /** FE beamed in by the transmitter; returns how much fitted. Nothing while folded. */
    public int receive(int amount, ServerLevel level) {
        if (!open) {
            return 0;
        }
        int stored = energy.generate(amount);
        if (stored > 0) {
            lastReceived = level.getGameTime();
            receivedNow += stored;
        }
        return stored;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        open = advanceStage(level, pos, state);
        receivedLast = receivedNow;
        receivedNow = 0;
        EnergyUtil.pushToNeighbors(level, pos, energy, Integer.MAX_VALUE);
        boolean nowReceiving = level.getGameTime() - lastReceived < RECEIVING_TICKS;
        boolean changed = nowReceiving != receiving;
        receiving = nowReceiving;
        if (level.getGameTime() % CHECK_INTERVAL == 0) {
            changed |= refreshLink(level);
        }
        if (changed) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Follows the network: its colour, its transmitter, or the link gone when the network was removed. */
    private boolean refreshLink(ServerLevel level) {
        DishNetworks.Network network = DishNetworks.get(level.getServer()).get(networkId);
        GlobalPos here = GlobalPos.of(level.dimension(), worldPosition);
        if (network == null || !network.receivers.contains(here)) {
            network = null;
            networkId = null;
        }
        GlobalPos nowTransmitter = network != null ? network.transmitter : null;
        int nowColour = network != null ? network.rgb() : -1;
        boolean changed = nowColour != colour || !java.util.Objects.equals(nowTransmitter, transmitter);
        transmitter = nowTransmitter;
        colour = nowColour;
        return changed;
    }

    /** Links to the network (leaving any other), if the player may see it and it has room. */
    public boolean link(ServerPlayer player, UUID id) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        DishNetworks networks = DishNetworks.get(server.getServer());
        DishNetworks.Network network = networks.get(id);
        if (network == null || !network.visibleTo(player) || !inRange(network, server)) {
            return false;
        }
        if (!networks.link(network, GlobalPos.of(server.dimension(), worldPosition))) {
            return false;
        }
        networkId = id;
        refreshLink(server);
        setChanged();
        server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    public void unlink() {
        if (level instanceof ServerLevel server) {
            DishNetworks.get(server.getServer()).unlink(GlobalPos.of(server.dimension(), worldPosition));
            networkId = null;
            refreshLink(server);
            setChanged();
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Whether the network's transmitter reaches this dish with the range module it has now. */
    private boolean inRange(DishNetworks.Network network, ServerLevel here) {
        ServerLevel there = here.getServer().getLevel(network.transmitter.dimension());
        if (there == null || !there.isLoaded(network.transmitter.pos())
                || !(there.getBlockEntity(network.transmitter.pos()) instanceof TransmitterDishBlockEntity transmitter)) {
            // An unloaded transmitter can't say its range: let the link wait for it
            return true;
        }
        return transmitter.reaches(GlobalPos.of(here.dimension(), worldPosition));
    }

    /** One row of the network list: what the GUI shows, and the network it links to. */
    public record Choice(UUID id, String name, int rgb, String where, int used, boolean linked) {}

    /** The networks this player may link to from here, nearest first, with the one it is on. */
    public List<Choice> choices(ServerPlayer player) {
        List<Choice> list = new ArrayList<>();
        if (!(level instanceof ServerLevel server)) {
            return list;
        }
        for (DishNetworks.Network network : DishNetworks.get(server.getServer()).all()) {
            boolean linked = network.id.equals(networkId);
            if (!linked && (!network.visibleTo(player) || !inRange(network, server))) {
                continue;
            }
            String where = network.transmitter.dimension() == server.dimension()
                    ? String.format(Locale.ROOT, "%,d blocks", Math.round(Math.sqrt(network.transmitter.pos().distSqr(worldPosition))))
                    : network.transmitter.dimension().identifier().getPath().replace('_', ' ');
            list.add(new Choice(network.id, network.name, network.rgb(), where, network.receivers.size(), linked));
        }
        list.sort((a, b) -> Boolean.compare(b.linked(), a.linked()));
        return list;
    }

    public static void writeChoices(RegistryFriendlyByteBuf buf, List<Choice> choices) {
        buf.writeVarInt(choices.size());
        for (Choice choice : choices) {
            UUIDUtil.STREAM_CODEC.encode(buf, choice.id());
            buf.writeUtf(choice.name());
            buf.writeInt(choice.rgb());
            buf.writeUtf(choice.where());
            buf.writeVarInt(choice.used());
            buf.writeBoolean(choice.linked());
        }
    }

    public static List<Choice> readChoices(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Choice> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new Choice(UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(), buf.readInt(), buf.readUtf(), buf.readVarInt(), buf.readBoolean()));
        }
        return list;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            DishNetworks.get(server.getServer()).unlink(GlobalPos.of(server.dimension(), pos));
        }
    }

    @Override
    protected boolean isWorking() {
        return receiving;
    }

    @Override
    protected int getExtraData(int id) {
        return id == 0 ? EnergyUtil.low(receivedLast) : EnergyUtil.high(receivedLast);
    }

    @Override
    protected int getExtraDataCount() {
        return 2;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("network", UUIDUtil.CODEC, networkId);
        output.storeNullable("transmitter", GlobalPos.CODEC, transmitter);
        output.putInt("colour", colour);
        output.putBoolean("receiving", receiving);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        networkId = input.read("network", UUIDUtil.CODEC).orElse(null);
        transmitter = input.read("transmitter", GlobalPos.CODEC).orElse(null);
        colour = input.getIntOr("colour", -1);
        receiving = input.getBooleanOr("receiving", false);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        List<Choice> choices = player instanceof ServerPlayer server ? choices(server) : List.of();
        return new ReceiverDishMenu(containerId, inventory, this, choices, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
