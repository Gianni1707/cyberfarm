package com.cyberfarm.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class MechRenderState extends LivingEntityRenderState {
    /** How open the chest is: 0 shut, 1 wide open. */
    public float open;
    /** Shut down: it slumps forward. */
    public boolean disabled;
    /** How far each module's parts are out, 0 stowed to 1 fully out: the jet's wings, the drill, the blade, the armour. */
    public float jet;
    public float drill;
    public float blade;
    public float armor;
    /** The drill's turn, in radians. */
    public float drillSpin;
    /** How far folded up into its frame, 0 not at all, 1 packed. */
    public float fold;
    /** How far down it crouches, 0 standing, 1 with the bay a step above the ground. */
    public float crouch;
    /** Seen from its own cockpit: the parts around the camera are not drawn. */
    public boolean firstPerson;
    /** The pilot's swing, 0 to 1, thrown as a punch by the right arm. */
    public float punch;
}
