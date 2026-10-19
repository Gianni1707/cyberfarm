package com.cyberfarm.radiation;

import com.cyberfarm.CyberFarm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Radiation Sickness: hunger, then weakness and nausea, and damage that bypasses armor, more often
 * the higher the level (I every 3 s, II every 1.5 s, III every 0.75 s).
 */
public class RadiationSicknessEffect extends MobEffect {
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, CyberFarm.id("radiation"));

    public RadiationSicknessEffect() {
        super(MobEffectCategory.HARMFUL, 0x7FDC3A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(15, 60 >> amplifier);
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        entity.hurtServer(level, new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE)), 1.0F);
        if (entity instanceof Player player) {
            player.causeFoodExhaustion(1.0F + amplifier);
        }
        if (amplifier >= 1) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, amplifier - 1));
        }
        if (amplifier >= 2) {
            entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200));
        }
        return true;
    }
}
