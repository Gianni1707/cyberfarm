package com.cyberfarm.client.renderer;

import com.cyberfarm.drone.DroneTask;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class DroneRenderState extends LivingEntityRenderState {
    /** Installed modules, as a bit mask of {@link DroneTask#bit()}. */
    public int taskMask;

    public boolean has(DroneTask task) {
        return (taskMask & task.bit()) != 0;
    }
}
