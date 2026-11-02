package com.cyberfarm.port.client;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The 26.x GUI drawing calls on a 1.20.1 GuiGraphics. A tooltip asked for during the frame is kept
 * and drawn by {@link #flushTooltip} at its end, above everything else, as 26.x does.
 */
public final class GuiGraphicsExtractor {
    private final GuiGraphics graphics;
    private final Pose pose;
    private @Nullable Font tooltipFont;
    private @Nullable List<? extends FormattedCharSequence> tooltip;
    private int tooltipX;
    private int tooltipY;

    public GuiGraphicsExtractor(GuiGraphics graphics) {
        this.graphics = graphics;
        this.pose = new Pose(graphics.pose());
    }

    public GuiGraphics vanilla() {
        return graphics;
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1, y1, color);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int top, int bottom) {
        graphics.fillGradient(x0, y0, x1, y1, top, bottom);
    }

    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y) {
        graphics.renderItemDecorations(font, stack, x, y);
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y, @Nullable String count) {
        graphics.renderItemDecorations(font, stack, x, y, count);
    }

    public Pose pose() {
        return pose;
    }

    public int guiWidth() {
        return graphics.guiWidth();
    }

    public int guiHeight() {
        return graphics.guiHeight();
    }

    public void enableScissor(int x0, int y0, int x1, int y1) {
        graphics.enableScissor(x0, y0, x1, y1);
    }

    public void disableScissor() {
        graphics.disableScissor();
    }

    public void setTooltipForNextFrame(Font font, Component text, int x, int y) {
        setTooltipForNextFrame(font, List.of(text.getVisualOrderText()), x, y);
    }

    public void setTooltipForNextFrame(Font font, List<? extends FormattedCharSequence> lines, int x, int y) {
        tooltipFont = font;
        tooltip = lines;
        tooltipX = x;
        tooltipY = y;
    }

    public void flushTooltip() {
        if (tooltip != null && tooltipFont != null) {
            graphics.renderTooltip(tooltipFont, tooltip, tooltipX, tooltipY);
            tooltip = null;
        }
    }

    /** The 26.x 2D matrix stack, on the 1.20.1 PoseStack. */
    public static final class Pose {
        private final PoseStack stack;

        Pose(PoseStack stack) {
            this.stack = stack;
        }

        public void pushMatrix() {
            stack.pushPose();
        }

        public void popMatrix() {
            stack.popPose();
        }

        public void translate(float x, float y) {
            stack.translate(x, y, 0.0F);
        }

        public void scale(float scale) {
            stack.scale(scale, scale, 1.0F);
        }

        public void scale(float x, float y) {
            stack.scale(x, y, 1.0F);
        }

        public void rotate(float radians) {
            stack.mulPose(com.mojang.math.Axis.ZP.rotation(radians));
        }
    }
}
