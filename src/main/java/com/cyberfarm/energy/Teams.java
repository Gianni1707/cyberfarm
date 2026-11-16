package com.cyberfarm.energy;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.cyberfarm.CyberFarm;
import com.mojang.logging.LogUtils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Whether a player is on a network owner's team: the vanilla scoreboard team, or, when installed, a team of FTB Teams or
 * a party of Open Parties and Claims. The team mods are reached by name through their public API, with no dependency
 * to build against: FTB Teams has no 26.2 release yet, and this keeps working whichever version a server runs.
 */
public final class Teams {
    private static final Logger LOG = LogUtils.getLogger();
    private static boolean ftbBroken, opacBroken;

    private Teams() {}

    /** Whether {@code player} shares a team with the owner (by UUID; {@code ownerName} for the scoreboard). */
    public static boolean sameTeam(ServerPlayer player, UUID owner, String ownerName) {
        if (player.getUUID().equals(owner)) {
            return true;
        }
        PlayerTeam team = player.level().getScoreboard().getPlayersTeam(ownerName);
        if (team != null && team == player.getTeam()) {
            return true;
        }
        return ftbTeams(player.getUUID(), owner) || openParties(player.level().getServer(), player.getUUID(), owner);
    }

    /** FTB Teams: {@code FTBTeamsAPI.api().getManager().arePlayersInSameTeam(a, b)}. */
    private static boolean ftbTeams(UUID a, UUID b) {
        if (ftbBroken || !ModList.get().isLoaded("ftbteams")) {
            return false;
        }
        try {
            Object api = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI").getMethod("api").invoke(null);
            Object manager = call(api, "getManager");
            return (boolean) call(manager, "arePlayersInSameTeam", new Class<?>[] {UUID.class, UUID.class}, a, b);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            ftbBroken = true;
            LOG.warn("{}: FTB Teams is installed but its API did not answer; network teams use the scoreboard only", CyberFarm.MODID, e);
            return false;
        }
    }

    /** Open Parties and Claims: the two players' parties, {@code OpenPACServerAPI.get(server).getPartyManager().getPartyByMember(id)}. */
    private static boolean openParties(MinecraftServer server, UUID a, UUID b) {
        if (opacBroken || !ModList.get().isLoaded("openpartiesandclaims")) {
            return false;
        }
        try {
            Object api = Class.forName("xaero.pac.common.server.api.OpenPACServerAPI").getMethod("get", MinecraftServer.class).invoke(null, server);
            Object parties = call(api, "getPartyManager");
            Object first = party(parties, a), second = party(parties, b);
            return first != null && second != null && Objects.equals(id(first), id(second));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            opacBroken = true;
            LOG.warn("{}: Open Parties and Claims is installed but its API did not answer; network teams use the scoreboard only", CyberFarm.MODID, e);
            return false;
        }
    }

    private static @Nullable Object party(Object parties, UUID member) throws ReflectiveOperationException {
        return call(parties, "getPartyByMember", new Class<?>[] {UUID.class}, member);
    }

    private static @Nullable Object id(Object party) throws ReflectiveOperationException {
        return call(party, "getId");
    }

    private static @Nullable Object call(Object target, String name) throws ReflectiveOperationException {
        return call(target, name, new Class<?>[0]);
    }

    /** A public API method on {@code target}, found on a public type: the implementation class itself may be hidden. */
    static @Nullable Object call(Object target, String name, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        ArrayDeque<Class<?>> todo = new ArrayDeque<>(List.of(target.getClass()));
        while (!todo.isEmpty()) {
            Class<?> type = todo.poll();
            if (Modifier.isPublic(type.getModifiers())) {
                try {
                    return type.getMethod(name, types).invoke(target, args);
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    // not on this type: look further up
                }
            }
            if (type.getSuperclass() != null) {
                todo.add(type.getSuperclass());
            }
            todo.addAll(List.of(type.getInterfaces()));
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name);
    }
}
