package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.radiation.RadiationSicknessEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, CyberFarm.MODID);

    public static final DeferredHolder<MobEffect, RadiationSicknessEffect> RADIATION_SICKNESS = EFFECTS.register("radiation_sickness",
            RadiationSicknessEffect::new);
}
