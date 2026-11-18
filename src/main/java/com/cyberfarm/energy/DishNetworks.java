package com.cyberfarm.energy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cyberfarm.CyberFarm;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
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
        /** Who a network is for: its owner only, the owner's team too, or everyone. */
        public static final int PRIVATE = 0, TEAM = 1, PUBLIC = 2;

        static final Codec<Network> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(n -> n.id),
                Codec.STRING.fieldOf("name").forGetter(n -> n.name),
                Codec.INT.fieldOf("colour").forGetter(n -> n.colour),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(n -> n.owner),
                Codec.STRING.fieldOf("owner_name").forGetter(n -> n.ownerName),
                // Saved before the Team mode: "public" alone, and a private network then meant the owner's team
                Codec.BOOL.optionalFieldOf("public", false).forGetter(n -> n.access == PUBLIC),
                Codec.INT.optionalFieldOf("access").forGetter(n -> Optional.of(n.access)),
                GlobalPos.CODEC.fieldOf("transmitter").forGetter(n -> n.transmitter),
                GlobalPos.CODEC.listOf().fieldOf("receivers").forGetter(n -> n.receivers)).apply(i,
                (id, name, colour, owner, ownerName, isPublic, access, transmitter, receivers) -> new Network(id, name, colour, owner,
                        ownerName, access.orElse(isPublic ? PUBLIC : TEAM), transmitter, receivers)));

        public final UUID id;
        public String name;
        /** Index into {@link #COLOURS}. */
        public int colour;
        public final UUID owner;
        /** The owner's name, for the scoreboard team: refreshed whenever the owner is online, so a rename catches up. */
        public String ownerName;
        /** {@link #PRIVATE}, {@link #TEAM} or {@link #PUBLIC}: who may link a receiver and charge a sphere from it. */
        public int access;
        public final GlobalPos transmitter;
        public final List<GlobalPos> receivers;

        Network(UUID id, String name, int colour, UUID owner, String ownerName, int access, GlobalPos transmitter, List<GlobalPos> receivers) {
            this.id = id;
            this.name = name;
            this.colour = colour;
            this.owner = owner;
            this.ownerName = ownerName;
            this.access = Math.clamp(access, PRIVATE, PUBLIC);
            this.transmitter = transmitter;
            this.receivers = new ArrayList<>(receivers);
        }

        public int rgb() {
            return COLOURS[Math.floorMod(colour, COLOURS.length)];
        }

        public boolean isFull() {
            return receivers.size() >= MAX_RECEIVERS;
        }

        /** Who it is for, by its access: links a Receiver Dish to it and has an Energy Sphere charged by it. */
        public boolean visibleTo(ServerPlayer player) {
            return access == PUBLIC || player.getUUID().equals(owner) || access == TEAM && sameTeam(player);
        }

        private boolean sameTeam(ServerPlayer player) {
            ServerPlayer online = player.level().getServer().getPlayerList().getPlayer(owner);
            if (online != null && !online.getScoreboardName().equals(ownerName)) {
                ownerName = online.getScoreboardName();
                DishNetworks.get(player.level().getServer()).changed();
            }
            return Teams.sameTeam(player, owner, ownerName);
        }

        /** Name, colour and visibility: only for the owner and the players on the owner's team, public or not. */
        public boolean canEdit(ServerPlayer player) {
            return player.getUUID().equals(owner) || sameTeam(player);
        }
    }

    public static final Codec<DishNetworks> CODEC = Network.CODEC.listOf().xmap(DishNetworks::new, data -> List.copyOf(data.networks.values()));
    public static final SavedDataType<DishNetworks> TYPE = new SavedDataType<>(CyberFarm.id("dish_networks"), DishNetworks::new, CODEC);

    private final Map<UUID, Network> networks = new LinkedHashMap<>();

    public DishNetworks() {}

    private DishNetworks(List<Network> list) {
        list.forEach(network -> networks.put(network.id, network));
    }

    public static DishNetworks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Network create(ServerPlayer owner, GlobalPos transmitter) {
        Network network = new Network(UUID.randomUUID(), owner.getGameProfile().name() + "'s Network", 0, owner.getUUID(),
                owner.getGameProfile().name(), Network.PRIVATE, transmitter, List.of());
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
        // Checked first, or a click on a full network would drop the link the receiver already has
        if (network.isFull() && !network.receivers.contains(receiver)) {
            return false;
        }
        unlink(receiver);
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
