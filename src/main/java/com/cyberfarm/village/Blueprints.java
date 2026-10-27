package com.cyberfarm.village;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.registry.ModVillagers;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** The village buildings a player has learned from Blueprint Discs: only these show in their Multiblock Projector. */
public record Blueprints(List<Identifier> learned) {
    public static final Blueprints NONE = new Blueprints(List.of());
    public static final MapCodec<Blueprints> CODEC = Identifier.CODEC.listOf().fieldOf("learned").xmap(Blueprints::new, Blueprints::learned);
    public static final StreamCodec<ByteBuf, Blueprints> STREAM_CODEC = Identifier.STREAM_CODEC.apply(ByteBufCodecs.list())
            .map(Blueprints::new, Blueprints::learned);

    public static Blueprints of(Player player) {
        return player.getData(ModVillagers.BLUEPRINTS);
    }

    public boolean knows(Identifier id) {
        return learned.contains(id);
    }

    public Blueprints with(Identifier id) {
        List<Identifier> more = new ArrayList<>(learned);
        more.add(id);
        return new Blueprints(List.copyOf(more));
    }

    /** A village building shows in the projector once learned (always in creative); anything else always does. */
    public static boolean canProject(Player player, Identifier structure) {
        return !VillagePlan.isVillage(structure) || player.isCreative() || of(player).knows(structure);
    }
}
