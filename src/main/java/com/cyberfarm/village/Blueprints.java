package com.cyberfarm.village;

import com.cyberfarm.port.net.PortCodecs;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import com.cyberfarm.port.net.ByteBufCodecs;
import com.cyberfarm.port.net.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import com.cyberfarm.port.net.PacketDistributor;

/** The village buildings a player has learned from Blueprint Discs: only these show in their Multiblock Projector. */
public record Blueprints(List<ResourceLocation> learned) {
    public static final Blueprints NONE = new Blueprints(List.of());
    public static final MapCodec<Blueprints> CODEC = ResourceLocation.CODEC.listOf().fieldOf("learned").xmap(Blueprints::new, Blueprints::learned);
    public static final StreamCodec<ByteBuf, Blueprints> STREAM_CODEC = PortCodecs.RESOURCE_LOCATION.apply(ByteBufCodecs.list())
            .map(Blueprints::new, Blueprints::learned);

    private static final String TAG = "cyberfarm.blueprints";
    /** Client side: what the server last said the local player knows. */
    static Blueprints client = NONE;

    /** Kept in the player's persisted data, which Forge carries over death. */
    public static Blueprints of(Player player) {
        if (player.level().isClientSide()) {
            return client;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return CODEC.codec().parse(NbtOps.INSTANCE, persisted.get(TAG)).result().orElse(NONE);
    }

    public static void set(ServerPlayer player, Blueprints blueprints) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        CODEC.codec().encodeStart(NbtOps.INSTANCE, blueprints).result().ifPresent(tag -> persisted.put(TAG, tag));
        data.put(Player.PERSISTED_NBT_TAG, persisted);
        PacketDistributor.sendToPlayer(player, new BlueprintsPayload(blueprints));
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new BlueprintsPayload(of(player)));
        }
    }

    public boolean knows(ResourceLocation id) {
        return learned.contains(id);
    }

    public Blueprints with(ResourceLocation id) {
        List<ResourceLocation> more = new ArrayList<>(learned);
        more.add(id);
        return new Blueprints(List.copyOf(more));
    }

    /** A village building shows in the projector once learned (always in creative); anything else always does. */
    public static boolean canProject(Player player, ResourceLocation structure) {
        return !VillagePlan.isVillage(structure) || player.isCreative() || of(player).knows(structure);
    }
}
