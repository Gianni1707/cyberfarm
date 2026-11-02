package com.cyberfarm.registry;

import com.cyberfarm.drone.DroneModules;
import com.cyberfarm.port.DataComponentType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Item data, kept in the stack NBT on 1.20.1 (26.x data components). */
public class ModDataComponents {
    /** FE stored in an item, e.g. the drone's battery. */
    public static final DataComponentType<Integer> ENERGY = DataComponentType.ofInt("energy");
    /** A folded mech's modules, as a bit mask of MechModule#bit(). */
    public static final DataComponentType<Integer> MECH_MODULES = DataComponentType.ofInt("mech_modules");

    public static final DataComponentType<DroneModules> DRONE_MODULES = DataComponentType.of("drone_modules", DroneModules.CODEC);

    /** Mob Capsule: the saved data of the captured mob. */
    public static final DataComponentType<CompoundTag> CAPTURED_MOB = DataComponentType.of("captured_mob", CompoundTag.CODEC);

    /** Survey Tool: the dock it is configuring. */
    public static final DataComponentType<BlockPos> LINKED_DOCK = DataComponentType.of("linked_dock", BlockPos.CODEC);

    /** Survey Tool: the source container of the transport route being set. */
    public static final DataComponentType<BlockPos> ROUTE_SOURCE = DataComponentType.of("route_source", BlockPos.CODEC);

    /** Survey Tool: the first corner of the area being marked. */
    public static final DataComponentType<BlockPos> SURVEY_CORNER = DataComponentType.of("survey_corner", BlockPos.CODEC);

    /** Survey Tool: the mode it works in, by {@code SurveyToolItem.Mode} ordinal. */
    public static final DataComponentType<Integer> SURVEY_MODE = DataComponentType.ofInt("survey_mode");

    /** Blueprint Disc: the village building (a schematic) it teaches. */
    public static final DataComponentType<ResourceLocation> BLUEPRINT = DataComponentType.of("blueprint", ResourceLocation.CODEC);
    /** Multiblock Projector: the chosen structure (a multiblock or a schematic), and where and which way it is pinned. */
    public static final DataComponentType<ResourceLocation> PROJECTOR_STRUCTURE = DataComponentType.of("projector_structure", ResourceLocation.CODEC);
    /** The multiblock's number, as projectors saved it before the schematics; still read so they keep their choice. */
    public static final DataComponentType<Integer> PROJECTOR_BLUEPRINT = DataComponentType.ofInt("projector_blueprint");
    public static final DataComponentType<BlockPos> PROJECTOR_ANCHOR = DataComponentType.of("projector_anchor", BlockPos.CODEC);
    public static final DataComponentType<Integer> PROJECTOR_FACING = DataComponentType.ofInt("projector_facing");
}
