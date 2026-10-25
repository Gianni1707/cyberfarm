package com.cyberfarm.block.entity;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Hologram Table: saves only how far it shows (its zoom). On the client it keeps the last map of the
 * land its renderer drew, so the relief is read again only every couple of seconds.
 */
public class HologramTableBlockEntity extends BlockEntity {
    /** How many blocks every way the map shows, one per right-click in turn. */
    public static final int[] RADII = {16, 24, 32};

    private int zoom = 1;

    // Client-side cache for HologramRenderer, never saved
    public long mappedAt;
    public int mapRadius;
    public float[] mapTops = new float[0];
    public int[] mapColours = new int[0];
    /** The top of the tallest column shown, for the scan line and the summary above it. */
    public float mapPeak;
    public List<Vec3> mapDocks = List.of();
    /** World height of the projection's floor: the ground most of the map stands on. */
    public int mapLowest;
    /** How far the projection has risen (0 asleep, 1 on), and when the renderer last moved it. */
    public float power;
    public float lastFrame;
    /** The last few map positions of each drone, by entity id, and when they were last sampled. */
    public final Map<Integer, ArrayDeque<Vec3>> trails = new HashMap<>();
    public long trailTick;

    public HologramTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOLOGRAM_TABLE.get(), pos, state);
    }

    public int radius() {
        return RADII[zoom];
    }

    /** The next range in turn; returns the new radius. */
    public int cycleZoom() {
        zoom = (zoom + 1) % RADII.length;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        return radius();
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
        output.putInt("zoom", zoom);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        zoom = Math.floorMod(input.getIntOr("zoom", 1), RADII.length);
    }
}
