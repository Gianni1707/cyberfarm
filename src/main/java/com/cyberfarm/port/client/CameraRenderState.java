package com.cyberfarm.port.client;

import org.joml.Quaternionf;

import net.minecraft.client.Camera;

/** The camera as 26.x hands it to renderers: where it is and which way it looks. */
public final class CameraRenderState {
    public double x;
    public double y;
    public double z;
    public final Quaternionf orientation = new Quaternionf();

    public CameraRenderState set(Camera camera) {
        x = camera.getPosition().x;
        y = camera.getPosition().y;
        z = camera.getPosition().z;
        orientation.set(camera.rotation());
        return this;
    }
}
