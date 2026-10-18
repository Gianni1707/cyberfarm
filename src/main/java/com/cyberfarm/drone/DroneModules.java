package com.cyberfarm.drone;

import java.util.List;
import java.util.Optional;

import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.item.DroneModuleItem;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/** What a drone was built from: one frame plus its function modules. Stored on the drone item and entity. */
public record DroneModules(List<Item> parts) {
    public static final DroneModules EMPTY = new DroneModules(List.of());

    public static final Codec<DroneModules> CODEC = BuiltInRegistries.ITEM.byNameCodec().listOf()
            .xmap(DroneModules::new, DroneModules::parts);

    public static final StreamCodec<RegistryFriendlyByteBuf, DroneModules> STREAM_CODEC = ByteBufCodecs.registry(Registries.ITEM)
            .apply(ByteBufCodecs.list())
            .map(DroneModules::new, DroneModules::parts);

    public DroneModules {
        parts = List.copyOf(parts);
    }

    public Optional<DroneFrameItem> frame() {
        return parts.stream()
                .filter(DroneFrameItem.class::isInstance)
                .map(DroneFrameItem.class::cast)
                .findFirst();
    }

    public List<DroneModuleItem> modules() {
        return parts.stream()
                .filter(DroneModuleItem.class::isInstance)
                .map(DroneModuleItem.class::cast)
                .toList();
    }

    public boolean has(Item item) {
        return parts.contains(item);
    }

    public int energyCapacity() {
        return frame().map(DroneFrameItem::getEnergyCapacity).orElse(0);
    }

    /** Navigation speed multiplier given by the frame tier. */
    public double speed() {
        return frame().map(frame -> frame.getSpeedPercent() / 100.0).orElse(1.0);
    }
}
