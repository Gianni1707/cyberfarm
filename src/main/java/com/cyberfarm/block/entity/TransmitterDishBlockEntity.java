package com.cyberfarm.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.cyberfarm.energy.DishNetworks;
import com.cyberfarm.item.RangeModuleItem;
import com.cyberfarm.menu.TransmitterDishMenu;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
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
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Transmitter Dish: a 2x2 small multiblock (the dish and three Cyber Frames) that owns one dish network and sends FE to
 * its receivers, at most four, in turn: it turns to the next one, holds still while it transmits, then moves on. No
 * beam: the dish pointing, and the receiver's rings lighting up, are the only signs. A tenth of the FE is lost on the
 * way. The range module sets how far it reaches and how much it sends.
 */
public class TransmitterDishBlockEntity extends SmallMultiblockBlockEntity {
    public static final int MODULE = 0;
    /** Ticks to turn to the next receiver, and to hold on it while transmitting. */
    public static final int TURN_TICKS = 20;
    public static final int HOLD_TICKS = 80;
    private static final int OPEN_TICKS = 40;
    private static final int BASE_CAPACITY = 20_000;
    private static final int INFINITE_CAPACITY = 1_000_000_000;
    /** The share that arrives: 9 FE of every 10 sent. */
    private static final int ARRIVE_TENTHS = 9;

    private @Nullable UUID networkId;
    private int appliedTier = -1;
    private int served;
    private int turnTicks;
    private int holdTicks;
    /** Synced to clients: the receiver it points at, the network's colour and whether it is transmitting. */
    private @Nullable GlobalPos aim;
    private int colour = -1;
    private boolean transmitting;

    public TransmitterDishBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRANSMITTER_DISH.get(), pos, state, 1, BASE_CAPACITY, RangeModuleItem.power(0), 0);
    }

    @Override
    public int openTicks() {
        return OPEN_TICKS;
    }

    public @Nullable UUID networkId() {
        return networkId;
    }

    public @Nullable GlobalPos aim() {
        return aim;
    }

    public int colour() {
        return colour;
    }

    public boolean transmitting() {
        return transmitting;
    }

    public int tier() {
        return RangeModuleItem.tierOf(getStack(MODULE));
    }

    /** Makes this dish's network on first use, owned by {@code player}. */
    public DishNetworks.Network network(ServerPlayer player) {
        DishNetworks networks = DishNetworks.get(player.level().getServer());
        DishNetworks.Network network = networks.get(networkId);
        if (network == null) {
            network = networks.create(player, GlobalPos.of(level.dimension(), worldPosition));
            networkId = network.id;
            setChanged();
        }
        return network;
    }

    /** Whether a receiver there is within range: the same dimension within the module's blocks, or anywhere with the Infinity module. */
    public boolean reaches(GlobalPos receiver) {
        int range = RangeModuleItem.range(tier());
        if (range < 0) {
            return true;
        }
        return level != null && receiver.dimension() == level.dimension() && receiver.pos().distSqr(worldPosition) <= (double) range * range;
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return resource.getItem() instanceof RangeModuleItem;
    }

    @Override
    protected int getSlotLimit(int index, ItemResource resource) {
        return 1;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = advanceStage(level, pos, state);
        applyTier();
        DishNetworks.Network network = DishNetworks.get(level.getServer()).get(networkId);
        GlobalPos target = null;
        boolean sending = false;
        if (open && network != null) {
            List<ReceiverDishBlockEntity> ready = new ArrayList<>();
            List<GlobalPos> where = new ArrayList<>();
            for (GlobalPos receiver : network.receivers) {
                ReceiverDishBlockEntity dish = loadedReceiver(level, receiver);
                if (dish != null && dish.isOpen() && network.id.equals(dish.networkId()) && reaches(receiver)) {
                    ready.add(dish);
                    where.add(receiver);
                }
            }
            if (!ready.isEmpty()) {
                served %= ready.size();
                target = where.get(served);
                if (!target.equals(aim)) {
                    turnTicks = TURN_TICKS;
                    holdTicks = 0;
                } else if (turnTicks > 0) {
                    turnTicks--;
                } else {
                    sending = send(level, ready.get(served));
                    if (ready.size() > 1 && ++holdTicks >= HOLD_TICKS) {
                        served = (served + 1) % ready.size();
                    }
                }
            }
        }
        int nowColour = network != null ? network.rgb() : -1;
        if (!Objects.equals(target, aim) || sending != transmitting || nowColour != colour) {
            aim = target;
            transmitting = sending;
            colour = nowColour;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Sends one tick's FE to the receiver; true if any arrived. */
    private boolean send(ServerLevel level, ReceiverDishBlockEntity receiver) {
        int power = RangeModuleItem.power(tier());
        int sent = power < 0 ? energy.getEnergy() : Math.min(energy.getEnergy(), power);
        if (sent <= 0 || !(receiver.getLevel() instanceof ServerLevel there)) {
            return false;
        }
        int arrived = receiver.receive((int) ((long) sent * ARRIVE_TENTHS / 10), there);
        if (arrived <= 0) {
            return false;
        }
        energy.drain((int) Math.min(sent, ((long) arrived * 10 + ARRIVE_TENTHS - 1) / ARRIVE_TENTHS));
        return true;
    }

    private static @Nullable ReceiverDishBlockEntity loadedReceiver(ServerLevel here, GlobalPos receiver) {
        ServerLevel there = here.getServer().getLevel(receiver.dimension());
        return there != null && there.isLoaded(receiver.pos()) && there.getBlockEntity(receiver.pos()) instanceof ReceiverDishBlockEntity dish ? dish : null;
    }

    /** The buffer and what cables may put in follow the module's power; with Infinity there is no practical limit. */
    private void applyTier() {
        int tier = tier();
        if (tier == appliedTier) {
            return;
        }
        appliedTier = tier;
        int power = RangeModuleItem.power(tier);
        energy.setCapacity(power < 0 ? INFINITE_CAPACITY : Math.max(BASE_CAPACITY, power * 10));
        energy.setTransferLimits(power < 0 ? Integer.MAX_VALUE : power, 0);
    }

    /** Receivers of its network, for the GUI: where each is, as a distance or a dimension, in the order it serves them. */
    public List<String> receiverLabels() {
        List<String> labels = new ArrayList<>();
        if (!(level instanceof ServerLevel server)) {
            return labels;
        }
        DishNetworks.Network network = DishNetworks.get(server.getServer()).get(networkId);
        if (network != null) {
            for (GlobalPos receiver : network.receivers) {
                labels.add(receiver.dimension() == server.dimension()
                        ? String.format("%,d blocks", Math.round(Math.sqrt(receiver.pos().distSqr(worldPosition))))
                        : receiver.dimension().identifier().getPath().replace('_', ' '));
            }
        }
        return labels;
    }

    /** Which receiver it serves now, in {@link #receiverLabels} order, or -1. */
    public int servedIndex() {
        if (aim == null || !(level instanceof ServerLevel server)) {
            return -1;
        }
        DishNetworks.Network network = DishNetworks.get(server.getServer()).get(networkId);
        return network != null ? network.receivers.indexOf(aim) : -1;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && networkId != null) {
            DishNetworks.get(server.getServer()).remove(networkId);
        }
    }

    @Override
    protected boolean isWorking() {
        return transmitting;
    }

    @Override
    protected int getExtraData(int id) {
        if (!(level instanceof ServerLevel server)) {
            return 0;
        }
        DishNetworks.Network network = DishNetworks.get(server.getServer()).get(networkId);
        return switch (id) {
            case 0 -> network != null ? network.colour : 0;
            case 1 -> network != null && network.isPublic ? 1 : 0;
            case 2 -> tier();
            case 3 -> servedIndex() + 1;
            case 4 -> network != null ? network.receivers.size() : 0;
            case 5 -> transmitting ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 6;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("network", UUIDUtil.CODEC, networkId);
        output.storeNullable("aim", GlobalPos.CODEC, aim);
        output.putInt("colour", colour);
        output.putBoolean("transmitting", transmitting);
        output.putInt("served", served);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        networkId = input.read("network", UUIDUtil.CODEC).orElse(null);
        aim = input.read("aim", GlobalPos.CODEC).orElse(null);
        colour = input.getIntOr("colour", -1);
        transmitting = input.getBooleanOr("transmitting", false);
        served = input.getIntOr("served", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        String name = "";
        if (player instanceof ServerPlayer server) {
            name = network(server).name;
        }
        return new TransmitterDishMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition), name, receiverLabels());
    }
}
