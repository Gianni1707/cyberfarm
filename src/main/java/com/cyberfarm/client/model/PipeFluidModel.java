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

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Adds to a pipe's model what is going through it right now: an Energy Conduit's arc, a Gas or Liquid Pipe's fluid in its own texture and
 * colour. It is part of the chunk's mesh, so it costs nothing per frame however many pipes are in sight and the textures animate with the
 * atlas; the mesh is only rebuilt when a flow starts, stops or changes fluid. A fluid fills the glass from the block's edge, through the
 * node's glass cube where arms meet, a gas with puffs drifting along its arms; the arc runs along two crossed sheets in the glass, meeting
 * at the centre. At most 54 quads.
 */
public final class PipeFluidModel extends DelegateBlockStateModel {
    private static final float PIXEL = 1 / 16.0F;
    /** The glass runs to the block's edge, into the next pipe or a machine's collar. */
    private static final float NEAR = 0;
    /** Just inside the 4.9 px glass, the gas's puffs between the two, and how far the arc's sheets reach from the centre. */
    private static final float HALF = 2.3F, OVERLAY_HALF = 2.35F, ARC_HALF = 1.5F;
    private static final int GAS_ALPHA = 0xA0;
    private static final Identifier CLOUD = CyberFarm.id("block/pipe_gas_cloud"), ARC_A = CyberFarm.id("block/energy_conduit_arc_a"),
            ARC_B = CyberFarm.id("block/energy_conduit_arc_b");

    /** {@code shown} is a fluid or {@link NetworkPipeBlockEntity#ENERGY}. */
    private record Key(Object shown, int connections) {}

    private record FluidPart(List<BakedQuad> quads, Material.Baked particle) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particle;
        }

        @Override
        public int materialFlags() {
            return BakedQuad.FLAG_TRANSLUCENT | BakedQuad.FLAG_ANIMATED;
        }
    }

    private final Map<Key, BlockStateModelPart> parts;

    private PipeFluidModel(BlockStateModel delegate, Map<Key, BlockStateModelPart> parts) {
        super(delegate);
        this.parts = parts;
    }

    public static void wrap(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        // Sprites move on a resource reload, so what is built from them lasts one bake
        Map<Key, BlockStateModelPart> parts = new ConcurrentHashMap<>();
        Map<BlockStateModel, BlockStateModel> wrapped = new IdentityHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof NetworkPipeBlock) {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    models.computeIfPresent(state, (key, model) -> wrapped.computeIfAbsent(model, plain -> new PipeFluidModel(plain, parts)));
                }
            }
        }
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
        super.collectParts(level, pos, state, random, output);
        Object shown = level.getModelData(pos).get(NetworkPipeBlockEntity.SHOWN);
        if (shown != null && state.getBlock() instanceof NetworkPipeBlock) {
            output.add(parts.computeIfAbsent(new Key(shown, connections(state)), key -> build(key, particleMaterial())));
        }
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

    private static BlockStateModelPart build(Key key, Material.Baked pipeParticle) {
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
            return new FluidPart(List.copyOf(quads), pipeParticle);
        }
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        FluidTintSource tint = model.fluidTintSource();
        int rgb = tint != null ? tint.colorAsStack(new FluidStack(fluid, 1000)) : 0xFFFFFF;
        boolean gas = fluid.getFluidType().isLighterThanAir();
        Material.Baked still = model.stillMaterial();
        int light = fluid.getFluidType().getLightLevel();
        Faces faces = new Faces(quads, still.sprite(), ((gas ? GAS_ALPHA : 0xFF) << 24) | (rgb & 0xFFFFFF), light);
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
        return new FluidPart(List.copyOf(quads), still);
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
        private final BakedColors color;
        private final BakedQuad.MaterialInfo material;
        private final Direction.@Nullable Axis along;

        Faces(List<BakedQuad> quads, TextureAtlasSprite sprite, int argb, int light) {
            this(quads, sprite, argb, light, null);
        }

        Faces(List<BakedQuad> quads, TextureAtlasSprite sprite, int argb, int light, Direction.@Nullable Axis along) {
            this.quads = quads;
            this.sprite = sprite;
            this.along = along;
            color = BakedColors.of(argb);
            // Lit at full (the arc) means no shading either
            material = new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.TRANSLUCENT, Sheets.translucentBlockItemSheet(), -1, light < 15, light, false);
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
            quads.add(new BakedQuad(position(corners[0]), position(corners[1]), position(corners[2]), position(corners[3]),
                    uv(face, corners[0]), uv(face, corners[1]), uv(face, corners[2]), uv(face, corners[3]),
                    face, material, BakedNormals.UNSPECIFIED, color));
        }

        private static Vector3f position(float[] px) {
            return new Vector3f(px[0] * PIXEL, px[1] * PIXEL, px[2] * PIXEL);
        }

        private long uv(Direction face, float[] px) {
            if (along != null) {
                // The third axis, neither the pipe's nor the face's: on a level pipe's flank that is the height
                int across = 3 - along.ordinal() - face.getAxis().ordinal();
                return UVPair.pack(sprite.getU(px[along.ordinal()] * PIXEL), sprite.getV(px[across] * PIXEL));
            }
            float u = (face.getAxis() == Direction.Axis.X ? px[2] : px[0]) * PIXEL;
            float v = (face.getAxis() == Direction.Axis.Y ? px[2] : 16 - px[1]) * PIXEL;
            return UVPair.pack(sprite.getU(u), sprite.getV(v));
        }
    }
}
