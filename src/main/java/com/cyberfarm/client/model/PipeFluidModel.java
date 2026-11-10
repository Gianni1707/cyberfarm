package com.cyberfarm.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.NetworkPipeBlock;
import com.cyberfarm.block.entity.NetworkPipeBlockEntity;
import com.cyberfarm.port.client.FluidSprites;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Adds to a pipe's model what is going through it right now: an Energy Conduit's arc, a Gas or Liquid Pipe's fluid in its own texture and
 * colour. It is part of the chunk's mesh, so it costs nothing per frame however many pipes are in sight and the textures animate with the
 * atlas; the mesh is only rebuilt when a flow starts, stops or changes fluid. A fluid fills the glass from the block's edge, through the
 * node's glass cube where arms meet, a gas with puffs drifting along its arms; the arc runs along two crossed sheets in the glass, meeting
 * at the centre. At most 54 quads.
 * <p>
 * On 1.20.1 it wraps the pipe's baked model and adds the quads in the translucent pass. That pass ignores a quad's vertex alpha, so a gas
 * shows as opaque as its texture where 26.x thins it to about 60%.
 */
public final class PipeFluidModel extends BakedModelWrapper<BakedModel> {
    private static final float PIXEL = 1 / 16.0F;
    /** The glass runs to the block's edge, into the next pipe or a machine's collar. */
    private static final float NEAR = 0;
    /** Just inside the 4.9 px glass, the gas's puffs between the two, and how far the arc's sheets reach from the centre. */
    private static final float HALF = 2.3F, OVERLAY_HALF = 2.35F, ARC_HALF = 1.5F;
    private static final int GAS_ALPHA = 0xA0;
    private static final ResourceLocation CLOUD = CyberFarm.id("block/pipe_gas_cloud"), ARC_A = CyberFarm.id("block/energy_conduit_arc_a"),
            ARC_B = CyberFarm.id("block/energy_conduit_arc_b");

    /** {@code shown} is a fluid or {@link NetworkPipeBlockEntity#ENERGY}. */
    private record Key(Object shown, int connections) {}

    private final Map<Key, List<BakedQuad>> parts;

    private PipeFluidModel(BakedModel delegate, Map<Key, List<BakedQuad>> parts) {
        super(delegate);
        this.parts = parts;
    }

    public static void wrap(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        // Sprites move on a resource reload, so what is built from them lasts one bake
        Map<Key, List<BakedQuad>> parts = new ConcurrentHashMap<>();
        Map<BakedModel, BakedModel> wrapped = new IdentityHashMap<>();
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block instanceof NetworkPipeBlock) {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    models.computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                            (key, model) -> wrapped.computeIfAbsent(model, plain -> new PipeFluidModel(plain, parts)));
                }
            }
        }
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        ChunkRenderTypeSet own = super.getRenderTypes(state, rand, data);
        return data.get(NetworkPipeBlockEntity.SHOWN) == null ? own : ChunkRenderTypeSet.union(own, ChunkRenderTypeSet.of(RenderType.translucent()));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        // The pipe's own quads only in its own passes, or they would be drawn again in the translucent one
        List<BakedQuad> own = state == null || renderType == null || super.getRenderTypes(state, rand, data).contains(renderType)
                ? super.getQuads(state, side, rand, data, renderType) : List.of();
        Object shown = data.get(NetworkPipeBlockEntity.SHOWN);
        if (shown == null || side != null || state == null || !(state.getBlock() instanceof NetworkPipeBlock)
                || renderType != null && renderType != RenderType.translucent()) {
            return own;
        }
        List<BakedQuad> flow = parts.computeIfAbsent(new Key(shown, connections(state)), PipeFluidModel::build);
        List<BakedQuad> all = new ArrayList<>(own.size() + flow.size());
        all.addAll(own);
        all.addAll(flow);
        return all;
    }

    private static int connections(BlockState state) {
        int mask = 0;
        for (Direction side : Direction.values()) {
            if (state.getValue(NetworkPipeBlock.CONNECTIONS.get(side))) {
                mask |= 1 << side.ordinal();
            }
        }
        return mask;
    }

    private static List<BakedQuad> build(Key key) {
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        List<BakedQuad> quads = new ArrayList<>();
        Direction.Axis straight = straightAxis(key.connections());
        if (!(key.shown() instanceof Fluid fluid)) {
            for (Direction side : arms(key.connections(), straight)) {
                float far = straight != null ? 16 - NEAR : 8;
                List<Direction.Axis> across = Arrays.stream(Direction.Axis.VALUES).filter(axis -> axis != side.getAxis()).toList();
                new Faces(quads, atlas.getSprite(ARC_A), 0xFFFFFFFF, 15, side.getAxis()).sheet(side, across.get(0), ARC_HALF, far);
                new Faces(quads, atlas.getSprite(ARC_B), 0xFFFFFFFF, 15, side.getAxis()).sheet(side, across.get(1), ARC_HALF, far);
            }
            return List.copyOf(quads);
        }
        int rgb = FluidSprites.tint(fluid, 1000);
        boolean gas = fluid.getFluidType().isLighterThanAir();
        TextureAtlasSprite still = FluidSprites.still(fluid);
        int light = fluid.getFluidType().getLightLevel();
        Faces faces = new Faces(quads, still, ((gas ? GAS_ALPHA : 0xFF) << 24) | (rgb & 0xFFFFFF), light);
        for (Direction side : arms(key.connections(), straight)) {
            // Up to the core's box, whose walls stand only where no arm comes in
            faces.arm(side, HALF, straight != null ? 16 - NEAR : 8 - HALF);
            if (gas) {
                new Faces(quads, atlas.getSprite(CLOUD), 0xFFFFFFFF, light, side.getAxis())
                        .arm(side, OVERLAY_HALF, straight != null ? 16 - NEAR : 8 - OVERLAY_HALF);
            }
        }
        if (straight == null) {
            for (Direction side : Direction.values()) {
                if ((key.connections() & 1 << side.ordinal()) == 0) {
                    faces.core(side, HALF);
                }
            }
        }
        return List.copyOf(quads);
    }

    /** The sides an arm runs from: on a straight run one box from end to end does for both. */
    private static List<Direction> arms(int connections, Direction.@Nullable Axis straight) {
        if (straight != null) {
            return List.of(Direction.fromAxisAndDirection(straight, Direction.AxisDirection.NEGATIVE));
        }
        return Arrays.stream(Direction.values()).filter(side -> (connections & 1 << side.ordinal()) != 0).toList();
    }

    private static Direction.@Nullable Axis straightAxis(int connections) {
        for (Direction.Axis axis : Direction.Axis.VALUES) {
            int both = 1 << Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE).ordinal()
                    | 1 << Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE).ordinal();
            if (connections == both) {
                return axis;
            }
        }
        return null;
    }

    /**
     * The quads of a box's sides, in px. The texture is mapped by position, so it runs on unbroken from block to block: by the face's own
     * axes for the fluid, or with u along the pipe and v across it for an overlay, whose frames move along u.
     */
    private static final class Faces {
        private final List<BakedQuad> quads;
        private final TextureAtlasSprite sprite;
        private final int argb;
        private final int light;
        private final Direction.@Nullable Axis along;

        Faces(List<BakedQuad> quads, TextureAtlasSprite sprite, int argb, int light) {
            this(quads, sprite, argb, light, null);
        }

        Faces(List<BakedQuad> quads, TextureAtlasSprite sprite, int argb, int light, Direction.@Nullable Axis along) {
            this.quads = quads;
            this.sprite = sprite;
            this.along = along;
            this.argb = argb;
            this.light = light;
        }

        /** A box from {@code side}'s edge, {@code far} px in, with no end faces. */
        void arm(Direction side, float half, float far) {
            float[] min = {8 - half, 8 - half, 8 - half}, max = {8 + half, 8 + half, 8 + half};
            span(side, far, min, max);
            for (Direction face : Direction.values()) {
                if (face.getAxis() != side.getAxis()) {
                    face(face, min, max);
                }
            }
        }

        /** One wall of the box where arms meet. */
        void core(Direction face, float half) {
            float low = 8 - half, high = 8 + half;
            face(face, new float[] {low, low, low}, new float[] {high, high, high});
        }

        /** A flat sheet through the centre from {@code side}'s edge, facing along {@code normal}, seen from both sides. */
        void sheet(Direction side, Direction.Axis normal, float half, float far) {
            float[] min = {8 - half, 8 - half, 8 - half}, max = {8 + half, 8 + half, 8 + half};
            span(side, far, min, max);
            min[normal.ordinal()] = 8;
            max[normal.ordinal()] = 8;
            face(Direction.fromAxisAndDirection(normal, Direction.AxisDirection.NEGATIVE), min, max);
            face(Direction.fromAxisAndDirection(normal, Direction.AxisDirection.POSITIVE), min, max);
        }

        private static void span(Direction side, float far, float[] min, float[] max) {
            int axis = side.getAxis().ordinal();
            boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            min[axis] = positive ? 16 - far : NEAR;
            max[axis] = positive ? 16 - NEAR : far;
        }

        private void face(Direction face, float[] min, float[] max) {
            float x0 = min[0], y0 = min[1], z0 = min[2], x1 = max[0], y1 = max[1], z1 = max[2];
            float[][] corners = switch (face) {
                case DOWN -> new float[][] {{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
                case UP -> new float[][] {{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
                case NORTH -> new float[][] {{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
                case SOUTH -> new float[][] {{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
                case WEST -> new float[][] {{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
                case EAST -> new float[][] {{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
            };
            QuadBakingVertexConsumer.Buffered baker = new QuadBakingVertexConsumer.Buffered();
            baker.setSprite(sprite);
            baker.setDirection(face);
            // Lit at full (the arc) means no shading either
            baker.setShade(light < 15);
            baker.setHasAmbientOcclusion(false);
            for (float[] corner : corners) {
                float[] uv = uv(face, corner);
                baker.vertex(corner[0] * PIXEL, corner[1] * PIXEL, corner[2] * PIXEL)
                        .color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24)
                        .uv(uv[0], uv[1])
                        .uv2(0, 0)
                        .normal(face.getStepX(), face.getStepY(), face.getStepZ())
                        .endVertex();
            }
            BakedQuad quad = baker.getQuad();
            if (light > 0) {
                QuadTransformers.settingEmissivity(light).processInPlace(quad);
            }
            quads.add(quad);
        }

        private float[] uv(Direction face, float[] px) {
            if (along != null) {
                // The third axis, neither the pipe's nor the face's: on a level pipe's flank that is the height
                int across = 3 - along.ordinal() - face.getAxis().ordinal();
                return new float[] {sprite.getU(16.0F * (px[along.ordinal()] * PIXEL)), sprite.getV(16.0F * (px[across] * PIXEL))};
            }
            float u = (face.getAxis() == Direction.Axis.X ? px[2] : px[0]) * PIXEL;
            float v = (face.getAxis() == Direction.Axis.Y ? px[2] : 16 - px[1]) * PIXEL;
            return new float[] {sprite.getU(16.0F * (u)), sprite.getV(16.0F * (v))};
        }
    }
}
