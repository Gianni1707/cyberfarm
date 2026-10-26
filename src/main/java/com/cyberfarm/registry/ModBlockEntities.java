package com.cyberfarm.registry;

import java.util.Set;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.entity.AcidSynthesizerBlockEntity;
import com.cyberfarm.block.entity.ElectrolyzerBlockEntity;
import com.cyberfarm.block.entity.FabricatorBlockEntity;
import com.cyberfarm.block.entity.PyrolysisOvenBlockEntity;
import com.cyberfarm.block.entity.CrystallizerBlockEntity;
import com.cyberfarm.block.entity.SteamEngineBlockEntity;
import com.cyberfarm.block.entity.MechHangarBlockEntity;
import com.cyberfarm.block.entity.BioDigesterBlockEntity;
import com.cyberfarm.block.entity.CombustionGeneratorBlockEntity;
import com.cyberfarm.block.entity.CoolantMixerBlockEntity;
import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.block.entity.SwarmDockBlockEntity;
import com.cyberfarm.block.entity.HologramTableBlockEntity;
import com.cyberfarm.block.entity.EnergyConduitBlockEntity;
import com.cyberfarm.block.entity.FluidTankBlockEntity;
import com.cyberfarm.block.entity.GasPipeBlockEntity;
import com.cyberfarm.block.entity.GasTurbineBlockEntity;
import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;
import com.cyberfarm.block.entity.MufflerBlockEntity;
import com.cyberfarm.block.entity.ReactorControllerBlockEntity;
import com.cyberfarm.block.entity.ReactorPortBlockEntity;
import com.cyberfarm.block.entity.TurbineControllerBlockEntity;
import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;
import com.cyberfarm.block.entity.WasteReprocessorBlockEntity;
import com.cyberfarm.block.entity.WaterPumpBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CyberFarm.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR = BLOCK_ENTITIES.register("combustion_generator",
            () -> new BlockEntityType<>(CombustionGeneratorBlockEntity::new, Set.of(ModBlocks.COMBUSTION_GENERATOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneAssemblerBlockEntity>> DRONE_ASSEMBLER = BLOCK_ENTITIES.register("drone_assembler",
            () -> new BlockEntityType<>(DroneAssemblerBlockEntity::new, Set.of(ModBlocks.DRONE_ASSEMBLER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyConduitBlockEntity>> ENERGY_CONDUIT = BLOCK_ENTITIES.register("energy_conduit",
            () -> new BlockEntityType<>(EnergyConduitBlockEntity::new, Set.of(ModBlocks.ENERGY_CONDUIT.get(), ModBlocks.ADVANCED_ENERGY_CONDUIT.get(), ModBlocks.ELITE_ENERGY_CONDUIT.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasPipeBlockEntity>> GAS_PIPE = BLOCK_ENTITIES.register("gas_pipe",
            () -> new BlockEntityType<>(GasPipeBlockEntity::new, Set.of(ModBlocks.GAS_PIPE.get(), ModBlocks.ADVANCED_GAS_PIPE.get(), ModBlocks.ELITE_GAS_PIPE.get(),
                    ModBlocks.LIQUID_PIPE.get(), ModBlocks.ADVANCED_LIQUID_PIPE.get(), ModBlocks.ELITE_LIQUID_PIPE.get())));


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BioDigesterBlockEntity>> BIO_DIGESTER = BLOCK_ENTITIES.register("bio_digester",
            () -> new BlockEntityType<>(BioDigesterBlockEntity::new, Set.of(ModBlocks.BIO_DIGESTER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasTurbineBlockEntity>> GAS_TURBINE = BLOCK_ENTITIES.register("gas_turbine",
            () -> new BlockEntityType<>(GasTurbineBlockEntity::new, Set.of(ModBlocks.GAS_TURBINE.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MethaneExtractorBlockEntity>> METHANE_EXTRACTOR = BLOCK_ENTITIES.register("methane_extractor",
            () -> new BlockEntityType<>(MethaneExtractorBlockEntity::new, Set.of(ModBlocks.METHANE_EXTRACTOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MufflerBlockEntity>> MUFFLER = BLOCK_ENTITIES.register("muffler",
            () -> new BlockEntityType<>(MufflerBlockEntity::new, Set.of(ModBlocks.MUFFLER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineControllerBlockEntity>> TURBINE_CONTROLLER = BLOCK_ENTITIES.register("turbine_controller",
            () -> new BlockEntityType<>(TurbineControllerBlockEntity::new, Set.of(ModBlocks.TURBINE_CONTROLLER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorControllerBlockEntity>> REACTOR_CONTROLLER = BLOCK_ENTITIES.register("reactor_controller",
            () -> new BlockEntityType<>(ReactorControllerBlockEntity::new, Set.of(ModBlocks.REACTOR_CONTROLLER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorPortBlockEntity>> REACTOR_PORT = BLOCK_ENTITIES.register("reactor_port",
            () -> new BlockEntityType<>(ReactorPortBlockEntity::new, Set.of(ModBlocks.REACTOR_PORT.get(), ModBlocks.TURBINE_PORT.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechHangarBlockEntity>> MECH_HANGAR = BLOCK_ENTITIES.register("mech_hangar",
            () -> new BlockEntityType<>(MechHangarBlockEntity::new, Set.of(ModBlocks.MECH_HANGAR.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE = BLOCK_ENTITIES.register("steam_engine",
            () -> new BlockEntityType<>(SteamEngineBlockEntity::new, Set.of(ModBlocks.STEAM_ENGINE.get())));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AcidSynthesizerBlockEntity>> ACID_SYNTHESIZER = BLOCK_ENTITIES.register("acid_synthesizer",
            () -> new BlockEntityType<>(AcidSynthesizerBlockEntity::new, Set.of(ModBlocks.ACID_SYNTHESIZER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectrolyzerBlockEntity>> ELECTROLYZER = BLOCK_ENTITIES.register("electrolyzer",
            () -> new BlockEntityType<>(ElectrolyzerBlockEntity::new, Set.of(ModBlocks.ELECTROLYZER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PyrolysisOvenBlockEntity>> PYROLYSIS_OVEN = BLOCK_ENTITIES.register("pyrolysis_oven",
            () -> new BlockEntityType<>(PyrolysisOvenBlockEntity::new, Set.of(ModBlocks.PYROLYSIS_OVEN.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FabricatorBlockEntity>> FABRICATOR = BLOCK_ENTITIES.register("fabricator",
            () -> new BlockEntityType<>(FabricatorBlockEntity::new, Set.of(ModBlocks.FABRICATOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoolantMixerBlockEntity>> COOLANT_MIXER = BLOCK_ENTITIES.register("coolant_mixer",
            () -> new BlockEntityType<>(CoolantMixerBlockEntity::new, Set.of(ModBlocks.COOLANT_MIXER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystallizerBlockEntity>> CRYSTALLIZER = BLOCK_ENTITIES.register("crystallizer",
            () -> new BlockEntityType<>(CrystallizerBlockEntity::new, Set.of(ModBlocks.CRYSTALLIZER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WasteReprocessorBlockEntity>> WASTE_REPROCESSOR = BLOCK_ENTITIES.register("waste_reprocessor",
            () -> new BlockEntityType<>(WasteReprocessorBlockEntity::new, Set.of(ModBlocks.WASTE_REPROCESSOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK = BLOCK_ENTITIES.register("fluid_tank",
            () -> new BlockEntityType<>(FluidTankBlockEntity::new, Set.of(ModBlocks.LIQUID_TANK.get(), ModBlocks.GAS_TANK.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResonanceCoreBlockEntity>> RESONANCE_CORE = BLOCK_ENTITIES.register("resonance_core",
            () -> new BlockEntityType<>(ResonanceCoreBlockEntity::new, Set.of(ModBlocks.RESONANCE_CORE.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterPumpBlockEntity>> WATER_PUMP = BLOCK_ENTITIES.register("water_pump",
            () -> new BlockEntityType<>(WaterPumpBlockEntity::new, Set.of(ModBlocks.WATER_PUMP.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneDockBlockEntity>> DRONE_DOCK = BLOCK_ENTITIES.register("drone_dock",
            () -> new BlockEntityType<>(DroneDockBlockEntity::new, Set.of(ModBlocks.DRONE_DOCK.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SwarmDockBlockEntity>> SWARM_DOCK = BLOCK_ENTITIES.register("swarm_dock",
            () -> new BlockEntityType<>(SwarmDockBlockEntity::new, Set.of(ModBlocks.SWARM_DOCK.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HologramTableBlockEntity>> HOLOGRAM_TABLE = BLOCK_ENTITIES.register("hologram_table",
            () -> new BlockEntityType<>(HologramTableBlockEntity::new, Set.of(ModBlocks.HOLOGRAM_TABLE.get())));
}
