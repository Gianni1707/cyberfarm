package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.FluidTankBlock;
import com.cyberfarm.block.entity.FluidTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * A tank's glass and the fluid behind it. The panes run between the frame's pieces, right across a side's joints, so a joined tank has
 * one pane per side; the fluid is a volume behind them, up to the level of the whole tank, in its own texture and colour, so it works
 * for any mod's fluid. Gases are drawn see-through. Both use one render type, to be sorted against each other.
 * On the lid, once for the whole tank, a mark of what it holds: waves or a cloud, twice the size on a tank at least 2 by 2,
 * upright for whoever placed the block running the tank.
 * At most 10 quads a block, within the default 64 blocks.
 */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity, FluidTankRenderer.State> {
    private static final RenderType TRANSLUCENT = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final Identifier GLASS = CyberFarm.id("block/tank_glass");
    private static final Identifier WAVES = CyberFarm.id("block/tank_waves"), WAVES_LARGE = CyberFarm.id("block/tank_waves_x2");
    private static final Identifier CLOUD = CyberFarm.id("block/tank_cloud"), CLOUD_LARGE = CyberFarm.id("block/tank_cloud_x2");
    private static final float PIXEL = 1 / 16.0F;
    /** The frame's width, and how far in from the block's face the glass is and the fluid, right behind it so it fills the tank; the frame hides its edges. */
    private static final float FRAME = 3.0F * PIXEL, PANE = 1.5F * PIXEL, FLUID = 1.8F * PIXEL;
    private static final int GAS_ALPHA = 0xA0, CLEAR = 0xFFFFFFFF;
    /** Just above the lid's plate. */
    private static final float SYMBOL_Y = 15.02F * PIXEL;

    public static class State extends BlockEntityRenderState {
        @Nullable TextureAtlasSprite glass;
        @Nullable TextureAtlasSprite fluid;
        int color;
        int light;
        final boolean[] joined = new boolean[6];
        float fluidTop;
        float fluidBottom;
        boolean surface;
        @Nullable TextureAtlasSprite symbol;
        /** The part of the mark on this block: where, and which part of the texture. */
        float symbolX0, symbolX1, symbolZ0, symbolZ1, symbolU0, symbolU1, symbolV0, symbolV1;
        /** The way the mark's bottom points. */
        Direction symbolFacing = Direction.SOUTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FluidTankBlockEntity entity, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        state.fluid = null;
        Level level = entity.getLevel();
        if (level == null) {
            state.glass = null;
            return;
        }
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        state.glass = atlas.getSprite(GLASS);
        extractSymbol(entity, state, atlas, level);
        state.light = LightCoordsUtil.getLightCoords(level, entity.getBlockPos());
        for (Direction side : Direction.values()) {
            state.joined[side.ordinal()] = entity.joined(side);
        }
        FluidTankBlockEntity runner = entity.runner();
        if (runner == null || runner.shownFill() <= 0.0F) {
            return;
        }
        int low = entity.boxMin().getY();
        int height = entity.boxMax().getY() - low + 1;
        int y = entity.getBlockPos().getY();
        float surface = FRAME + runner.shownFill() * (height - 2 * FRAME) - (y - low);
        float bottom = y == low ? FRAME : 0.0F;
        float ceiling = y == entity.boxMax().getY() ? 1.0F - FRAME : 1.0F;
        if (surface <= bottom) {
            return;
        }
        state.fluidBottom = bottom;
        state.fluidTop = Math.min(surface, ceiling);
        state.surface = surface < ceiling;
        Fluid fluid = runner.shownFluid();
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        FluidTintSource tint = model.fluidTintSource();
        int color = tint != null ? tint.colorAsStack(new FluidStack(fluid, 1000)) : 0xFFFFFFFF;
        int alpha = fluid.getFluidType().isLighterThanAir() ? GAS_ALPHA : 0xFF;
        state.fluid = model.stillMaterial().sprite();
        state.color = (alpha << 24) | (color & 0x00FFFFFF);
    }

    /** The mark is centred on the tank's lid, so it can cross several blocks: each draws its own part. A pipe on the lid's centre would cover it, so then there is none. */
    private static void extractSymbol(FluidTankBlockEntity entity, State state, TextureAtlas atlas, Level level) {
        state.symbol = null;
        BlockPos pos = entity.getBlockPos();
        BlockPos min = entity.boxMin();
        BlockPos max = entity.boxMax();
        if (pos.getY() != max.getY() || !(entity.getBlockState().getBlock() instanceof FluidTankBlock block)) {
            return;
        }
        int wide = max.getX() - min.getX() + 1;
        int deep = max.getZ() - min.getZ() + 1;
        // shortcut: a pipe on the centre hides the mark rather than moving it elsewhere on the lid; find a free block if that shows.
        for (int x = min.getX() + (wide - 1) / 2; x <= min.getX() + wide / 2; x++) {
            for (int z = min.getZ() + (deep - 1) / 2; z <= min.getZ() + deep / 2; z++) {
                BlockState centre = level.getBlockState(new BlockPos(x, pos.getY(), z));
                if (centre.getBlock() instanceof FluidTankBlock && centre.getValue(FluidTankBlock.SIDES.get(Direction.UP)) == FluidTankBlock.Side.SOCKET) {
                    return;
                }
            }
        }
        boolean large = Math.min(wide, deep) >= 2;
        float size = large ? 2.0F : 1.0F;
        float x0 = min.getX() + wide / 2.0F - size / 2.0F - pos.getX();
        float z0 = min.getZ() + deep / 2.0F - size / 2.0F - pos.getZ();
        state.symbolX0 = Math.max(x0, 0.0F);
        state.symbolX1 = Math.min(x0 + size, 1.0F);
        state.symbolZ0 = Math.max(z0, 0.0F);
        state.symbolZ1 = Math.min(z0 + size, 1.0F);
        if (state.symbolX0 >= state.symbolX1 || state.symbolZ0 >= state.symbolZ1) {
            return;
        }
        state.symbolU0 = (state.symbolX0 - x0) / size;
        state.symbolU1 = (state.symbolX1 - x0) / size;
        state.symbolV0 = (state.symbolZ0 - z0) / size;
        state.symbolV1 = (state.symbolZ1 - z0) / size;
        state.symbol = atlas.getSprite(block.gas() ? (large ? CLOUD_LARGE : CLOUD) : (large ? WAVES_LARGE : WAVES));
        // All of a tank's blocks must agree, and they may have been placed from different sides
        BlockState runner = level.getBlockState(min);
        state.symbolFacing = runner.getBlock() instanceof FluidTankBlock ? runner.getValue(FluidTankBlock.FACING) : Direction.SOUTH;
    }

    /** A side's bound: the given inset from the block's edge, or the edge itself where the tank goes on. */
    private static float low(State state, Direction side, float inset) {
        return state.joined[side.ordinal()] ? 0.0F : inset;
    }

    private static float high(State state, Direction side, float inset) {
        return state.joined[side.ordinal()] ? 1.0F : 1.0F - inset;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite glass = state.glass;
        if (glass == null) {
            return;
        }
        collector.submitCustomGeometry(poseStack, TRANSLUCENT, (pose, buffer) -> {
            TextureAtlasSprite fluid = state.fluid;
            float west = low(state, Direction.WEST, FLUID), east = high(state, Direction.EAST, FLUID);
            float north = low(state, Direction.NORTH, FLUID), south = high(state, Direction.SOUTH, FLUID);
            if (fluid != null) {
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    if (!state.joined[side.ordinal()]) {
                        side(pose, buffer, side, west, east, north, south, state.fluidBottom, state.fluidTop, fluid, state.color, state.light);
                    }
                }
                if (state.surface) {
                    vertex(pose, buffer, west, state.fluidTop, south, west, south, 0, 1, 0, fluid, state.color, state.light);
                    vertex(pose, buffer, east, state.fluidTop, south, east, south, 0, 1, 0, fluid, state.color, state.light);
                    vertex(pose, buffer, east, state.fluidTop, north, east, north, 0, 1, 0, fluid, state.color, state.light);
                    vertex(pose, buffer, west, state.fluidTop, north, west, north, 0, 1, 0, fluid, state.color, state.light);
                }
            }
            float paneWest = low(state, Direction.WEST, FRAME), paneEast = high(state, Direction.EAST, FRAME);
            float paneNorth = low(state, Direction.NORTH, FRAME), paneSouth = high(state, Direction.SOUTH, FRAME);
            float paneBottom = low(state, Direction.DOWN, FRAME), paneTop = high(state, Direction.UP, FRAME);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (!state.joined[side.ordinal()]) {
                    side(pose, buffer, side, side == Direction.WEST ? PANE : paneWest, side == Direction.EAST ? 1.0F - PANE : paneEast,
                            side == Direction.NORTH ? PANE : paneNorth, side == Direction.SOUTH ? 1.0F - PANE : paneSouth,
                            paneBottom, paneTop, glass, CLEAR, state.light);
                }
            }
            TextureAtlasSprite symbol = state.symbol;
            if (symbol != null) {
                symbolVertex(pose, buffer, state.symbolX0, state.symbolZ1, state.symbolU0, state.symbolV1, state, symbol);
                symbolVertex(pose, buffer, state.symbolX1, state.symbolZ1, state.symbolU1, state.symbolV1, state, symbol);
                symbolVertex(pose, buffer, state.symbolX1, state.symbolZ0, state.symbolU1, state.symbolV0, state, symbol);
                symbolVertex(pose, buffer, state.symbolX0, state.symbolZ0, state.symbolU0, state.symbolV0, state, symbol);
            }
        });
    }

    /** A corner of the mark; fx and fz say where on it, from its north-west corner as it would lie with its bottom to the south. */
    private static void symbolVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float z, float fx, float fz, State state, TextureAtlasSprite sprite) {
        float u = switch (state.symbolFacing) {
            case EAST -> 1.0F - fz;
            case NORTH -> 1.0F - fx;
            case WEST -> fz;
            default -> fx;
        };
        float v = switch (state.symbolFacing) {
            case EAST -> fx;
            case NORTH -> 1.0F - fz;
            case WEST -> 1.0F - fx;
            default -> fz;
        };
        vertex(pose, buffer, x, SYMBOL_Y, z, u, v, 0, 1, 0, sprite, CLEAR, state.light);
    }

    /** The outward face of a box's side: its first corner is on the left of someone looking at it from outside. */
    private static void side(PoseStack.Pose pose, VertexConsumer buffer, Direction side, float west, float east, float north, float south,
            float bottom, float top, TextureAtlasSprite sprite, int color, int light) {
        float[] ends = switch (side) {
            case NORTH -> new float[] {east, north, west, north};
            case EAST -> new float[] {east, south, east, north};
            case SOUTH -> new float[] {west, south, east, south};
            default -> new float[] {west, north, west, south};
        };
        int nx = side.getStepX(), nz = side.getStepZ();
        // Across the texture: along the side, whichever way it runs
        float u0 = nz != 0 ? ends[0] : ends[1], u1 = nz != 0 ? ends[2] : ends[3];
        vertex(pose, buffer, ends[0], bottom, ends[1], u0, 1.0F - bottom, nx, 0, nz, sprite, color, light);
        vertex(pose, buffer, ends[2], bottom, ends[3], u1, 1.0F - bottom, nx, 0, nz, sprite, color, light);
        vertex(pose, buffer, ends[2], top, ends[3], u1, 1.0F - top, nx, 0, nz, sprite, color, light);
        vertex(pose, buffer, ends[0], top, ends[1], u0, 1.0F - top, nx, 0, nz, sprite, color, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v,
            int nx, int ny, int nz, TextureAtlasSprite sprite, int color, int light) {
        buffer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(sprite.getU(u), sprite.getV(v))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
