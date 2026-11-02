package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.radiation.RadiationSicknessEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import com.cyberfarm.port.DeferredHolder;
import com.cyberfarm.port.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, CyberFarm.MODID);

    public static final DeferredHolder<MobEffect, RadiationSicknessEffect> RADIATION_SICKNESS = EFFECTS.register("radiation_sickness",
            RadiationSicknessEffect::new);
}
