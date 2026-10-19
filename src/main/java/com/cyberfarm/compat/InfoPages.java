package com.cyberfarm.compat;

import java.util.List;

import com.cyberfarm.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/** Short "how does this work" texts shown by the recipe viewers' information pages. */
public final class InfoPages {
    private InfoPages() {}

    public record Page(Item item, Component text) {}

    public static List<Page> all() {
        return List.of(
                page(ModItems.COMBUSTION_GENERATOR.get()),
                page(ModItems.DRONE_ASSEMBLER.get()),
                page(ModItems.DRONE_DOCK.get()),
                page(ModItems.ENERGY_CONDUIT.get()),
                page(ModItems.GAS_PIPE.get()),
                page(ModItems.BIO_DIGESTER.get()),
                page(ModItems.GAS_TURBINE.get()),
                page(ModItems.METHANE_EXTRACTOR.get()),
                page(ModItems.CYBER_FRAME.get()),
                page(ModItems.MOB_CAPSULE.get()),
                page(ModItems.MUFFLER.get()),
                page(ModItems.DRONE.get()),
                page(ModItems.SURVEY_TOOL.get()),
                page(ModItems.HARVESTER_MODULE.get()),
                page(ModItems.QUARRY_MODULE.get()),
                page(ModItems.TRANSPORT_MODULE.get()),
                page(ModItems.PLANTER_MODULE.get()),
                page(ModItems.LOGGER_MODULE.get()),
                page(ModItems.TAXI_MODULE.get()));
    }

    private static Page page(Item item) {
        return new Page(item, Component.translatable("info." + item.getDescriptionId()));
    }
}
