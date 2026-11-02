package com.cyberfarm.village;

import com.cyberfarm.port.net.PortCodecs;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.registry.ModVillagers;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import com.cyberfarm.port.net.ByteBufCodecs;
import com.cyberfarm.port.net.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** The village buildings a player has learned from Blueprint Discs: only these show in their Multiblock Projector. */
public record Blueprints(List<ResourceLocation> learned) {
    public static final Blueprints NONE = new Blueprints(List.of());
    public static final MapCodec<Blueprints> CODEC = ResourceLocation.CODEC.listOf().fieldOf("learned").xmap(Blueprints::new, Blueprints::learned);
    public static final StreamCodec<ByteBuf, Blueprints> STREAM_CODEC = PortCodecs.RESOURCE_LOCATION.apply(ByteBufCodecs.list())
            .map(Blueprints::new, Blueprints::learned);

    public static Blueprints of(Player player) {
        return player.getData(ModVillagers.BLUEPRINTS);
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
