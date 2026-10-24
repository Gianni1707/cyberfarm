package com.cyberfarm.client;

import com.cyberfarm.entity.MechEntity;
import com.cyberfarm.entity.MechModule;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * A mech's running sounds, following it: the jet's roar while it is in the air with its wings out
 * (higher as it climbs), or the drill's spin while the drill is out. Silent the rest of the time.
 */
public class MechLoopSound extends AbstractTickableSoundInstance {
    private final MechEntity mech;
    private final MechModule module;

    public MechLoopSound(MechEntity mech, MechModule module) {
        super(module == MechModule.JET ? ModSounds.MECH_JET.get() : ModSounds.MECH_DRILL.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.mech = mech;
        this.module = module;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.x = mech.getX();
        this.y = mech.getY() + 2;
        this.z = mech.getZ();
    }

    @Override
    public void tick() {
        if (mech.isRemoved()) {
            stop();
            return;
        }
        x = mech.getX();
        y = mech.getY() + 2;
        z = mech.getZ();
        float out = mech.deployAmount(module, 1.0F);
        float targetVolume;
        float targetPitch;
        if (module == MechModule.JET) {
            // Climbing it roars higher; gliding it only breathes
            float climb = (float) (mech.getY() - mech.yo);
            targetVolume = mech.onGround() ? 0.0F : out * Mth.clamp(0.5F + climb * 3.0F, 0.3F, 1.0F);
            targetPitch = 0.8F + Mth.clamp(climb * 2.0F, -0.2F, 0.5F);
        } else {
            targetVolume = out * 0.8F;
            targetPitch = 0.6F + 0.5F * out;
        }
        volume += (targetVolume - volume) * 0.2F;
        pitch += (targetPitch - pitch) * 0.2F;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
