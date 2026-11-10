package com.cyberfarm.energy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cyberfarm.CyberFarm;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

/**
 * Every dish network of the world, kept with the Overworld: one per Transmitter Dish, with its name, colour, owner,
 * whether anyone may join, and the Receiver Dishes linked to it (at most {@link #MAX_RECEIVERS}).
 */
public class DishNetworks extends SavedData {
    public static final int MAX_RECEIVERS = 4;
    public static final int MAX_NAME = 24;
    /** The colours a network can take, as the indicator lights show them. */
    public static final int[] COLOURS = {0xFF9F2E, 0xC46BFF, 0x3FD0FF, 0xFF5A6E, 0x9BE15D, 0xFFD84A, 0x5A7CFF, 0xE8F0EE};

    public static final class Network {
        static final Codec<Network> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(n -> n.id),
                Codec.STRING.fieldOf("name").forGetter(n -> n.name),
                Codec.INT.fieldOf("colour").forGetter(n -> n.colour),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(n -> n.owner),
                Codec.STRING.fieldOf("owner_name").forGetter(n -> n.ownerName),
                Codec.BOOL.fieldOf("public").forGetter(n -> n.isPublic),
                GlobalPos.CODEC.fieldOf("transmitter").forGetter(n -> n.transmitter),
                GlobalPos.CODEC.listOf().fieldOf("receivers").forGetter(n -> n.receivers)).apply(i, Network::new));

        public final UUID id;
        public String name;
        /** Index into {@link #COLOURS}. */
        public int colour;
        public final UUID owner;
        public final String ownerName;
        public boolean isPublic;
        public final GlobalPos transmitter;
        public final List<GlobalPos> receivers;

        Network(UUID id, String name, int colour, UUID owner, String ownerName, boolean isPublic, GlobalPos transmitter, List<GlobalPos> receivers) {
            this.id = id;
            this.name = name;
            this.colour = colour;
            this.owner = owner;
            this.ownerName = ownerName;
            this.isPublic = isPublic;
            this.transmitter = transmitter;
            this.receivers = new ArrayList<>(receivers);
        }

        public int rgb() {
            return COLOURS[Math.floorMod(colour, COLOURS.length)];
        }

        public boolean isFull() {
            return receivers.size() >= MAX_RECEIVERS;
        }

        /** Public networks for everyone; private ones for their owner and the players on the owner's team. */
        public boolean visibleTo(ServerPlayer player) {
            if (isPublic || player.getUUID().equals(owner)) {
                return true;
            }
            PlayerTeam team = player.level().getScoreboard().getPlayersTeam(ownerName);
            return team != null && team == player.getTeam();
        }
    }

    public static final Codec<DishNetworks> CODEC = Network.CODEC.listOf().xmap(DishNetworks::new, data -> List.copyOf(data.networks.values()));
    private static final String NAME = CyberFarm.MODID + "_dish_networks";

    private final Map<UUID, Network> networks = new LinkedHashMap<>();

    public DishNetworks() {}

    private DishNetworks(List<Network> list) {
        list.forEach(network -> networks.put(network.id, network));
    }

    public static DishNetworks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DishNetworks::load, DishNetworks::new, NAME);
    }

    private static DishNetworks load(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag.get("networks")).result().orElseGet(DishNetworks::new);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CODEC.encodeStart(NbtOps.INSTANCE, this).result().ifPresent(encoded -> tag.put("networks", encoded));
        return tag;
    }

    public Network create(ServerPlayer owner, GlobalPos transmitter) {
        Network network = new Network(UUID.randomUUID(), owner.getGameProfile().getName() + "'s Network", 0, owner.getUUID(),
                owner.getGameProfile().getName(), false, transmitter, List.of());
        networks.put(network.id, network);
        setDirty();
        return network;
    }

    public @Nullable Network get(@Nullable UUID id) {
        return id == null ? null : networks.get(id);
    }

    public void remove(UUID id) {
        if (networks.remove(id) != null) {
            setDirty();
        }
    }

    public Iterable<Network> all() {
        return networks.values();
    }

    /** Links a receiver to the network, leaving any other first; false when it is full. */
    public boolean link(Network network, GlobalPos receiver) {
        unlink(receiver);
        if (network.isFull()) {
            return false;
        }
        network.receivers.add(receiver);
        setDirty();
        return true;
    }

    public void unlink(GlobalPos receiver) {
        for (Network network : networks.values()) {
            if (network.receivers.remove(receiver)) {
                setDirty();
            }
        }
    }

    /** Call after changing a network's name, colour or visibility. */
    public void changed() {
        setDirty();
    }
}
