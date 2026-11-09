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
    /** From 0 in flight to 1 parked: how far what hangs under the skids has tucked up. */
    public float stow;
    /** From 0 tucked in to 1 lowered: the Sponge Module's hood. */
    public float hood;
    /** The Sponge Module's rings and lamp: the liquid's colour, and how full it is from 0 to 1. */
    public int liquidColour;
    public float liquidFill;
    /** Whether the liquid gives off light, as lava does. */
    public boolean liquidGlows;
    /** From 0 still to 1 pumping: the Tank Module's handle. */
    public float tankWork;

    public boolean has(DroneTask task) {
        return (taskMask & task.bit()) != 0;
    }
}
