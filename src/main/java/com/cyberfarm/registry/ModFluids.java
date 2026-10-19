package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.fluid.GasFluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CyberFarm.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, CyberFarm.MODID);

    // Negative density: lighter than air, like every gas
    public static final DeferredHolder<FluidType, FluidType> BIOGAS_TYPE = FLUID_TYPES.register("biogas",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.cyberfarm.biogas").density(-400).temperature(310).viscosity(200)));
    public static final DeferredHolder<FluidType, FluidType> METHANE_TYPE = FLUID_TYPES.register("methane",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.cyberfarm.methane").density(-700).temperature(290).viscosity(100)));

    public static final DeferredHolder<Fluid, GasFluid> BIOGAS = FLUIDS.register("biogas", () -> new GasFluid(BIOGAS_TYPE));
    public static final DeferredHolder<Fluid, GasFluid> METHANE = FLUIDS.register("methane", () -> new GasFluid(METHANE_TYPE));
}
