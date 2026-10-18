package com.cyberfarm.registry;

import java.util.Set;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.entity.CombustionGeneratorBlockEntity;
import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.block.entity.DroneDockBlockEntity;

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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneDockBlockEntity>> DRONE_DOCK = BLOCK_ENTITIES.register("drone_dock",
            () -> new BlockEntityType<>(DroneDockBlockEntity::new, Set.of(ModBlocks.DRONE_DOCK.get())));
}
