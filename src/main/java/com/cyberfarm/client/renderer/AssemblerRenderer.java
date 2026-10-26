package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.block.entity.MachineStatus;
import com.cyberfarm.client.model.DroneModel;
import com.cyberfarm.entity.DroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * While the Drone Assembler works, the drone it is making takes shape as a hologram on the pad in
 * its chamber: faint and folded at first, solid and unfolded as the assembly ends, its rotors
 * starting to turn in the last moments, with a scan ring running up and down it between the
 * pillars. When it is done the assembler flashes it into the output slot (see {@code Effects.print}).
 */
public class AssemblerRenderer implements BlockEntityRenderer<DroneAssemblerBlockEntity, AssemblerRenderer.State> {
    private static final RenderType HOLOGRAM = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/drone.png"));
    private static final RenderType LIGHT = RenderTypes.entityTranslucent(CyberFarm.id("textures/entity/hologram.png"));
    private static final int CYAN = 0x2FE0FF;
    /** The pad's top, where the drone stands, its size in the chamber and how far the scan ring runs. */
    private static final float PAD = 4.0F / 16.0F;
    private static final float SCALE = 0.5F;
    private static final float HEIGHT = 0.55F;
    /** Inside the pillars. */
    private static final float INNER = 2.2F / 16.0F;

    public static class State extends BlockEntityRenderState {
        /** 0 to 1 through the assembly; negative when the assembler is idle. */
        float progress = -1.0F;
        float time;
        final DroneRenderState drone = new DroneRenderState();
    }

    private final DroneModel model;

    public AssemblerRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new DroneModel(context.bakeLayer(DroneModel.LAYER));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DroneAssemblerBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        state.progress = -1.0F;
        if (entity.getLevel() == null) {
            return;
        }
        MachineStatus status = entity.status;
        float time = entity.getLevel().getGameTime() + partialTick;
        state.time = time;
        if (!status.working() || status.progress() < 0) {
            return;
        }
        // The status comes once a second: carried on at the assembly's pace in between
        float since = Math.max(0.0F, time - entity.statusTime);
        state.progress = Mth.clamp(status.progress() + since / DroneAssemblerBlockEntity.ASSEMBLY_TIME, 0.0F, 1.0F);
        DroneRenderState drone = state.drone;
        drone.ageInTicks = time;
        drone.taskMask = 0;
        drone.shutdown = -1.0F;
        // Unfolds as it is made; the deploy sequence spins the rotors up over its last quarter
        drone.startup = state.progress * DroneEntity.UNFOLD_TICKS;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.progress < 0) {
            return;
        }
        float p = state.progress;
        float flicker = 0.9F + 0.1F * Mth.sin(state.time * 0.7F) * Mth.sin(state.time * 0.31F);
        float alpha = (0.2F + 0.6F * p) * flicker;
        poseStack.pushPose();
        poseStack.translate(0.5F, PAD + 0.01F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 2.0F));
        // Entity models are built upside down, a block and a half tall
        poseStack.scale(-SCALE, -SCALE, SCALE);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        collector.submitModel(model, state.drone, poseStack, HOLOGRAM, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                ARGB.colorFromFloat(alpha, 0.55F + 0.45F * p, 0.95F, 1.0F), null);
        poseStack.popPose();
        // The scan ring, up and down the drone once a second
        float sweep = Mth.sin(state.time * Mth.PI / 20.0F) * 0.5F + 0.5F;
        float y = PAD + 0.02F + sweep * HEIGHT;
        int ring = (int) (0xC0 * flicker) << 24 | CYAN;
        collector.submitCustomGeometry(poseStack, LIGHT, (pose, buffer) -> ring(pose, buffer, INNER, 1.0F - INNER, y, 0.03F, ring));
    }

    private static void ring(PoseStack.Pose pose, VertexConsumer buffer, float a, float b, float y, float thick, int colour) {
        quad(pose, buffer, a, y, a, b, y, a, b, y, a + thick, a, y, a + thick, colour);
        quad(pose, buffer, a, y, b - thick, b, y, b - thick, b, y, b, a, y, b, colour);
        quad(pose, buffer, a, y, a + thick, a + thick, y, a + thick, a + thick, y, b - thick, a, y, b - thick, colour);
        quad(pose, buffer, b - thick, y, a + thick, b, y, a + thick, b, y, b - thick, b - thick, y, b - thick, colour);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz, int colour) {
        vertex(pose, buffer, ax, ay, az, colour);
        vertex(pose, buffer, bx, by, bz, colour);
        vertex(pose, buffer, cx, cy, cz, colour);
        vertex(pose, buffer, dx, dy, dz, colour);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int colour) {
        buffer.addVertex(pose, x, y, z).setColor(colour).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(DroneAssemblerBlockEntity entity) {
        return new AABB(entity.getBlockPos());
    }
}
