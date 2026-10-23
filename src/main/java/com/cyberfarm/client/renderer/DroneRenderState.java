package com.cyberfarm.client.renderer;

import com.cyberfarm.drone.DroneTask;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class DroneRenderState extends LivingEntityRenderState {
    /** Installed modules, as a bit mask of {@link DroneTask#bit()}. */
    public int taskMask;
    /** The cargo item hanging from the Transport claw; empty when there is none. */
    public final ItemStackRenderState carried = new ItemStackRenderState();
    /** Ticks into the deploy sequence (see {@link com.cyberfarm.entity.DroneEntity#startupTicks}), -1 when it is over. */
    public float startup = -1.0F;
    /** Ticks since a picked-up drone touched down, -1 when it is not being picked up. */
    public float shutdown = -1.0F;

    public boolean has(DroneTask task) {
        return (taskMask & task.bit()) != 0;
    }
}
