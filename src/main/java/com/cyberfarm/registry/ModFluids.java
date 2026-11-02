package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.fluid.CoolantFluidType;
import com.cyberfarm.fluid.GasFluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fluids.FluidType;
import com.cyberfarm.port.DeferredHolder;
import com.cyberfarm.port.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import com.cyberfarm.port.FluidResource;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, CyberFarm.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, CyberFarm.MODID);

    // Negative density: lighter than air, like every gas
    public static final DeferredHolder<FluidType, FluidType> BIOGAS_TYPE = FLUID_TYPES.register("biogas",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.cyberfarm.biogas").density(-400).temperature(310).viscosity(200)));
    public static final DeferredHolder<FluidType, FluidType> METHANE_TYPE = FLUID_TYPES.register("methane",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.cyberfarm.methane").density(-700).temperature(290).viscosity(100)));

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = FLUID_TYPES.register("steam",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.cyberfarm.steam").density(-1000).temperature(400).viscosity(50)));

    // Reactor coolants: real liquids that can be poured into the world, and freeze whoever wades in
    public static final DeferredHolder<FluidType, FluidType> COOLANT_TYPE = FLUID_TYPES.register("coolant",
            () -> new CoolantFluidType(FluidType.Properties.create().descriptionId("fluid.cyberfarm.coolant").density(1100).temperature(280).viscosity(1200), 0.7F));
    public static final DeferredHolder<FluidType, FluidType> CRYO_COOLANT_TYPE = FLUID_TYPES.register("cryo_coolant",
            () -> new CoolantFluidType(FluidType.Properties.create().descriptionId("fluid.cyberfarm.cryo_coolant").density(1200).temperature(80).viscosity(900), 0.5F));

    public static final DeferredHolder<Fluid, GasFluid> BIOGAS = FLUIDS.register("biogas", () -> new GasFluid(BIOGAS_TYPE, ModItems.BIOGAS_BUCKET));
    public static final DeferredHolder<Fluid, ForgeFlowingFluid.Source> COOLANT = FLUIDS.register("coolant",
            () -> new ForgeFlowingFluid.Source(coolant()));
    public static final DeferredHolder<Fluid, ForgeFlowingFluid.Flowing> FLOWING_COOLANT = FLUIDS.register("flowing_coolant",
            () -> new ForgeFlowingFluid.Flowing(coolant()));
    public static final DeferredHolder<Fluid, ForgeFlowingFluid.Source> CRYO_COOLANT = FLUIDS.register("cryo_coolant",
            () -> new ForgeFlowingFluid.Source(cryoCoolant()));
    public static final DeferredHolder<Fluid, ForgeFlowingFluid.Flowing> FLOWING_CRYO_COOLANT = FLUIDS.register("flowing_cryo_coolant",
            () -> new ForgeFlowingFluid.Flowing(cryoCoolant()));
    public static final DeferredHolder<Fluid, GasFluid> METHANE = FLUIDS.register("methane", () -> new GasFluid(METHANE_TYPE, ModItems.METHANE_BUCKET));
    public static final DeferredHolder<Fluid, GasFluid> STEAM = FLUIDS.register("steam", () -> new GasFluid(STEAM_TYPE, ModItems.STEAM_BUCKET));

    // Acid for the Methane Extractor's chamber: a liquid (it runs in Liquid Pipes) kept in tanks and buckets only
    public static final DeferredHolder<FluidType, FluidType> ACID_TYPE = FLUID_TYPES.register("acid",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.cyberfarm.acid").density(1300).temperature(300).viscosity(1500)));
    public static final DeferredHolder<Fluid, GasFluid> ACID = FLUIDS.register("acid", () -> new GasFluid(ACID_TYPE, ModItems.ACID_BUCKET));

    // They flow like water
    private static ForgeFlowingFluid.Properties coolant() {
        return new ForgeFlowingFluid.Properties(COOLANT_TYPE, COOLANT, FLOWING_COOLANT)
                .bucket(ModItems.COOLANT_BUCKET).block(ModBlocks.COOLANT).tickRate(5).slopeFindDistance(4).levelDecreasePerBlock(1);
    }

    private static ForgeFlowingFluid.Properties cryoCoolant() {
        return new ForgeFlowingFluid.Properties(CRYO_COOLANT_TYPE, CRYO_COOLANT, FLOWING_CRYO_COOLANT)
                .bucket(ModItems.CRYO_COOLANT_BUCKET).block(ModBlocks.CRYO_COOLANT).tickRate(5).slopeFindDistance(4).levelDecreasePerBlock(1);
    }

    /** Common steam tag, so steam from other mods drives our turbine and ours drives theirs. */
    public static final TagKey<Fluid> STEAM_TAG = TagKey.create(Registries.FLUID, new ResourceLocation("c", "steam"));

    public static boolean isSteam(FluidResource resource) {
        return !resource.isEmpty() && resource.getFluid().builtInRegistryHolder().is(STEAM_TAG);
    }
}
