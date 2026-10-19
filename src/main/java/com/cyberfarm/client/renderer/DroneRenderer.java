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
import net.minecraft.world.item.ItemDisplayContext;

public class DroneRenderer extends MobRenderer<DroneEntity, DroneRenderState, DroneModel> {
    private static final Identifier TEXTURE = CyberFarm.id("textures/entity/drone.png");
    /** Full-bright overlay for the cyan lights, visible in the dark. */
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/drone_glow.png"));

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
                pose.translate(0.0F, 5.0F / 16.0F, 0.0F);
                // Model space is upside down
                pose.mulPose(Axis.XP.rotationDegrees(180.0F));
                state.carried.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
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
        state.carried.clear();
        if (state.has(DroneTask.TRANSPORT)) {
            itemModelResolver.updateForLiving(state.carried, entity.getCarriedItem(), ItemDisplayContext.GROUND, entity);
        }
    }
}
