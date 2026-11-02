package com.cyberfarm.port.client;

/** The 26.x living render state: what the models read, filled from the 1.20.1 setupAnim call. */
public class LivingEntityRenderState {
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    /** Head yaw relative to the body, and pitch, in degrees. */
    public float yRot;
    public float xRot;
    public float scale = 1.0F;
    public int lightCoords;
    public int outlineColor;
}
