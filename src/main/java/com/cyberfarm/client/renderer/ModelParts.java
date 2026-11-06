package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * What the machines' renderers share to draw parts that move inside a block model: the model's frame, plain
 * tinted boxes and the spin-up and wind-down.
 */
final class ModelParts {
    /** Solid parts: this texture's white texel, tinted by the vertex colours. */
    static final RenderType SOLID = RenderTypes.entityCutout(CyberFarm.id("textures/entity/fabricator_card.png"));
    static final float WHITE_UV = 15.5F / 16.0F;
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float CARRY = 25.0F;

    private ModelParts() {
    }

    /** Coordinates become model pixels of the north-facing model. Pair it with a pushPose and popPose. */
    static void enterModel(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        poseStack.translate(-8.0F, 0.0F, -8.0F);
    }

    /** Moves the machine's speed towards {@code target} (1 = full), {@code ramp} ticks for the whole range; returns how far it has run, in ticks at full speed. */
    static float advance(MachineBlockEntity entity, float time, float target, float ramp) {
        float elapsed = entity.motionTime == 0.0F ? 0.0F : Mth.clamp(time - entity.motionTime, 0.0F, 5.0F);
        entity.motionTime = time;
        entity.motionSpeed += Mth.clamp(target - entity.motionSpeed, -elapsed / ramp, elapsed / ramp);
        entity.motionPhase += entity.motionSpeed * elapsed;
        return entity.motionPhase;
    }

    /** The recipe's progress now, 0 to 1. It comes every 20 ticks and is carried on for no longer, so a stalled machine stops its parts. */
    static float progress(MachineBlockEntity entity, float time, int recipeTicks) {
        float carried = entity.status.working() ? Math.min(time - entity.statusTime, CARRY) / recipeTicks : 0.0F;
        return Mth.clamp(entity.status.progress() + carried, 0.0F, 1.0F);
    }

    /** A plain box, its top one colour and the rest another. Rotate the pose before it to tilt the box: the normals follow. */
    static void box(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, float x1, float y1, float z1, int top, int colour, int light) {
        quad(pose, buffer, top, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(pose, buffer, colour, light, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        quad(pose, buffer, colour, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(pose, buffer, colour, light, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        quad(pose, buffer, colour, light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        quad(pose, buffer, colour, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int colour, int light, float nx, float ny, float nz, float... c) {
        for (int i = 0; i < 4; i++) {
            vertex(pose, buffer, c[i * 3], c[i * 3 + 1], c[i * 3 + 2], colour, light, nx, ny, nz);
        }
    }

    /** One corner of a {@link #SOLID} face. */
    static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int colour, int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(colour).setUv(WHITE_UV, WHITE_UV).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, ny, nz);
    }
}
