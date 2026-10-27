package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.drone.DroneModules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CyberFarm.MODID);

    /** FE stored in an item, e.g. the drone's battery. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.registerComponentType("energy",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** A folded mech's modules, as a bit mask of MechModule#bit(). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MECH_MODULES = COMPONENTS.registerComponentType("mech_modules",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DroneModules>> DRONE_MODULES = COMPONENTS.registerComponentType("drone_modules",
            builder -> builder.persistent(DroneModules.CODEC).networkSynchronized(DroneModules.STREAM_CODEC));

    /** Mob Capsule: the saved data of the captured mob. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> CAPTURED_MOB = COMPONENTS.registerComponentType("captured_mob",
            builder -> builder.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));

    /** Survey Tool: the dock it is configuring. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> LINKED_DOCK = COMPONENTS.registerComponentType("linked_dock",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    /** Survey Tool: the source container of the transport route being set. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> ROUTE_SOURCE = COMPONENTS.registerComponentType("route_source",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    /** Survey Tool: the first corner of the area being marked. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> SURVEY_CORNER = COMPONENTS.registerComponentType("survey_corner",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    /** Survey Tool: the mode it works in, by {@code SurveyToolItem.Mode} ordinal. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SURVEY_MODE = COMPONENTS.registerComponentType("survey_mode",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Blueprint Disc: the village building (a schematic) it teaches. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> BLUEPRINT = COMPONENTS.registerComponentType("blueprint",
            builder -> builder.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC));
    /** Multiblock Projector: the chosen structure (a multiblock or a schematic), and where and which way it is pinned. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> PROJECTOR_STRUCTURE = COMPONENTS.registerComponentType("projector_structure",
            builder -> builder.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC));
    /** The multiblock's number, as projectors saved it before the schematics; still read so they keep their choice. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PROJECTOR_BLUEPRINT = COMPONENTS.registerComponentType("projector_blueprint",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> PROJECTOR_ANCHOR = COMPONENTS.registerComponentType("projector_anchor",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PROJECTOR_FACING = COMPONENTS.registerComponentType("projector_facing",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
}
