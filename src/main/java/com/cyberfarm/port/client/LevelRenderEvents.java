package com.cyberfarm.port.client;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * The 26.x level render hooks (extract, then submit custom geometry) from the one 1.20.1 event:
 * after the translucent blocks, each extract runs, then each submit, then the batch is drawn.
 */
public final class LevelRenderEvents {
    private final Consumer<ExtractLevelRenderStateEvent>[] extractors;
    private final Consumer<SubmitCustomGeometryEvent>[] submitters;

    @SafeVarargs
    public static LevelRenderEvents extract(Consumer<ExtractLevelRenderStateEvent>... extractors) {
        return new LevelRenderEvents(extractors, null);
    }

    @SuppressWarnings("unchecked")
    private LevelRenderEvents(Consumer<ExtractLevelRenderStateEvent>[] extractors, Consumer<SubmitCustomGeometryEvent>[] submitters) {
        this.extractors = extractors;
        this.submitters = submitters != null ? submitters : new Consumer[0];
    }

    @SafeVarargs
    public final LevelRenderEvents submit(Consumer<SubmitCustomGeometryEvent>... submitters) {
        return new LevelRenderEvents(extractors, submitters);
    }

    public void onRender(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || minecraft.level == null) {
            return;
        }
        ScreenProjection.capture(event.getProjectionMatrix(), event.getPoseStack().last().pose(), event.getCamera().getPosition());
        ExtractLevelRenderStateEvent extract = new ExtractLevelRenderStateEvent(minecraft.level);
        for (Consumer<ExtractLevelRenderStateEvent> extractor : extractors) {
            extractor.accept(extract);
        }
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        SubmitCustomGeometryEvent submit = new SubmitCustomGeometryEvent(event.getPoseStack(), new SubmitNodeCollector(buffers), event.getCamera());
        for (Consumer<SubmitCustomGeometryEvent> submitter : submitters) {
            submitter.accept(submit);
        }
        buffers.endBatch();
    }

    public static final class ExtractLevelRenderStateEvent {
        private final ClientLevel level;

        ExtractLevelRenderStateEvent(ClientLevel level) {
            this.level = level;
        }

        public ClientLevel getLevel() {
            return level;
        }
    }

    public static final class SubmitCustomGeometryEvent {
        private final PoseStack poseStack;
        private final SubmitNodeCollector collector;
        private final Camera camera;

        SubmitCustomGeometryEvent(PoseStack poseStack, SubmitNodeCollector collector, Camera camera) {
            this.poseStack = poseStack;
            this.collector = collector;
            this.camera = camera;
        }

        public PoseStack getPoseStack() {
            return poseStack;
        }

        public SubmitNodeCollector getSubmitNodeCollector() {
            return collector;
        }

        public Camera getCamera() {
            return camera;
        }
    }
}
