package com.cyberfarm.client;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.jspecify.annotations.Nullable;

/**
 * A mob in a Methane Extractor's acid falls apart as it dissolves: holes open in its skin, patch by
 * patch, and what is left turns a sickly green. Drawn like the Ender Dragon's death: the dissolve
 * mask first, the further the acid has eaten the fewer of its pixels survive, then the mob's skin
 * only where the mask is left.
 *
 * <p>While it dissolves the mob's usual body is hidden (made invisible for the frame) and a layer
 * added to every living renderer draws it again with the dissolve, posed like the body would be.
 */
public final class DissolvingMobRenderer {
    private static final ResourceLocation MASK = CyberFarm.id("textures/entity/dissolve_mask.png");
    /** Below this the mob is drawn as usual. */
    private static final float VISIBLE_DECAY = 0.02F;

    /** The mob being drawn dissolving right now and how far it is gone; rendering is one thread, one mob at a time. */
    private static @Nullable Mob dissolving;
    private static float decay;

    private DissolvingMobRenderer() {}

    /** How far the acid has eaten a mob standing in a formed chamber; hides its usual body while it draws. */
    public static void pre(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.isInvisible()) {
            return;
        }
        Level level = mob.level();
        // The mob stands on the chamber's floor, two blocks under the extractor
        BlockState top = level.getBlockState(mob.blockPosition().above(2));
        if (!(top.getBlock() instanceof MethaneExtractorBlock) || !top.getValue(MethaneExtractorBlock.FORMED)) {
            return;
        }
        float eaten = MethaneExtractorBlockEntity.decayOf(mob, level.getGameTime());
        if (eaten >= VISIBLE_DECAY) {
            decay = eaten;
            dissolving = mob;
            // Client side only, and undone in post: the usual body is skipped, the layer draws it dissolving
            mob.setInvisible(true);
        }
    }

    public static void post(RenderLivingEvent.Post<?, ?> event) {
        if (dissolving != null && event.getEntity() == dissolving) {
            dissolving.setInvisible(false);
            dissolving = null;
        }
    }

    /** Adds the dissolving layer to every living renderer. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        // The dispatcher holds the new renderers when the event fires; Forge 47.1 has no getter for non-living ones
        for (EntityRenderer<?> renderer : Minecraft.getInstance().getEntityRenderDispatcher().renderers.values()) {
            if (renderer instanceof LivingEntityRenderer living) {
                living.addLayer(new Layer(living));
            }
        }
    }

    private static final class Layer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        private final LivingEntityRenderer<T, M> renderer;

        Layer(LivingEntityRenderer<T, M> renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity != dissolving) {
                return;
            }
            // Mask pixels below this alpha are eaten: never all of it, the acid finishes the mob off itself
            getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.dragonExplosionAlpha(MASK)), light,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.55F * decay);
            getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityDecal(renderer.getTextureLocation(entity))), light,
                    LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F - 0.85F * decay, 1.0F - 0.45F * decay, 1.0F - 0.1F * decay, 1.0F);
        }
    }
}
