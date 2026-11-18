package com.cyberfarm.client.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.BatteryBankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.cyberfarm.registry.ModItems;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Battery Bank's display and gauges, on the bank's outer faces only, so merged blocks meet flush. On the front one
 * bezelled screen spanning the whole front, drawn by one bottom corner block: the charge, the FE/t coming in and going
 * out and a charge bar; a bigger bank's screen spells the numbers out and adds what it stores. On both sides a gauge
 * running the bank's whole height, filled as far as it is charged. The block running the bank keeps the numbers
 * (synced twice a second) and its facing is the bank's front. Model pixels, front to -z as enterModel turns it.
 */
public class BatteryBankRenderer implements BlockEntityRenderer<BatteryBankBlockEntity, BatteryBankRenderer.State> {
    private static final int LIGHT_FULL = 0xF000F0;
    private static final int STEEL = 0xFFBDC6C5, STEEL_SIDE = 0xFF96A3A2, SCREEN = 0xFF071314, WELL = 0xFF0D1C1C;
    private static final int MINT = 0xFF5EC2B8, GLOW = 0xFFD1FEE7, EMERALD = 0xFF00B894, DIM = 0xFF2F6468;

    private final Font font;

    public BatteryBankRenderer(Font font) {
        this.font = font;
    }

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean screen;
        /** The bank's front in blocks, and this block's place in it: columns count towards the facing's clockwise side. */
        int width = 1;
        int height = 1;
        int column;
        int row;
        boolean left;
        boolean right;
        float fill;
        long energy;
        long capacity;
        int in;
        int out;
        int light;
        /** Holding the Cable Swapper: each outer side's mode, by direction (null where the side is inside the bank). */
        final BatteryBankBlockEntity.@Nullable FaceMode[] sides = new BatteryBankBlockEntity.FaceMode[6];
        boolean showSides;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BatteryBankBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        Level level = entity.getLevel();
        BatteryBankBlockEntity runner = entity.runner();
        state.facing = runner.getBlockState().getValue(MachineBlock.FACING);
        Direction right = state.facing.getClockWise();
        BlockPos pos = entity.getBlockPos(), min = entity.boxMin(), max = entity.boxMax();
        state.width = Math.abs(along(right, max) - along(right, min)) + 1;
        state.height = max.getY() - min.getY() + 1;
        state.column = along(right, pos) - Math.min(along(right, min), along(right, max));
        state.row = pos.getY() - min.getY();
        // A face shows its part only where it is outside the bank; the front's one screen from its first block
        state.screen = !entity.joined(state.facing) && state.column == 0 && state.row == 0;
        state.right = !entity.joined(right);
        state.left = !entity.joined(state.facing.getCounterClockWise());
        state.energy = runner.shownEnergy();
        state.capacity = runner.shownCapacity();
        state.in = runner.shownIn();
        state.out = runner.shownOut();
        state.fill = state.capacity > 0 ? Mth.clamp((float) state.energy / state.capacity, 0.0F, 1.0F) : 0.0F;
        state.light = level == null ? LIGHT_FULL : LevelRenderer.getLightCoords(level, entity.getBlockPos());
        Player player = Minecraft.getInstance().player;
        state.showSides = player != null && (player.getMainHandItem().is(ModItems.CABLE_SWAPPER.get()) || player.getOffhandItem().is(ModItems.CABLE_SWAPPER.get()));
        for (Direction side : Direction.values()) {
            state.sides[side.get3DDataValue()] = state.showSides && !entity.joined(side) ? entity.mode(side) : null;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        ModelParts.enterModel(poseStack, state.facing);
        int light = state.light;
        float fill = state.fill;
        float w = state.width * 16.0F, h = state.height * 16.0F, barTop = barTop(state);
        // The side gauge runs the bank's whole height: this block draws its own stretch of it
        float bottom = state.row == 0 ? 2.5F : 0.0F, top = state.row == state.height - 1 ? 14.0F : 16.0F;
        float wellBottom = state.row == 0 ? 3.5F : 0.0F, wellTop = state.row == state.height - 1 ? 13.0F : 16.0F;
        float level = 3.5F + fill * (h - 3.0F - 3.5F) - state.row * 16.0F;
        float filled = Mth.clamp(level, wellBottom, wellTop);
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            if (state.screen) {
                // The bezel, the screen, and a charge bar along the screen's bottom, across the whole front
                ModelParts.box(pose, buffer, 2.5F, 3.0F, -0.3F, w - 2.5F, h - 2.5F, 0.0F, STEEL, STEEL_SIDE, light);
                ModelParts.box(pose, buffer, 3.2F, 3.7F, -0.36F, w - 3.2F, h - 3.2F, -0.3F, SCREEN, SCREEN, LIGHT_FULL);
                ModelParts.box(pose, buffer, 3.8F, 4.3F, -0.4F, w - 3.8F, barTop, -0.36F, WELL, WELL, LIGHT_FULL);
                if (fill > 0) {
                    // Model x grows towards the viewer's left, so the bar fills from the high end to read left to right
                    ModelParts.box(pose, buffer, w - 3.8F - (w - 7.6F) * fill, 4.3F, -0.44F, w - 3.8F, barTop, -0.4F, MINT, MINT, LIGHT_FULL);
                }
            }
            for (int side = 0; side < 2; side++) {
                boolean shown = side == 0 ? state.left : state.right;
                if (!shown) {
                    continue;
                }
                // x = -0.4..0 on one side of the cell, 16..16.4 on the other
                float out = side == 0 ? -1.0F : 1.0F;
                float face = side == 0 ? 0.0F : 16.0F;
                ModelParts.box(pose, buffer, Math.min(face, face + out * 0.3F), bottom, 5.5F, Math.max(face, face + out * 0.3F), top, 10.5F,
                        STEEL, STEEL_SIDE, light);
                ModelParts.box(pose, buffer, Math.min(face, face + out * 0.36F), wellBottom, 6.5F, Math.max(face, face + out * 0.36F), wellTop,
                        9.5F, WELL, WELL, LIGHT_FULL);
                if (fill > 0 && filled > wellBottom) {
                    ModelParts.box(pose, buffer, Math.min(face, face + out * 0.42F), wellBottom, 6.5F, Math.max(face, face + out * 0.42F), filled,
                            9.5F, MINT, MINT, LIGHT_FULL);
                    if (level <= wellTop) {
                        // The bright edge where the charge stops, only in the block it stops in
                        ModelParts.box(pose, buffer, Math.min(face, face + out * 0.44F), Math.max(wellBottom, filled - 0.4F), 6.5F,
                                Math.max(face, face + out * 0.44F), filled, 9.5F, GLOW, GLOW, LIGHT_FULL);
                    }
                }
            }
        });
        if (state.screen) {
            text(state, poseStack, collector);
        }
        if (state.showSides) {
            sides(state, poseStack, collector);
        }
        poseStack.popPose();
    }

    /** In takes FE, Out gives it, Off is not connected: the Cable Swapper's view of the sides, as the GUI's flows. */
    private static final int[] MODE_COLOURS = {0xFF00B894, 0xFFFFB347, 0xFF7C8989};

    /**
     * With the Cable Swapper in hand, a frame round the edge of each outer side in its mode's colour. Model space: front
     * -z, the facing's clockwise side at x 16.
     */
    private static void sides(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        collector.submitCustomGeometry(poseStack, ModelParts.SOLID, (pose, buffer) -> {
            for (Direction side : Direction.values()) {
                BatteryBankBlockEntity.FaceMode mode = state.sides[side.get3DDataValue()];
                if (mode == null) {
                    continue;
                }
                int colour = MODE_COLOURS[mode.ordinal()];
                Direction face = side == state.facing ? Direction.NORTH : side == state.facing.getOpposite() ? Direction.SOUTH
                        : side == state.facing.getClockWise() ? Direction.EAST : side == state.facing.getCounterClockWise() ? Direction.WEST : side;
                // The frame, a pixel in from the edge
                face(pose, buffer, face, 0.5F, 0.5F, 15.5F, 1.5F, colour);
                face(pose, buffer, face, 0.5F, 14.5F, 15.5F, 15.5F, colour);
                face(pose, buffer, face, 0.5F, 1.5F, 1.5F, 14.5F, colour);
                face(pose, buffer, face, 14.5F, 1.5F, 15.5F, 14.5F, colour);
            }
        });
    }

    /** A flat box on the outside of a model face, from (u0, v0) to (u1, v1) in that face's own two axes. */
    private static void face(PoseStack.Pose pose, VertexConsumer buffer, Direction face, float u0, float v0, float u1, float v1, int colour) {
        float out = 0.06F;
        switch (face) {
            case NORTH -> ModelParts.box(pose, buffer, u0, v0, -out, u1, v1, 0.0F, colour, colour, LIGHT_FULL);
            case SOUTH -> ModelParts.box(pose, buffer, u0, v0, 16.0F, u1, v1, 16.0F + out, colour, colour, LIGHT_FULL);
            case WEST -> ModelParts.box(pose, buffer, -out, v0, u0, 0.0F, v1, u1, colour, colour, LIGHT_FULL);
            case EAST -> ModelParts.box(pose, buffer, 16.0F, v0, u0, 16.0F + out, v1, u1, colour, colour, LIGHT_FULL);
            case UP -> ModelParts.box(pose, buffer, u0, 16.0F, v0, u1, 16.0F + out, v1, colour, colour, LIGHT_FULL);
            case DOWN -> ModelParts.box(pose, buffer, u0, -out, v0, u1, 0.0F, v1, colour, colour, LIGHT_FULL);
        }
    }

    /** The charge bar's top: thicker on a bigger screen. */
    private static float barTop(State state) {
        return 4.3F + 0.8F * Math.min(state.width, state.height);
    }

    /** One row of the screen: its text, colour, and how much of the screen's height it takes. */
    private record Row(Component text, int colour, float weight) {}

    private static final String SCREEN_KEY = "block.cyberfarm.battery_bank.screen.";

    /**
     * What the screen says, top down. One block: the charge and the flows, short. Wider: the flows spelled out and the
     * stored energy beside the charge. Taller: a label above each number. Both: labels and numbers spelled out.
     */
    private static List<Row> rows(State state) {
        boolean wide = state.width > 1, tall = state.height > 1;
        Component percent = Component.literal(state.capacity > 0 ? Math.round(state.fill * 100) + "%" : "--%");
        int inColour = state.in > 0 ? EMERALD : DIM, outColour = state.out > 0 ? GLOW : DIM;
        String in = wide ? full(state.in) : compact(state.in), out = wide ? full(state.out) : compact(state.out);
        Component stored = Component.translatable(SCREEN_KEY + "stored_amount", wide ? full(state.energy) : compact(state.energy),
                wide ? full(state.capacity) : compact(state.capacity));
        Component inRate = wide ? Component.translatable(SCREEN_KEY + "rate", in) : Component.literal(in);
        Component outRate = wide ? Component.translatable(SCREEN_KEY + "rate", out) : Component.literal(out);
        List<Row> rows = new ArrayList<>();
        if (!tall) {
            rows.add(new Row(wide ? Component.empty().append(percent).append("   ").append(stored) : percent, GLOW, 1));
            rows.add(new Row(Component.translatable(SCREEN_KEY + "in", inRate), inColour, 1));
            rows.add(new Row(Component.translatable(SCREEN_KEY + "out", outRate), outColour, 1));
            return rows;
        }
        rows.add(new Row(Component.translatable(SCREEN_KEY + "charge"), MINT, 1));
        rows.add(new Row(percent, GLOW, 2));
        if (wide) {
            rows.add(new Row(Component.translatable(SCREEN_KEY + "in", inRate), inColour, 1));
            rows.add(new Row(Component.translatable(SCREEN_KEY + "out", outRate), outColour, 1));
        } else {
            rows.add(new Row(Component.translatable(SCREEN_KEY + "in_label"), MINT, 1));
            rows.add(new Row(inRate, inColour, 1));
            rows.add(new Row(Component.translatable(SCREEN_KEY + "out_label"), MINT, 1));
            rows.add(new Row(outRate, outColour, 1));
        }
        rows.add(new Row(Component.translatable(SCREEN_KEY + "stored"), MINT, 1));
        rows.add(new Row(stored, GLOW, 1));
        return rows;
    }

    /** The rows stacked between the screen's top and the charge bar, each as big as its share of the height and the width allow. */
    private void text(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        List<Row> rows = rows(state);
        float w = state.width * 16.0F, top = state.height * 16.0F - 3.6F, bottom = barTop(state) + 0.3F;
        float total = 0;
        for (Row row : rows) {
            total += row.weight();
        }
        float unit = (top - bottom) / total, y = top;
        for (Row row : rows) {
            float slot = unit * row.weight();
            float scale = Math.min(slot * 0.85F / font.lineHeight, (w - 8.4F) / Math.max(1, font.width(row.text())));
            line(poseStack, collector, row.text(), w / 2.0F, y - (slot - scale * font.lineHeight) / 2.0F, scale, row.colour());
            y -= slot;
        }
    }

    /** One line centred on {@code x}, its top at {@code top} model pixels, facing out of the front (-z). */
    private void line(PoseStack poseStack, SubmitNodeCollector collector, Component text, float x, float top, float scale, int colour) {
        poseStack.pushPose();
        poseStack.translate(x, top, -0.42F);
        // Text faces +z and runs along +x: turned round to face out of the front, then flipped so it reads top down
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(scale, -scale, scale);
        collector.submitText(poseStack, -font.width(text) / 2.0F, 0.0F, text.getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                LIGHT_FULL, colour, 0, 0);
        poseStack.popPose();
    }

    /** The whole bank, for its front's screen spans it. */
    @Override
    public AABB getRenderBoundingBox(BatteryBankBlockEntity entity) {
        BlockPos min = entity.boxMin(), max = entity.boxMax();
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }

    /** A block's place along a horizontal {@code direction}: its coordinate on that axis, counted the way it runs. */
    static int along(Direction direction, BlockPos pos) {
        return direction.getStepX() * pos.getX() + direction.getStepZ() * pos.getZ();
    }

    /** A number spelled out for a big screen: 12,400. */
    static String full(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** A number short enough for one block's screen: 950, 12.4k, 1.2M. */
    static String compact(long value) {
        if (value < 1_000) {
            return Long.toString(value);
        }
        if (value < 1_000_000) {
            return value < 10_000 ? String.format(Locale.ROOT, "%.1fk", value / 1000.0F) : value / 1000 + "k";
        }
        if (value < 1_000_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0F);
        }
        return String.format(Locale.ROOT, "%.1fG", value / 1_000_000_000.0F);
    }
}
