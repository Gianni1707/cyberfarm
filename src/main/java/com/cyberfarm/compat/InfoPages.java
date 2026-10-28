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
                page(ModItems.SWARM_DOCK.get()),
                page(ModItems.HOLOGRAM_TABLE.get()),
                page(ModItems.MINI_FRAME.get()),
                page(ModItems.ENERGY_CONDUIT.get()),
                page(ModItems.ADVANCED_ENERGY_CONDUIT.get()),
                page(ModItems.ELITE_ENERGY_CONDUIT.get()),
                page(ModItems.GAS_PIPE.get()),
                page(ModItems.ADVANCED_GAS_PIPE.get()),
                page(ModItems.ELITE_GAS_PIPE.get()),
                page(ModItems.LIQUID_PIPE.get()),
                page(ModItems.ADVANCED_LIQUID_PIPE.get()),
                page(ModItems.ELITE_LIQUID_PIPE.get()),
                page(ModItems.BIO_DIGESTER.get()),
                page(ModItems.GAS_TURBINE.get()),
                page(ModItems.METHANE_EXTRACTOR.get()),
                page(ModItems.CYBER_FRAME.get()),
                page(ModItems.MOB_CAPSULE.get()),
                page(ModItems.MUFFLER.get()),
                page(ModItems.REACTOR_CONTROLLER.get()),
                page(ModItems.REACTOR_CASING.get()),
                page(ModItems.REACTOR_PORT.get()),
                page(ModItems.TURBINE_CONTROLLER.get()),
                page(ModItems.TURBINE_CASING.get()),
                page(ModItems.TURBINE_PORT.get()),
                page(ModItems.WATER_PUMP.get()),
                page(ModItems.LASER_TURRET.get()),
                page(ModItems.LIQUID_TANK.get()),
                page(ModItems.GAS_TANK.get()),
                page(ModItems.COOLANT_MIXER.get()),
                page(ModItems.ACID_SYNTHESIZER.get()),
                page(ModItems.BAUXITE_ORE.get()),
                page(ModItems.ALUMINA.get()),
                page(ModItems.ALUMINUM_INGOT.get()),
                page(ModItems.ELECTROLYZER.get()),
                page(ModItems.PYROLYSIS_OVEN.get()),
                page(ModItems.CARBON_FIBER.get()),
                page(ModItems.FABRICATOR.get()),
                page(ModItems.SUPPLY_TERMINAL.get()),
                page(ModItems.BASIC_CIRCUIT.get()),
                page(ModItems.ADVANCED_CIRCUIT.get()),
                page(ModItems.ELITE_CIRCUIT.get()),
                page(ModItems.SULFUR.get()),
                page(ModItems.ACID_BUCKET.get()),
                page(ModItems.STEAM_ENGINE.get()),
                page(ModItems.STEAM_ENGINE_CYLINDER.get()),
                page(ModItems.STEAM_ENGINE_DYNAMO.get()),
                page(ModItems.NETHER_SULFUR_ORE.get()),
                page(ModItems.CONTAINMENT_GLASS.get()),
                page(ModItems.ACID_INLET.get()),
                page(ModItems.METHANE_OUTLET.get()),
                page(ModItems.INTAKE_GATE.get()),
                page(ModItems.FUEL_ROD.get()),
                page(ModItems.DEPLETED_FUEL_ROD.get()),
                page(ModItems.WASTE_REPROCESSOR.get()),
                page(ModItems.ISOTOPE_SHARD.get()),
                page(ModItems.CRYSTALLIZER.get()),
                page(ModItems.CRYSTALLIZER_PORT.get()),
                page(ModItems.FUSION_CRYSTAL.get()),
                page(ModItems.MECH_CORE.get()),
                page(ModItems.RESONANCE_CORE.get()),
                page(ModItems.STABILIZER_PYLON.get()),
                page(ModItems.RESONANCE_CELL.get()),
                page(ModItems.VITRIFIED_WASTE.get()),
                page(ModItems.HAZMAT_SUIT.get()),
                page(ModItems.GEIGER_COUNTER.get()),
                page(ModItems.BIOGAS_BUCKET.get()),
                page(ModItems.METHANE_BUCKET.get()),
                page(ModItems.STEAM_BUCKET.get()),
                page(ModItems.COOLANT_BUCKET.get()),
                page(ModItems.CRYO_COOLANT_BUCKET.get()),
                page(ModItems.DRONE.get()),
                page(ModItems.MECH_FRAME.get()),
                page(ModItems.MECH_HANGAR.get()),
                page(ModItems.HANGAR_FRAME.get()),
                page(ModItems.HANGAR_PAD.get()),
                page(ModItems.MECH_JET_MODULE.get()),
                page(ModItems.MECH_DRILL_MODULE.get()),
                page(ModItems.MECH_BLADE_MODULE.get()),
                page(ModItems.MECH_ARMOR_MODULE.get()),
                page(ModItems.SURVEY_TOOL.get()),
                page(ModItems.MULTIBLOCK_PROJECTOR.get()),
                page(ModItems.HARVESTER_MODULE.get()),
                page(ModItems.QUARRY_MODULE.get()),
                page(ModItems.TRANSPORT_MODULE.get()),
                page(ModItems.PLANTER_MODULE.get()),
                page(ModItems.LOGGER_MODULE.get()),
                page(ModItems.TAXI_MODULE.get()),
                page(ModItems.RANCHER_MODULE.get()),
                page(ModItems.FERTILIZER_MODULE.get()),
                page(ModItems.BUILDER_MODULE.get()),
                page(ModItems.FISHER_MODULE.get()),
                page(ModItems.BREEDER_MODULE.get()),
                page(ModItems.GUARD_MODULE.get()),
                page(ModItems.BOMB_MODULE.get()));
    }

    private static Page page(Item item) {
        return new Page(item, Component.translatable("info." + item.getDescriptionId()));
    }
}
