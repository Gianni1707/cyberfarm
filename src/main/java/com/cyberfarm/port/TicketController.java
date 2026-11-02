package com.cyberfarm.port;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;

/** The 26.x chunk ticket controller on Forge's ForgeChunkManager (one per mod id). */
public final class TicketController {
    private final String modId;
    private final ForgeChunkManager.LoadingValidationCallback callback;

    public TicketController(ResourceLocation id, ForgeChunkManager.LoadingValidationCallback callback) {
        this.modId = id.getNamespace();
        this.callback = callback;
    }

    /** Call while the mod is being constructed, before any world loads. */
    public void register() {
        ForgeChunkManager.setForcedChunkLoadingCallback(modId, callback);
    }

    public boolean forceChunk(ServerLevel level, BlockPos owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
        return ForgeChunkManager.forceChunk(level, modId, owner, chunkX, chunkZ, add, ticking);
    }
}
