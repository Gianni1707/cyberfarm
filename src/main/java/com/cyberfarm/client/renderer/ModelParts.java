package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.fluid.GasTank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.cyberfarm.port.client.VertexConsumer;
import com.mojang.math.Axis;
import com.cyberfarm.registry.ModBlocks;

import net.minecraft.client.renderer.RenderType;
import com.cyberfarm.port.client.RenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.cyberfarm.port.client.SubmitNodeCollector;
import com.cyberfarm.port.client.MovingBlockRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import com.cyberfarm.port.client.FluidSprites;
import net.minecraftforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * What the machines' renderers share to draw parts that move inside a block model: the model's frame, plain
 * tinted boxes and the spin-up and wind-down.
 */
final class ModelParts {
    /** Solid parts: this texture's white texel, tinted by the vertex colours. */
    static final RenderType SOLID = RenderTypes.entityCutout(CyberFarm.id("textures/entity/fabricator_card.png"));
    static final float WHITE_UV = 15.5F / 16.0F;
    /** Fluids in sight glasses. */
    static final RenderType FLUID = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
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

    /** A part's model at the machine's place, for its light: what a piston draws as it pushes a block. */
    static MovingBlockRenderState movingPart(Level level, BlockPos pos, int part, Direction facing) {
        MovingBlockRenderState state = new MovingBlockRenderState();
        state.randomSeedPos = pos;
        state.blockPos = pos;
        state.blockState = ModBlocks.MACHINE_PART.get().defaultBlockState().setValue(MachinePartBlock.FACING, facing).setValue(MachinePartBlock.PART, part);
        state.biome = level.getBiome(pos);
        if (level instanceof ClientLevel client) {
            // 1.20.1 lights the part from the client level
        }
        state.lightEngine = level.getLightEngine();
        return state;
    }

    /** Draws a part shifted by this many model pixels of the north-facing model. */
    static void slid(PoseStack poseStack, SubmitNodeCollector collector, MovingBlockRenderState part, Direction facing, float dx, float dy, float dz) {
        float a = (180.0F - facing.toYRot()) * Mth.DEG_TO_RAD;
        poseStack.pushPose();
        poseStack.translate((dx * Mth.cos(a) + dz * Mth.sin(a)) * PIXEL, dy * PIXEL, (dz * Mth.cos(a) - dx * Mth.sin(a)) * PIXEL);
        collector.submitMovingBlock(poseStack, part, 0);
        poseStack.popPose();
    }

    /** Draws a part turned round a vertical hinge, at this place on the north-facing model. */
    static void swung(PoseStack poseStack, SubmitNodeCollector collector, MovingBlockRenderState part, Direction facing, float hingeX, float hingeZ, float angle) {
        float a = (180.0F - facing.toYRot()) * Mth.DEG_TO_RAD;
        float dx = hingeX - 8.0F, dz = hingeZ - 8.0F;
        float x = (8.0F + dx * Mth.cos(a) + dz * Mth.sin(a)) * PIXEL, z = (8.0F + dz * Mth.cos(a) - dx * Mth.sin(a)) * PIXEL;
        poseStack.pushPose();
        poseStack.translate(x, 0.0F, z);
        poseStack.mulPose(Axis.YP.rotation(angle));
        poseStack.translate(-x, 0.0F, -z);
        collector.submitMovingBlock(poseStack, part, 0);
        poseStack.popPose();
    }

    /** Draws a part turned round the front-to-back axis at height {@code y}, raised by {@code lift}, this far behind the machine in model pixels. */
    static void spun(PoseStack poseStack, SubmitNodeCollector collector, MovingBlockRenderState part, Direction facing, float behind, float y, float lift, float angle) {
        Direction back = facing.getOpposite();
        float x = 0.5F + back.getStepX() * behind * PIXEL, z = 0.5F + back.getStepZ() * behind * PIXEL;
        poseStack.pushPose();
        poseStack.translate(x, (y + lift) * PIXEL, z);
        poseStack.mulPose(new org.joml.Quaternionf().rotationAxis(angle, back.getStepX(), 0.0F, back.getStepZ()));
        poseStack.translate(-0.5F, -y * PIXEL, -0.5F);
        collector.submitMovingBlock(poseStack, part, 0);
        poseStack.popPose();
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

    static float fluidFill(GasTank tank) {
        return tank.capacity() > 0 ? Math.min(1.0F, (float) tank.amount() / tank.capacity()) : 0.0F;
    }

    static @Nullable TextureAtlasSprite fluidSprite(GasTank tank) {
        return tank.amount() > 0 ? FluidSprites.still(tank.fluid()) : null;
    }

    static int fluidTint(GasTank tank) {
        if (tank.amount() <= 0) {
            return 0;
        }
        return FluidSprites.tint(tank.fluid(), tank.amount());
    }

    /** The colour of a fluid's surface line. */
    static int lighter(int rgb) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        return r + (255 - r) * 2 / 5 << 16 | g + (255 - g) * 2 / 5 << 8 | b + (255 - b) * 2 / 5;
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
