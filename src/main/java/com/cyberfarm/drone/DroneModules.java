package com.cyberfarm.drone;

import java.util.List;
import java.util.Optional;

import com.cyberfarm.item.DroneAddonItem;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.item.DroneModuleItem;
import com.cyberfarm.registry.ModItems;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/** What a drone was built from: one frame, its function modules and its addons (one entry per addon). */
public record DroneModules(List<Item> parts) {
    public static final DroneModules EMPTY = new DroneModules(List.of());
    private static final double SPEED_PER_ADDON = 0.10;
    private static final double BATTERY_PER_ADDON = 0.25;
    private static final int BASE_TRANSPORT = 16;
    private static final int STACK_PER_ADDON = 64;
    private static final int MAX_TRANSPORT = 9 * 64;

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

    /** The jobs this drone can do, in the order its modules were installed. */
    public List<DroneTask> tasks() {
        return parts.stream()
                .filter(DroneModuleItem.class::isInstance)
                .map(item -> ((DroneModuleItem) item).getTask())
                .toList();
    }

    /** Built on a Mini Frame: half size, and welcome on a Builder Swarm Dock. */
    public boolean isMini() {
        return frame().filter(frame -> frame == ModItems.MINI_FRAME.get()).isPresent();
    }

    public int taskMask() {
        return tasks().stream().mapToInt(DroneTask::bit).reduce(0, (a, b) -> a | b);
    }

    /** Battery size: the frame's capacity, plus 25% per Battery Addon. */
    public int energyCapacity() {
        int frameCapacity = frame().map(DroneFrameItem::getEnergyCapacity).orElse(0);
        return (int) (frameCapacity * (1.0 + BATTERY_PER_ADDON * addonCount(AddonType.BATTERY)));
    }

    /** FE per tick while flying: (frame + modules) times (1 + the addons' flight percentages), rounded up. */
    public int flightCost() {
        int base = 0;
        int percent = 0;
        for (Item part : parts) {
            if (part instanceof DroneFrameItem frame) {
                base += frame.getFlightCost();
            } else if (part instanceof DroneModuleItem module) {
                base += module.getFlightCost();
            } else if (part instanceof DroneAddonItem addon) {
                percent += addon.getAddonType().getFlightCostPercent();
            }
        }
        return (int) Math.ceil(base * (100 + percent) / 100.0);
    }

    /** Multiplier on the energy of every job, from the Fortune and Silk Touch addons. */
    public double jobCostMultiplier() {
        int percent = 0;
        for (Item part : parts) {
            if (part instanceof DroneAddonItem addon) {
                percent += addon.getAddonType().getJobCostPercent();
            }
        }
        return (100 + percent) / 100.0;
    }

    public int addonCount(AddonType type) {
        return (int) parts.stream()
                .filter(item -> item instanceof DroneAddonItem addon && addon.getAddonType() == type)
                .count();
    }

    /** Speed multiplier: the frame tier, plus 10% per Speed Addon. */
    public double speed() {
        double frameSpeed = frame().map(frame -> frame.getSpeedPercent() / 100.0).orElse(1.0);
        return frameSpeed * (1.0 + SPEED_PER_ADDON * addonCount(AddonType.SPEED));
    }

    /** Items carried per Transport trip: 16, plus one stack per Stack Addon, up to the 9 cargo slots. */
    public int transportCapacity() {
        return Math.min(BASE_TRANSPORT + STACK_PER_ADDON * addonCount(AddonType.STACK), MAX_TRANSPORT);
    }

    public int fortuneLevel() {
        return Math.min(addonCount(AddonType.FORTUNE), AddonType.FORTUNE.getMaxPerDrone());
    }

    public boolean hasSilkTouch() {
        return addonCount(AddonType.SILK_TOUCH) > 0;
    }
}
