package com.cyberfarm.client.renderer;

import com.cyberfarm.drone.DroneTask;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class DroneRenderState extends LivingEntityRenderState {
    /** Installed modules, as a bit mask of {@link DroneTask#bit()}. */
    public int taskMask;
    /** The cargo item hanging from the Transport claw; empty when there is none. */
    public final ItemStackRenderState carried = new ItemStackRenderState();

    public boolean has(DroneTask task) {
        return (taskMask & task.bit()) != 0;
    }
}
