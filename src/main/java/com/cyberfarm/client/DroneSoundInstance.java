package com.cyberfarm.client;

import com.cyberfarm.block.entity.MufflerBlockEntity;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** The rotor hum that follows a drone: quiet on the pad, louder and higher-pitched as it speeds up. */
public class DroneSoundInstance extends AbstractTickableSoundInstance {
    private final DroneEntity drone;

    public DroneSoundInstance(DroneEntity drone) {
        super(ModSounds.DRONE_FLY.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.drone = drone;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.x = drone.getX();
        this.y = drone.getY();
        this.z = drone.getZ();
    }

    @Override
    public void tick() {
        if (drone.isRemoved()) {
            stop();
            return;
        }
        x = drone.getX();
        y = drone.getY();
        z = drone.getZ();
        // Speed from the last tick's movement: the client does not always know the drone's velocity
        float speed = (float) Math.sqrt(drone.distanceToSqr(drone.xo, drone.yo, drone.zo));
        boolean muffled = MufflerBlockEntity.isMuffled(drone.level(), x, y, z, MufflerBlockEntity.Category.DRONES);
        volume = muffled ? 0.0F : Mth.clamp(0.15F + speed * 4.0F, 0.0F, 1.0F);
        pitch = 0.8F + Mth.clamp(speed * 3.0F, 0.0F, 0.6F);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !drone.isSilent();
    }
}
