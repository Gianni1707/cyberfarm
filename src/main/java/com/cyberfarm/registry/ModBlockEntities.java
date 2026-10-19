package com.cyberfarm.registry;

import java.util.Set;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.entity.BioDigesterBlockEntity;
import com.cyberfarm.block.entity.CombustionGeneratorBlockEntity;
import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.block.entity.EnergyConduitBlockEntity;
import com.cyberfarm.block.entity.GasPipeBlockEntity;
import com.cyberfarm.block.entity.GasTurbineBlockEntity;
import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;

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
            () -> new BlockEntityType<>(EnergyConduitBlockEntity::new, Set.of(ModBlocks.ENERGY_CONDUIT.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasPipeBlockEntity>> GAS_PIPE = BLOCK_ENTITIES.register("gas_pipe",
            () -> new BlockEntityType<>(GasPipeBlockEntity::new, Set.of(ModBlocks.GAS_PIPE.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BioDigesterBlockEntity>> BIO_DIGESTER = BLOCK_ENTITIES.register("bio_digester",
            () -> new BlockEntityType<>(BioDigesterBlockEntity::new, Set.of(ModBlocks.BIO_DIGESTER.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasTurbineBlockEntity>> GAS_TURBINE = BLOCK_ENTITIES.register("gas_turbine",
            () -> new BlockEntityType<>(GasTurbineBlockEntity::new, Set.of(ModBlocks.GAS_TURBINE.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MethaneExtractorBlockEntity>> METHANE_EXTRACTOR = BLOCK_ENTITIES.register("methane_extractor",
            () -> new BlockEntityType<>(MethaneExtractorBlockEntity::new, Set.of(ModBlocks.METHANE_EXTRACTOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneDockBlockEntity>> DRONE_DOCK = BLOCK_ENTITIES.register("drone_dock",
            () -> new BlockEntityType<>(DroneDockBlockEntity::new, Set.of(ModBlocks.DRONE_DOCK.get())));
}
