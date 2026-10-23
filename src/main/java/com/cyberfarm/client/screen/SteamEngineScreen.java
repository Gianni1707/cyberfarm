package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.SteamEngineBlockEntity;
import com.cyberfarm.menu.SteamEngineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * A live side view of the engine: the boiler's coils glowing with its speed, the pistons of every
 * cylinder pumping and the flywheel turning; under it the cylinders, the power made and the speed.
 */
public class SteamEngineScreen extends MachineScreen<SteamEngineMenu> {
    private static final int TANK_X = 136;
    private static final int TOP = 18;
    private static final int HEIGHT = 56;
    private static final int VIEW_X0 = 8;
    private static final int VIEW_X1 = 130;
    private static final int ORANGE = 0xFFFF8C1A;
    private static final int SHELL = 0xFFE3E9ED;
    private static final int STEEL = 0xFF8A96A2;

    /** Flywheel angle, turned every frame by the synced speed. */
    private float angle;
    private float lastTime = -1;

    public SteamEngineScreen(SteamEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 196);
        energyBarY = TOP;
        energyBarHeight = HEIGHT;
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawTank(graphics, x + TANK_X, y + TOP, HEIGHT, menu.getFuelAmount(), SteamEngineBlockEntity.TANK_CAPACITY, menu.getFuelFluid());
        graphics.fill(x + VIEW_X0 - 1, y + TOP - 1, x + VIEW_X1 + 1, y + TOP + HEIGHT + 1, ACCENT_DIM);
        graphics.fill(x + VIEW_X0, y + TOP, x + VIEW_X1, y + TOP + HEIGHT, 0xFF0B1216);
        for (int gx = VIEW_X0 + 10; gx < VIEW_X1; gx += 10) {
            graphics.fill(x + gx, y + TOP, x + gx + 1, y + TOP + HEIGHT, 0xFF101C22);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        int n = menu.getCylinders();
        float speed = menu.getSpeedPercent() / 100.0F;
        float time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() + partialTick : 0;
        if (lastTime >= 0) {
            angle += Math.max(0.0F, time - lastTime) * speed * SteamEngineBlockEntity.TURN_PER_TICK;
        }
        lastTime = time;

        int floor = y + TOP + HEIGHT - 6;
        if (n <= 0) {
            Component line = Component.translatable("gui.cyberfarm.engine.incomplete");
            graphics.text(font, line, x + (VIEW_X0 + VIEW_X1 - font.width(line)) / 2, y + TOP + 18, WARM, false);
            int lineY = y + TOP + 30;
            for (FormattedCharSequence hint : font.split(Component.translatable("gui.cyberfarm.engine.hint"), VIEW_X1 - VIEW_X0 - 8)) {
                graphics.text(font, hint, x + (VIEW_X0 + VIEW_X1 - font.width(hint)) / 2, lineY, TEXT_DIM, false);
                lineY += font.lineHeight;
            }
        } else {
            // Plinth with its light line
            graphics.fill(x + VIEW_X0 + 4, floor, x + VIEW_X1 - 4, floor + 4, PANEL_LIGHT);
            graphics.fill(x + VIEW_X0 + 4, floor + 2, x + VIEW_X1 - 4, floor + 3, ACCENT);
            int step = 14;
            int bx = x + VIEW_X0 + 6;
            // Boiler with its coils, brighter the faster it runs
            graphics.fill(bx, floor - 30, bx + 14, floor, SHELL);
            float pulse = 0.7F + 0.3F * Mth.sin(time * (0.1F + 0.3F * speed));
            int coil = dim(ORANGE, 0.3F + 0.7F * Math.max(speed, 0.1F) * pulse);
            for (int k = 0; k < 3; k++) {
                graphics.fill(bx, floor - 24 + k * 8, bx + 14, floor - 21 + k * 8, coil);
            }
            graphics.fill(bx + 3, floor - 34, bx + 11, floor - 30, STEEL);
            // Steam line over the cylinders
            int cx0 = bx + 14 + 4;
            graphics.fill(bx + 9, floor - 40, cx0 + (n - 1) * step + 10, floor - 38, STEEL);
            graphics.fill(bx + 9, floor - 40, bx + 11, floor - 34, STEEL);
            for (int i = 0; i < n; i++) {
                int cx = cx0 + i * step;
                float a = angle + (i + 1) * Mth.HALF_PI;
                int head = (int) (5 * Mth.sin(a));
                graphics.fill(cx, floor - 16, cx + 12, floor, SHELL);
                graphics.fill(cx, floor - 10, cx + 12, floor - 8, 0xFF2A3138);
                // Glass sleeve with the lit piston head and its rod
                graphics.fill(cx + 2, floor - 32, cx + 10, floor - 16, 0x40A8E4F0);
                graphics.fill(cx + 3, floor - 25 - head, cx + 9, floor - 23 - head, ACCENT);
                graphics.fill(cx + 5, floor - 37, cx + 7, floor - 25 - head, STEEL);
                graphics.fill(cx + 8, floor - 38, cx + 10, floor - 32, STEEL);
            }
            // Dynamo with copper windings and the flywheel on its back
            int dx = cx0 + n * step;
            graphics.fill(dx, floor - 18, dx + 14, floor, SHELL);
            int copper = dim(0xFFE08A45, 0.5F + 0.5F * speed);
            for (int k = 0; k < 3; k++) {
                graphics.fill(dx + 2 + k * 4, floor - 18, dx + 4 + k * 4, floor, copper);
            }
            flywheel(graphics, dx + 24, floor - 16, 12, angle);
        }

        // Cylinders in the window's corner; power and speed under it, then the speed bar
        if (n > 0) {
            graphics.text(font, Component.translatable("gui.cyberfarm.engine.cylinders", n, SteamEngineBlockEntity.MAX_CYLINDERS),
                    x + VIEW_X0 + 3, y + TOP + 2, ACCENT, true);
            graphics.text(font, Component.translatable("gui.cyberfarm.fe_per_tick", String.format("%,d", menu.getOutput())),
                    x + VIEW_X0, y + 78, ACCENT, false);
            Component speedText = Component.translatable("gui.cyberfarm.engine.speed", menu.getSpeedPercent());
            graphics.text(font, speedText, x + VIEW_X1 - font.width(speedText), y + 78, TEXT_DIM, false);
            drawProgressBar(graphics, x + VIEW_X0, y + 89, VIEW_X1 - VIEW_X0, 4, speed, ORANGE);
        }
    }

    /** A spoked wheel drawn pixel by pixel: rim dashes and six spokes, turned by {@code spin}. */
    private static void flywheel(GuiGraphicsExtractor graphics, int cx, int cy, int r, float spin) {
        for (int i = 0; i < 48; i++) {
            float a = spin + Mth.TWO_PI * i / 48;
            int px = cx + Math.round(r * Mth.cos(a));
            int py = cy + Math.round(r * Mth.sin(a));
            graphics.fill(px, py, px + 1, py + 1, i % 4 < 2 ? ACCENT : 0xFF3A444E);
        }
        for (int s = 0; s < 6; s++) {
            float a = spin + s * Mth.TWO_PI / 6;
            for (int d = 2; d < r - 1; d++) {
                int px = cx + Math.round(d * Mth.cos(a));
                int py = cy + Math.round(d * Mth.sin(a));
                graphics.fill(px, py, px + 1, py + 1, STEEL);
            }
        }
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, SHELL);
    }

    private static int dim(int color, float factor) {
        float f = Mth.clamp(factor, 0.0F, 1.0F);
        return (color & 0xFF000000) | ((int) (((color >> 16) & 0xFF) * f) << 16) | ((int) (((color >> 8) & 0xFF) * f) << 8)
                | (int) ((color & 0xFF) * f);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TOP, HEIGHT, menu.getFuelAmount(), SteamEngineBlockEntity.TANK_CAPACITY, menu.getFuelFluid());
    }
}
