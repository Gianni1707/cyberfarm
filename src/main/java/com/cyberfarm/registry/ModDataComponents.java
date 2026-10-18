package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.drone.DroneModules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CyberFarm.MODID);

    /** FE stored in an item, e.g. the drone's battery. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.registerComponentType("energy",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DroneModules>> DRONE_MODULES = COMPONENTS.registerComponentType("drone_modules",
            builder -> builder.persistent(DroneModules.CODEC).networkSynchronized(DroneModules.STREAM_CODEC));

    /** Survey Tool: the dock it is configuring. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> LINKED_DOCK = COMPONENTS.registerComponentType("linked_dock",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    /** Survey Tool: the first corner of the area being marked. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> SURVEY_CORNER = COMPONENTS.registerComponentType("survey_corner",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));
}
