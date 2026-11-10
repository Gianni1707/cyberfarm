package com.cyberfarm.client.renderer;

import com.cyberfarm.port.client.SubmitNodeCollector;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.model.DroneModel;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.port.client.MobRenderer;
import com.cyberfarm.port.client.RenderTypes;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.cyberfarm.port.client.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class DroneRenderer extends MobRenderer<DroneEntity, DroneRenderState, DroneModel> {
    private static final ResourceLocation TEXTURE = CyberFarm.id("textures/entity/drone.png");
    /** Full-bright overlay for the lights, visible in the dark. */
    private static final ResourceLocation GLOW_TEXTURE = CyberFarm.id("textures/entity/drone_glow.png");
    private static final RenderType GLOW = RenderTypes.eyes(GLOW_TEXTURE);
    /** At run time {@code ModelPart.Cube.polygons}, {@code Polygon.vertices}, {@code Vertex.u} and {@code Vertex.v}; Polygon and Vertex are not public here. */
    private static final String CUBE_POLYGONS = "f_104341_", POLYGON_VERTICES = "f_104359_", VERTEX_U = "f_104372_", VERTEX_V = "f_104373_";
    private static final int IDLE_LAMP = 0x2B3A3A;
    /** The load's middle, in pixels under the hull, level with the jaws' toes and clear of the claw's housing above. */
    private static final float CARRIED_Y = 4.4F;
    /** In the Tank Module's window, in pixels (y down): the floor of the liquid, and how tall it stands when full. */
    private static final float TANK_FLOOR = 2.43F, TANK_HEIGHT = 2.6F;

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context, new DroneModel(context.bakeLayer(DroneModel.LAYER)), 0.4F);
        // The lights are a few faces: a copy of the model with only those, so the glow pass skips the rest
        DroneModel glowModel = new DroneModel(context.bakeLayer(DroneModel.LAYER));
        keepLitFaces(glowModel.root(), GLOW_TEXTURE);
        addLayer(new EyesLayer<DroneEntity, EntityModel<DroneEntity>>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, DroneEntity drone, float limbSwing, float limbSwingAmount,
                    float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                glowModel.setupAnim(state());
                glowModel.renderToBuffer(pose, buffers.getBuffer(GLOW), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }

            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
        // The cargo item, held between the claw's tines
        addLayer(new RenderLayer<DroneEntity, EntityModel<DroneEntity>>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, DroneEntity drone, float limbSwing, float limbSwingAmount,
                    float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                ItemStack carried = state().carried;
                if (carried.isEmpty()) {
                    return;
                }
                pose.pushPose();
                model().translateToClaw(pose);
                // Vanilla shows a block on the ground 4 pixels wide and 3 up from its origin, a flat item 8 wide and 2 up:
                // both are brought to the same small size, with their middle level with the jaws' toes
                boolean block = Minecraft.getInstance().getItemRenderer().getModel(carried, drone.level(), null, drone.getId()).usesBlockLight();
                float size = 2.0F * DroneModel.CARRIED_HALF_WIDTH / (block ? 4.0F : 8.0F);
                pose.translate(0.0F, (CARRIED_Y + (block ? 3.0F : 2.0F) * size) / 16.0F, 0.0F);
                // Model space is upside down
                pose.mulPose(Axis.XP.rotationDegrees(180.0F));
                pose.scale(size, size, size);
                Minecraft.getInstance().getItemRenderer().renderStatic(drone, carried, ItemDisplayContext.GROUND, false, pose, buffers,
                        drone.level(), light, OverlayTexture.NO_OVERLAY, drone.getId());
                pose.popPose();
            }
        });
        // The liquid in the Tank Module's window, up to its real level, with a lighter surface
        addLayer(new RenderLayer<DroneEntity, EntityModel<DroneEntity>>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, DroneEntity drone, float limbSwing, float limbSwingAmount,
                    float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                DroneRenderState state = state();
                SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
                float height = TANK_HEIGHT * state.liquidFill;
                if (!state.has(DroneTask.TANK) || height < 0.02F) {
                    return;
                }
                pose.pushPose();
                model().translateToTank(pose);
                pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                int shown = state.liquidGlows ? LightCoordsUtil.FULL_BRIGHT : light;
                int colour = 0xFF000000 | state.liquidColour;
                int surface = 0xFF000000 | ModelParts.towardsWhite(state.liquidColour, 0.3F);
                float top = TANK_FLOOR - height;
                collector.submitCustomGeometry(pose, ModelParts.SOLID, (matrix, buffer) -> {
                    ModelParts.box(matrix, buffer, -4.98F, top, -1.38F, -3.52F, top + Math.min(0.2F, height), 1.38F, surface, surface, shown);
                    if (height > 0.2F) {
                        ModelParts.box(matrix, buffer, -4.98F, top + 0.2F, -1.38F, -3.52F, TANK_FLOOR, 1.38F, colour, colour, shown);
                    }
                });
                pose.popPose();
            }
        });
        // The Sponge Module's rings climbing the tube and its gauge lamp, in the colour of the liquid
        addLayer(new RenderLayer<DroneEntity, EntityModel<DroneEntity>>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, DroneEntity drone, float limbSwing, float limbSwingAmount,
                    float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                DroneRenderState state = state();
                SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
                if (!state.has(DroneTask.SPONGE)) {
                    return;
                }
                pose.pushPose();
                model().translateToSponge(pose);
                pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                float reach = DroneModel.spongeReach(state);
                collector.submitCustomGeometry(pose, ModelParts.SOLID, (matrix, buffer) -> {
                    int lamp = 0xFF000000 | (state.liquidFill > 0.0F ? state.liquidColour : IDLE_LAMP);
                    float half = 0.4F + 0.6F * state.liquidFill;
                    ModelParts.box(matrix, buffer, -half, 1.1F, -1.95F, half, 1.9F, -1.4F, lamp, lamp, LightCoordsUtil.FULL_BRIGHT);
                    if (reach > 3.0F) {
                        int ring = 0xFF000000 | state.liquidColour;
                        for (int i = 0; i < 2; i++) {
                            float climbed = (state.ageInTicks * 0.05F + i * 0.5F) % 1.0F;
                            float y = 2.0F + reach * (1.0F - climbed) - 0.5F;
                            float size = 1.4F * Math.max(0.25F, Mth.sin(Mth.PI * climbed));
                            ModelParts.box(matrix, buffer, -size, y, -size, size, y + 1.0F, size, ring, ring, light);
                        }
                    }
                });
                pose.popPose();
            }
        });
    }

    /** Drops every face whose patch of the glow texture is all clear. Renderers are rebuilt on each resource reload, so a new texture is read again. */
    private static void keepLitFaces(ModelPart root, ResourceLocation texture) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(texture); NativeImage image = NativeImage.read(stream)) {
            int[] faces = new int[2];
            root.visit(new PoseStack(), (pose, path, index, cube) -> {
                Field field = ObfuscationReflectionHelper.findField(ModelPart.Cube.class, CUBE_POLYGONS);
                Object[] all = (Object[]) get(field, cube);
                Object[] lit = Arrays.stream(all).filter(face -> isLit(face, image)).toArray(length -> (Object[]) Array.newInstance(all.getClass().getComponentType(), length));
                faces[0] += lit.length;
                faces[1] += all.length;
                try {
                    field.set(cube, lit);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            });
            CyberFarm.LOGGER.debug("Drone glow pass: {} of {} faces", faces[0], faces[1]);
        } catch (IOException | RuntimeException e) {
            // Without the texture, or the field, every face stays, as before
            CyberFarm.LOGGER.warn("Could not trim the drone's glow pass, drawing the whole drone in it", e);
        }
    }

    private static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isLit(Object face, NativeImage image) {
        float minU = 1.0F, minV = 1.0F, maxU = 0.0F, maxV = 0.0F;
        Field vertices = ObfuscationReflectionHelper.findField(face.getClass(), POLYGON_VERTICES);
        for (Object vertex : (Object[]) get(vertices, face)) {
            float u = (float) get(ObfuscationReflectionHelper.findField(vertex.getClass(), VERTEX_U), vertex);
            float v = (float) get(ObfuscationReflectionHelper.findField(vertex.getClass(), VERTEX_V), vertex);
            minU = Math.min(minU, u);
            maxU = Math.max(maxU, u);
            minV = Math.min(minV, v);
            maxV = Math.max(maxV, v);
        }
        int x0 = Mth.clamp(Mth.floor(minU * image.getWidth()), 0, image.getWidth() - 1);
        int x1 = Mth.clamp(Mth.ceil(maxU * image.getWidth()), x0 + 1, image.getWidth());
        int y0 = Mth.clamp(Mth.floor(minV * image.getHeight()), 0, image.getHeight() - 1);
        int y1 = Mth.clamp(Mth.ceil(maxV * image.getHeight()), y0 + 1, image.getHeight());
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (image.getPixelRGBA(x, y) >>> 24 != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(DroneRenderState state) {
        return TEXTURE;
    }

    @Override
    public DroneRenderState createRenderState() {
        return new DroneRenderState();
    }

    @Override
    public void extractRenderState(DroneEntity entity, DroneRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.taskMask = entity.getTaskMask();
        state.startup = entity.startupTicks(partialTicks);
        state.shutdown = entity.shutdownTicks(partialTicks);
        state.stow = entity.stow(partialTicks);
        if (state.has(DroneTask.SPONGE) || state.has(DroneTask.TANK)) {
            state.hood = entity.hood(partialTicks);
            state.tankWork = entity.tankWork(partialTicks);
            state.liquidFill = (float) entity.getLiquidAmount() / DroneEntity.TANK_MODULE_CAPACITY;
            // Holding nothing yet, the rings show what it is about to suck up
            Fluid liquid = entity.getLiquidShown();
            for (int down = 1; down <= 2 && liquid == Fluids.EMPTY; down++) {
                liquid = entity.level().getFluidState(entity.blockPosition().below(down)).getType();
            }
            state.liquidColour = ModelParts.liquidColour(entity.level(), liquid);
            state.liquidGlows = liquid.getFluidType().getLightLevel() > 0;
        }
        state.carried = state.has(DroneTask.TRANSPORT) || state.has(DroneTask.BUILD) ? entity.getCarriedItem() : ItemStack.EMPTY;
    }
}
