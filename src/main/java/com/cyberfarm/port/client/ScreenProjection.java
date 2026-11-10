package com.cyberfarm.port.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.world.phys.Vec3;

/**
 * The 26.x GameRenderer.projectPointToScreen: a point of the world on the screen, in normalized device coordinates
 * (x and y from -1 to 1). 1.20.1 has no such call, so the last frame's camera and matrices are kept as the level
 * renders (see {@link LevelRenderEvents}).
 */
public final class ScreenProjection {
    private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
    private static Vec3 camera = Vec3.ZERO;

    private ScreenProjection() {}

    static void capture(Matrix4f projection, Matrix4f view, Vec3 cameraPos) {
        VIEW_PROJECTION.set(projection).mul(view);
        camera = cameraPos;
    }

    public static Vec3 project(Vec3 point) {
        Vector3f at = new Vector3f((float) (point.x - camera.x), (float) (point.y - camera.y), (float) (point.z - camera.z));
        VIEW_PROJECTION.transformProject(at);
        return new Vec3(at.x(), at.y(), at.z());
    }
}
