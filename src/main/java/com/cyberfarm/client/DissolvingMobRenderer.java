package com.cyberfarm.client;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * A mob in a Methane Extractor's acid falls apart as it dissolves: holes open in its skin, patch by
 * patch, and what is left turns a sickly green. Drawn like the Ender Dragon's death, with a dissolve
 * mask: the further the acid has eaten it, the fewer of the mask's pixels survive.
 *
 * <p>While it dissolves the mob's usual body is hidden (as if invisible) and a layer added to every
 * living renderer draws it again with the dissolve, posed and turned like the body would be.
 */
public final class DissolvingMobRenderer {
    private static final ContextKey<Float> DECAY = new ContextKey<>(CyberFarm.id("decay"));
    private static final Identifier MASK = CyberFarm.id("textures/entity/dissolve_mask.png");
    /** Below this the mob is drawn as usual. */
    private static final float VISIBLE_DECAY = 0.02F;

    private DissolvingMobRenderer() {}

    /** Render state modifier: how far the acid has eaten a mob standing in a formed chamber. */
    public static void extract(LivingEntity entity, LivingEntityRenderState state) {
        if (!(entity instanceof Mob mob) || state.isInvisibleToPlayer) {
            return;
        }
        Level level = entity.level();
        // The mob stands on the chamber's floor, two blocks under the extractor
        BlockState top = level.getBlockState(mob.blockPosition().above(2));
        if (!(top.getBlock() instanceof MethaneExtractorBlock) || !top.getValue(MethaneExtractorBlock.FORMED)) {
            return;
        }
        float decay = MethaneExtractorBlockEntity.decayOf(mob, level.getGameTime());
        if (decay >= VISIBLE_DECAY) {
            state.setRenderData(DECAY, decay);
            // The usual body is skipped; the layer below draws it dissolving
            state.isInvisible = true;
            state.isInvisibleToPlayer = true;
        }
    }

    /** Adds the dissolving layer to every living renderer. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer(type);
            if (renderer instanceof LivingEntityRenderer living) {
                living.addLayer(new Layer(living));
            }
        }
    }

    private static final class Layer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
        private final LivingEntityRenderer<?, S, M> renderer;

        Layer(LivingEntityRenderer<?, S, M> renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
            Float decay = state.getRenderData(DECAY);
            if (decay == null) {
                return;
            }
            // Alpha is how much of the mask survives: never all of it gone, the acid finishes the mob off itself
            int color = ARGB.colorFromFloat(1.0F - 0.85F * decay, 1.0F - 0.45F * decay, 1.0F - 0.1F * decay, 1.0F - 0.55F * decay);
            collector.submitModel(getParentModel(), state, poseStack,
                    RenderTypes.entityCutoutDissolve(renderer.getTextureLocation(state), MASK), light,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F), color, null, state.outlineColor, null);
        }
    }
}
