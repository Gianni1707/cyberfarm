package com.cyberfarm.port.client;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * A 26.x model, posed from a render state, on the 1.20.1 entity model. {@link MobRenderer} hands it
 * the state before each render; the 1.20.1 setupAnim fills in the walk and head angles and calls
 * {@link #setupAnim(LivingEntityRenderState)}, which starts from the baked pose like 26.x does.
 */
public abstract class EntityModel<S extends LivingEntityRenderState> extends net.minecraft.client.model.EntityModel<Entity> {
    protected final ModelPart root;
    private final ModelPart[] parts;
    @Nullable S state;

    protected EntityModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        super(renderType);
        this.root = root;
        this.parts = root.getAllParts().toArray(ModelPart[]::new);
    }

    public ModelPart root() {
        return root;
    }

    public void setupAnim(S state) {
        for (ModelPart part : parts) {
            part.resetPose();
        }
    }

    @Override
    public final void setupAnim(Entity entity, float walkPos, float walkSpeed, float ageInTicks, float yRot, float xRot) {
        S current = state;
        if (current == null) {
            return;
        }
        current.walkAnimationPos = walkPos;
        current.walkAnimationSpeed = walkSpeed;
        current.ageInTicks = ageInTicks;
        current.yRot = yRot;
        current.xRot = xRot;
        setupAnim(current);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    }
}
