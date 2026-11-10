package com.cyberfarm.client.renderer;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MachinePartBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.block.entity.SteamEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What moves on the assembled Steam Engine: a piston in every cylinder, the dynamo's flywheel and the boiler's coils, brighter the
 * faster it runs. Once, on assembling, the rail plates run out from under the boiler like a tape measure and each cell composes on
 * its plate. The parts are the static models' own boxes ({@link MachinePartBlock}), so nothing changes when they stop.
 * <p>
 * 4 part models and 4 boxes once assembled; during the 3.3 s of an opening 23 part models, rarely. Within the default 64 blocks.
 */
public class SteamEngineRenderer implements BlockEntityRenderer<SteamEngineBlockEntity, SteamEngineRenderer.State> {
    private static final int CYLINDERS = SteamEngineBlockEntity.CYLINDERS;
    /** Half a piston's stroke: 5 px in all. */
    private static final float SWING = 2.5F;
    /** The flywheel's axis height on the dynamo. */
    private static final float WHEEL_Y = 9.0F;
    private static final int MINT = 0xFF5EC2B8;
    private static final float CELL = 16.0F;
    /** The boiler's coil rings, each of two crossed boxes. */
    private static final float[][] RINGS = {{5.0F, 6.0F}, {8.6F, 9.6F}};

    /** A rail plate's run, in seconds; how far under the floor it runs, and how much deeper for each plate already ahead of it. */
    private static final float RAIL_GAP = 0.2F, RAIL_TIME = 0.7F, RAIL_DEPTH = 2.6F, RAIL_STACK = 2.7F;
    /** When each cylinder starts composing, and the dynamo. */
    private static final float CYLINDER_START = 1.1F, CYLINDER_GAP = 0.35F, DYNAMO_START = 2.0F;

    /** A part rising out of its rail plate: from this depth, starting this long after its cell does, for so many seconds. */
    private record Step(int part, float depth, float from, float seconds) {
        float eased(float second, float start) {
            return ease(second, start + from, start + from + seconds);
        }

        float rise(float second, float start) {
            return -depth * (1.0F - eased(second, start));
        }
    }

    private static final Step[] CYLINDER = {new Step(MachinePartBlock.STEAM_BASE, 2.15F, 0.0F, 0.45F),
            new Step(MachinePartBlock.STEAM_LINE, 13.65F, 0.2F, 0.5F), new Step(MachinePartBlock.STEAM_POSTS, 10.65F, 0.35F, 0.5F),
            new Step(MachinePartBlock.STEAM_LID, 11.45F, 0.7F, 0.35F)};
    private static final Step PISTON = new Step(MachinePartBlock.STEAM_PISTON, 8.05F, 0.9F, 0.4F);
    private static final Step[] DYNAMO = {new Step(MachinePartBlock.STEAM_DRUM, 11.5F, 0.0F, 0.6F),
            new Step(MachinePartBlock.STEAM_RIM, 12.85F, 0.4F, 0.6F), new Step(MachinePartBlock.STEAM_SHAFT, 13.65F, 0.8F, 0.4F)};
    private static final Step WHEEL = new Step(MachinePartBlock.STEAM_WHEEL, 12.85F, 0.4F, 0.6F);

    public static class State extends BlockEntityRenderState {
        int stage;
        Direction facing = Direction.NORTH;
        float seconds;
        float angle;
        float brightness;
        boolean rings;
        MovingBlockRenderState[] pistons = {};
        @Nullable MovingBlockRenderState wheel;
        /** The plates and the cells' other parts while they aren't the static models yet, or null. */
        MovingBlockRenderState @Nullable [] parts;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SteamEngineBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        state.stage = level == null ? SmallMultiblockBlock.LOOSE : entity.getBlockState().getValue(SmallMultiblockBlock.STAGE);
        state.parts = null;
        if (level == null) {
            return;
        }
        float time = level.getGameTime() + partialTick;
        state.seconds = entity.shownOpening(state.stage, time) / 20.0F;
        if (state.stage == SmallMultiblockBlock.LOOSE) {
            return;
        }
        boolean holding = entity.shownHolding(state.stage, time);
        state.rings = state.stage == SmallMultiblockBlock.OPEN || holding;
        state.facing = entity.getBlockState().getValue(MachineBlock.FACING);
        state.angle = entity.angle(partialTick);
        float pulse = 0.75F + 0.25F * Mth.sin(time * (0.1F + 0.3F * entity.speed()));
        state.brightness = 0.55F + 0.45F * entity.speed() * pulse;
        BlockPos pos = entity.getBlockPos();
        Direction back = state.facing.getOpposite();
        state.pistons = new MovingBlockRenderState[CYLINDERS];
        for (int i = 0; i < CYLINDERS; i++) {
            state.pistons[i] = ModelParts.movingPart(level, pos.relative(back, i + 1), PISTON.part(), state.facing);
        }
        BlockPos dynamo = pos.relative(back, CYLINDERS + 1);
        state.wheel = ModelParts.movingPart(level, dynamo, WHEEL.part(), state.facing);
        if (state.stage != SmallMultiblockBlock.OPEN || holding) {
            MovingBlockRenderState[] parts = new MovingBlockRenderState[CYLINDERS * (CYLINDER.length + 1) + DYNAMO.length + 1];
            int next = 0;
            for (int i = 1; i <= CYLINDERS; i++) {
                parts[next++] = ModelParts.movingPart(level, pos.relative(back, i), MachinePartBlock.STEAM_RAIL, state.facing);
                for (Step step : CYLINDER) {
                    parts[next++] = ModelParts.movingPart(level, pos.relative(back, i), step.part(), state.facing);
                }
            }
            parts[next++] = ModelParts.movingPart(level, dynamo, MachinePartBlock.STEAM_DYNAMO_RAIL, state.facing);
            for (Step step : DYNAMO) {
                parts[next++] = ModelParts.movingPart(level, dynamo, step.part(), state.facing);
            }
            state.parts = parts;
        }
    }

    private static float ease(float t, float from, float to) {
        float x = Mth.clamp((t - from) / (to - from), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** How far out a cell's rail plate has run, from 0 under the boiler to 1 in its place. */
    private static float run(float second, int cell) {
        return ease(second, RAIL_GAP * (cell - 1), RAIL_GAP * (cell - 1) + RAIL_TIME);
    }

    /** A plate's height at that point: under the floor, stacked below those ahead of it, until the last cell of its run. */
    private static float railY(int cell, float run) {
        float left = CELL * cell * (1.0F - run);
        return -RAIL_DEPTH * Math.min(1.0F, left / CELL) - RAIL_STACK * (cell - 1) * (1.0F - ease(run, 0.0F, 0.25F));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        MovingBlockRenderState wheel = state.wheel;
        if (state.stage == SmallMultiblockBlock.LOOSE || wheel == null) {
            return;
        }
        float second = state.seconds;
        MovingBlockRenderState[] parts = state.parts;
        int next = 0;
        for (int i = 1; i <= CYLINDERS; i++) {
            float run = run(second, i);
            float y = railY(i, run);
            float z = i * CELL * run;
            float start = CYLINDER_START + CYLINDER_GAP * (i - 1);
            if (parts != null) {
                ModelParts.slid(poseStack, collector, parts[next++], state.facing, 0.0F, y, z);
                for (Step step : CYLINDER) {
                    ModelParts.slid(poseStack, collector, parts[next++], state.facing, 0.0F, y + step.rise(second, start), z);
                }
            }
            // It only starts pumping once it is up, so the head never pokes through its plate
            float lift = SWING * Mth.sin(state.angle + i * Mth.HALF_PI) * PISTON.eased(second, start);
            ModelParts.slid(poseStack, collector, state.pistons[i - 1], state.facing, 0.0F, y + PISTON.rise(second, start) + lift, z);
        }
        int cell = CYLINDERS + 1;
        float run = run(second, cell);
        float y = railY(cell, run);
        float z = cell * CELL * run;
        if (parts != null) {
            ModelParts.slid(poseStack, collector, parts[next++], state.facing, 0.0F, y, z);
            for (Step step : DYNAMO) {
                ModelParts.slid(poseStack, collector, parts[next++], state.facing, 0.0F, y + step.rise(second, DYNAMO_START), z);
            }
        }
        ModelParts.spun(poseStack, collector, wheel, state.facing, z, WHEEL_Y, y + WHEEL.rise(second, DYNAMO_START), -state.angle);
        if (state.rings) {
            int ring = ARGB.scaleRGB(MINT, state.brightness);
            poseStack.pushPose();
            ModelParts.enterModel(poseStack, state.facing);
            collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
                for (float[] band : RINGS) {
                    ModelParts.box(pose, buffer, 4.8F, band[0], 1.2F, 11.2F, band[1], 14.8F, ring, ring, LightCoordsUtil.FULL_BRIGHT);
                    ModelParts.box(pose, buffer, 1.2F, band[0] + 0.02F, 4.8F, 14.8F, band[1] - 0.02F, 11.2F, ring, ring, LightCoordsUtil.FULL_BRIGHT);
                }
            });
            poseStack.popPose();
        }
    }

    // The row runs 4 blocks behind the boiler: keep drawing while any of it is in view
    @Override
    public AABB getRenderBoundingBox(SteamEngineBlockEntity entity) {
        Direction back = entity.getBlockState().getValue(MachineBlock.FACING).getOpposite();
        return new AABB(entity.getBlockPos()).expandTowards(back.getStepX() * (CYLINDERS + 1.0), 0.0, back.getStepZ() * (CYLINDERS + 1.0));
    }
}
