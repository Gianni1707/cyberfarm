package com.cyberfarm.multiblock;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cyberfarm.CyberFarm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The schematics of the loaded data packs: read by the server when it starts and on /reload, and
 * sent to every player who joins (see {@link SchematicsPayload}).
 */
public final class Schematics {
    // shortcut: one static list, which in single player the integrated server and its client share; both hold the same schematics
    private static List<Schematic> all = List.of();

    private Schematics() {}

    public static List<Schematic> all() {
        return all;
    }

    public static Optional<Schematic> byId(Identifier id) {
        return all.stream().filter(schematic -> schematic.id().equals(id)).findFirst();
    }

    /** What the server sent, on the client. */
    public static void set(List<Schematic> schematics) {
        all = List.copyOf(schematics);
    }

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        load(event.getServer());
    }

    /** A player joins (they get the list) or /reload ran (read the packs again, then everyone gets it). */
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            load(event.getPlayerList().getServer());
        }
        SchematicsPayload payload = new SchematicsPayload(all);
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    private static void load(MinecraftServer server) {
        StructureTemplateManager manager = server.getStructureManager();
        var blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        all = manager.listTemplates()
                .filter(Schematic::isSchematic)
                .sorted(Comparator.comparing(Identifier::toString))
                .map(id -> manager.get(id).map(template -> {
                    Schematic schematic = Schematic.of(id, template, blocks);
                    if (schematic == null) {
                        CyberFarm.LOGGER.warn("Skipping schematic {}: empty or bigger than {} blocks a side", id, Schematic.MAX_SIZE);
                    }
                    return schematic;
                }).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        CyberFarm.LOGGER.info("Loaded {} schematics", all.size());
    }
}
