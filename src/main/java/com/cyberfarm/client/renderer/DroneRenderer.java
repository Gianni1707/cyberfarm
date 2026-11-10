package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.model.DroneModel;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.entity.DroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class DroneRenderer extends MobRenderer<DroneEntity, DroneRenderState, DroneModel> {
    private static final Identifier TEXTURE = CyberFarm.id("textures/entity/drone.png");
    /** Full-bright overlay for the lights, visible in the dark. */
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/drone_glow.png"));
    private static final int IDLE_LAMP = 0x2B3A3A;
    /** The load's middle, in pixels under the hull, level with the jaws' toes and clear of the claw's housing above. */
    private static final float CARRIED_Y = 4.4F;
    /** In the Tank Module's window, in pixels (y down): the floor of the liquid, and how tall it stands when full. */
    private static final float TANK_FLOOR = 2.43F, TANK_HEIGHT = 2.6F;

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context, new DroneModel(context.bakeLayer(DroneModel.LAYER)), 0.4F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
        // The cargo item, held between the claw's tines
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack pose, SubmitNodeCollector collector, int light, DroneRenderState state, float yRot, float xRot) {
                if (state.carried.isEmpty()) {
                    return;
                }
                pose.pushPose();
                getParentModel().translateToClaw(pose);
                // Vanilla shows a block on the ground 4 pixels wide and 3 up from its origin, a flat item 8 wide and 2 up:
                // both are brought to the same small size, with their middle level with the jaws' toes
                boolean block = state.carried.usesBlockLight();
                float size = 2.0F * DroneModel.CARRIED_HALF_WIDTH / (block ? 4.0F : 8.0F);
                pose.translate(0.0F, (CARRIED_Y + (block ? 3.0F : 2.0F) * size) / 16.0F, 0.0F);
                // Model space is upside down
                pose.mulPose(Axis.XP.rotationDegrees(180.0F));
                pose.scale(size, size, size);
                state.carried.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                pose.popPose();
            }
        });
        // The liquid in the Tank Module's window, up to its real level, with a lighter surface
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack pose, SubmitNodeCollector collector, int light, DroneRenderState state, float yRot, float xRot) {
                float height = TANK_HEIGHT * state.liquidFill;
                if (!state.has(DroneTask.TANK) || height < 0.02F) {
                    return;
                }
                pose.pushPose();
                getParentModel().translateToTank(pose);
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
        addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack pose, SubmitNodeCollector collector, int light, DroneRenderState state, float yRot, float xRot) {
                if (!state.has(DroneTask.SPONGE)) {
                    return;
                }
                pose.pushPose();
                getParentModel().translateToSponge(pose);
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

    @Override
    public Identifier getTextureLocation(DroneRenderState state) {
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
        state.carried.clear();
        if (state.has(DroneTask.TRANSPORT) || state.has(DroneTask.BUILD)) {
            itemModelResolver.updateForLiving(state.carried, entity.getCarriedItem(), ItemDisplayContext.GROUND, entity);
        }
    }
}
