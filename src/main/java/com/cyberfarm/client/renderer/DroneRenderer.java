package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.model.DroneModel;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.port.client.MobRenderer;
import com.cyberfarm.port.client.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DroneRenderer extends MobRenderer<DroneEntity, DroneRenderState, DroneModel> {
    private static final ResourceLocation TEXTURE = CyberFarm.id("textures/entity/drone.png");
    /** Full-bright overlay for the lights, visible in the dark. */
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/drone_glow.png"));

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context, new DroneModel(context.bakeLayer(DroneModel.LAYER)), 0.4F);
        addLayer(new EyesLayer<DroneEntity, EntityModel<DroneEntity>>(this) {
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
                pose.translate(0.0F, 6.0F / 16.0F, 0.0F);
                // Model space is upside down
                pose.mulPose(Axis.XP.rotationDegrees(180.0F));
                Minecraft.getInstance().getItemRenderer().renderStatic(drone, carried, ItemDisplayContext.GROUND, false, pose, buffers,
                        drone.level(), light, OverlayTexture.NO_OVERLAY, drone.getId());
                pose.popPose();
            }
        });
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
        state.carried = state.has(DroneTask.TRANSPORT) || state.has(DroneTask.BUILD) ? entity.getCarriedItem() : ItemStack.EMPTY;
    }
}
