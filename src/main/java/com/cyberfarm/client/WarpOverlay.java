package com.cyberfarm.client;

import com.cyberfarm.CyberFarm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.cyberfarm.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;

/**
 * The Taxi drone's warp seen from its hook: a loading screen in the mod's colours, like changing dimension, that
 * covers the jump and the terrain loading in, then fades out on the new place.
 */
public final class WarpOverlay {
    private static final ResourceLocation LAYER = CyberFarm.id("warp");
    /** Milliseconds: the whole screen, its fade in and its fade out. */
    private static final long LENGTH = 1800, FADE_IN = 150, FADE_OUT = 600;
    private static final int BACKGROUND = 0x040B0C, MINT = 0xFF5EC2B8, GLOW = 0xFFD1FEE7, DIM = 0xFF2F6468;
    private static long started = -LENGTH;

    private WarpOverlay() {}

    public static void start() {
        started = Util.getMillis();
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll(LAYER.getPath(), (gui, graphics, partialTick, width, height) -> render(new GuiGraphicsExtractor(graphics)));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        long elapsed = Util.getMillis() - started;
        if (elapsed >= LENGTH) {
            return;
        }
        float alpha = Math.min(Mth.clamp(elapsed / (float) FADE_IN, 0.0F, 1.0F), Mth.clamp((LENGTH - elapsed) / (float) FADE_OUT, 0.0F, 1.0F));
        int a = (int) (alpha * 255) << 24;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, a | BACKGROUND);
        if (alpha < 0.3F) {
            return;
        }
        int textAlpha = (int) (alpha * 255) << 24;
        Component title = Component.translatable("gui.cyberfarm.warp.title");
        Component line = Component.translatable("gui.cyberfarm.warp.loading");
        int cy = height / 2;
        graphics.text(font, title, (width - font.width(title)) / 2, cy - 18, textAlpha | MINT & 0xFFFFFF, false);
        graphics.text(font, line, (width - font.width(line)) / 2, cy - 6, textAlpha | GLOW & 0xFFFFFF, false);
        // A bar sweeping across, as the terrain loads
        int barW = 120, x0 = (width - barW) / 2, y0 = cy + 8;
        graphics.fill(x0, y0, x0 + barW, y0 + 3, textAlpha | DIM & 0xFFFFFF);
        int run = (int) (elapsed % 700 * (barW + 30) / 700) - 30;
        graphics.fill(Math.max(x0, x0 + run), y0, Math.min(x0 + barW, x0 + run + 30), y0 + 3, textAlpha | MINT & 0xFFFFFF);
    }
}
