package com.cyberfarm.client.renderer;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.model.MechModel;
import com.cyberfarm.entity.MechEntity;
import com.cyberfarm.entity.MechModule;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class MechRenderer extends MobRenderer<MechEntity, MechRenderState, MechModel> {
    private static final Identifier TEXTURE = CyberFarm.id("textures/entity/mech.png");
    /** Full-bright overlay for the visor, the cockpit and the thrusters. */
    private static final RenderType GLOW = RenderTypes.eyes(CyberFarm.id("textures/entity/mech_glow.png"));

    public MechRenderer(EntityRendererProvider.Context context) {
        super(context, new MechModel(context.bakeLayer(MechModel.LAYER)), 1.1F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    /** Folding up, the last part of the move packs the whole mech down into its frame, on the ground. */
    @Override
    protected void scale(MechRenderState state, PoseStack poseStack) {
        float pack = state.fold <= 0.6F ? 0.0F : (state.fold - 0.6F) / 0.4F;
        if (pack > 0.0F) {
            float size = 1.0F - 0.88F * pack * pack * (3.0F - 2.0F * pack);
            // Model space is upside down here: the feet are 1.5 blocks below its origin, at +1.5
            poseStack.translate(0.0F, 1.5F, 0.0F);
            poseStack.scale(size, size, size);
            poseStack.translate(0.0F, -1.5F, 0.0F);
        }
    }

    @Override
    public Identifier getTextureLocation(MechRenderState state) {
        return TEXTURE;
    }

    @Override
    public MechRenderState createRenderState() {
        return new MechRenderState();
    }

    @Override
    public void extractRenderState(MechEntity entity, MechRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.open = entity.openAmount(partialTicks);
        state.disabled = entity.isDisabled();
        state.crouch = entity.crouchAmount(partialTicks);
        state.fold = entity.foldAmount(partialTicks);
        state.jet = entity.deployAmount(MechModule.JET, partialTicks);
        state.drill = entity.deployAmount(MechModule.DRILL, partialTicks);
        state.blade = entity.deployAmount(MechModule.BLADE, partialTicks);
        state.armor = entity.deployAmount(MechModule.ARMOR, partialTicks);
        state.drillSpin = entity.isToolOn(MechModule.DRILL) ? (entity.tickCount + partialTicks) * 0.9F : 0.0F;
        Minecraft minecraft = Minecraft.getInstance();
        state.firstPerson = minecraft.player != null && minecraft.player.getVehicle() == entity
                && minecraft.options.getCameraType().isFirstPerson() && entity.isPilotInside(entity.stateProgress(partialTicks));
        state.punch = entity.getFirstPassenger() instanceof Player pilot ? pilot.getAttackAnim(partialTicks) : 0.0F;
    }
}
