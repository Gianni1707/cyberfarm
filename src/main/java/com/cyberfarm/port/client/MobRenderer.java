package com.cyberfarm.port.client;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * A 26.x mob renderer (extract a state, pose the model from it) on the 1.20.1 one. One state per
 * renderer, refilled for each mob it draws.
 */
public abstract class MobRenderer<E extends Mob, S extends LivingEntityRenderState, M extends EntityModel<S>>
        extends net.minecraft.client.renderer.entity.MobRenderer<E, net.minecraft.client.model.EntityModel<E>> {
    private final M portModel;
    private final float baseShadowRadius;
    private @Nullable S state;

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected MobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        // The model is an EntityModel<Entity>, which poses any entity; 1.20.1 only wants it typed as E's
        super(context, (net.minecraft.client.model.EntityModel) model, shadowRadius);
        this.portModel = model;
        this.baseShadowRadius = shadowRadius;
    }

    public abstract S createRenderState();

    public abstract ResourceLocation getTextureLocation(S state);

    public void extractRenderState(E entity, S state, float partialTicks) {
        state.scale = entity.getScale();
    }

    /** Extra transforms in model space, as in 26.x. */
    protected void scale(S state, PoseStack poseStack) {}

    public M model() {
        return portModel;
    }

    /** The state of the mob being drawn now. */
    protected final S state() {
        S current = state;
        if (current == null) {
            current = state = createRenderState();
        }
        return current;
    }

    @Override
    public void render(E entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        S current = state();
        extractRenderState(entity, current, partialTicks);
        current.lightCoords = light;
        portModel.state = current;
        // 26.x shrinks the shadow with the mob's scale
        shadowRadius = baseShadowRadius * current.scale;
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
    }

    @Override
    protected final void scale(E entity, PoseStack poseStack, float partialTicks) {
        S current = state();
        if (current.scale != 1.0F) {
            poseStack.scale(current.scale, current.scale, current.scale);
        }
        scale(current, poseStack);
    }

    @Override
    public final ResourceLocation getTextureLocation(E entity) {
        return getTextureLocation(state());
    }
}
