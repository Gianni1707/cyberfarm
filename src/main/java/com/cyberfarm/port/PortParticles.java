package com.cyberfarm.port;

import org.joml.Vector3f;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

/** 26.x builds dust from an RGB int; 1.20.1 wants a color vector. */
public final class PortParticles {
    /** 1.20.1 has no white smoke (it came in 1.20.3): small cloud puffs are the nearest. */
    public static final SimpleParticleType WHITE_SMOKE = ParticleTypes.CLOUD;

    private PortParticles() {}

    public static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F), scale);
    }
}
