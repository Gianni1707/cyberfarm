package com.cyberfarm.client.screen;

import java.util.Random;

import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;
import com.cyberfarm.menu.MethaneExtractorMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/**
 * A live view into the chamber: the mob inside standing in the rising acid, bubbles and arcs while
 * it dissolves, how much of it is left, and the acid and methane tanks on either side.
 */
public class MethaneExtractorScreen extends MachineScreen<MethaneExtractorMenu> {
    private static final int ACID_X = 9;
    private static final int TANK_X = 151;
    private static final int TANK_Y = 30;
    private static final int TANK_HEIGHT = 62;
    private static final int VIEW_X0 = 32;
    private static final int VIEW_X1 = 144;
    private static final int VIEW_Y0 = 19;
    private static final int VIEW_Y1 = 93;
    private static final int ACID_COLOR = 0xFF9BE04A;
    private static final int ACID_FILL = 0x709BE04A;
    private static final int ORANGE = 0xFFFF8C1A;

    public MethaneExtractorScreen(MethaneExtractorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 210);
    }

    private @Nullable LivingEntity mob() {
        int id = menu.getMobId();
        Entity entity = id != 0 && minecraft != null && minecraft.level != null ? minecraft.level.getEntity(id) : null;
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.text(font, Component.translatable("gui.cyberfarm.extractor.acid_label"), x + ACID_X - 1, y + 20, ACID_COLOR, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.extractor.methane_label"), x + TANK_X - 2, y + 20, ACCENT, false);
        drawTank(graphics, x + ACID_X, y + TANK_Y, TANK_HEIGHT, menu.getAcidAmount(), MethaneExtractorBlockEntity.TANK_CAPACITY, menu.getAcidFluid());
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getTankAmount(), MethaneExtractorBlockEntity.TANK_CAPACITY, menu.getTankFluid());

        // The window's frame: cut corners, glass edge, dark inside and two lit pillars
        graphics.fill(x + VIEW_X0 + 2, y + VIEW_Y0 - 1, x + VIEW_X1 - 2, y + VIEW_Y1 + 1, OUTLINE);
        graphics.fill(x + VIEW_X0 - 1, y + VIEW_Y0 + 2, x + VIEW_X1 + 1, y + VIEW_Y1 - 2, OUTLINE);
        graphics.fill(x + VIEW_X0 + 1, y + VIEW_Y0, x + VIEW_X1 - 1, y + VIEW_Y1, ACCENT_DIM);
        graphics.fill(x + VIEW_X0, y + VIEW_Y0 + 1, x + VIEW_X1, y + VIEW_Y1 - 1, ACCENT_DIM);
        graphics.fill(x + VIEW_X0 + 1, y + VIEW_Y0 + 1, x + VIEW_X1 - 1, y + VIEW_Y1 - 1, 0xFF0B1216);
        for (int gx = VIEW_X0 + 12; gx < VIEW_X1 - 6; gx += 12) {
            graphics.fill(x + gx, y + VIEW_Y0 + 1, x + gx + 1, y + VIEW_Y1 - 1, 0xFF101C22);
        }
        for (int px : new int[] {VIEW_X0 + 4, VIEW_X1 - 8}) {
            graphics.fill(x + px, y + VIEW_Y0 + 1, x + px + 4, y + VIEW_Y1 - 1, PANEL_LIGHT);
            graphics.fill(x + px + 1, y + VIEW_Y0 + 4, x + px + 3, y + VIEW_Y1 - 4, ORANGE);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        float time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() + partialTick : 0;
        LivingEntity mob = menu.isFormed() ? mob() : null;
        boolean dissolving = mob != null && menu.getAcidAmount() >= MethaneExtractorBlockEntity.ACID_PER_TICK && menu.getRate() > 0;

        if (mob != null) {
            float size = Math.max(mob.getType().getHeight(), mob.getType().getWidth());
            int scale = (int) Math.min(30, 58 / Math.max(0.5F, size));
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x + VIEW_X0 + 10, y + VIEW_Y0 + 4, x + VIEW_X1 - 10, y + VIEW_Y1 - 2,
                    scale, 0.0625F, mouseX, mouseY, mob);
        }

        // Acid over the mob's feet, rising with the tank like the pool in the chamber
        int inner = VIEW_Y1 - VIEW_Y0 - 2;
        int depth = (int) (inner * 0.8F * Math.min(1.0F, (float) menu.getAcidAmount() / MethaneExtractorBlockEntity.TANK_CAPACITY));
        if (depth > 0) {
            int surface = y + VIEW_Y1 - 1 - depth;
            graphics.fill(x + VIEW_X0 + 1, surface, x + VIEW_X1 - 1, y + VIEW_Y1 - 1, ACID_FILL);
            graphics.fill(x + VIEW_X0 + 1, surface, x + VIEW_X1 - 1, surface + 1, 0xFFC8F890);
            for (int i = 0; i < 12; i++) {
                float phase = (time * (0.02F + 0.01F * (i % 3)) + i * 0.37F) % 1.0F;
                int bx = x + VIEW_X0 + 8 + (i * 37) % (VIEW_X1 - VIEW_X0 - 16);
                int by = y + VIEW_Y1 - 3 - (int) (phase * (depth - 3));
                graphics.fill(bx, by, bx + 1 + i % 2, by + 1 + i % 2, 0xC0E4FFB0);
            }
        }
        if (menu.isFilling() && menu.isFormed()) {
            // Acid pouring from the top of the window, splashing where it lands
            int cx = x + (VIEW_X0 + VIEW_X1) / 2;
            int bottom = y + VIEW_Y1 - 1 - Math.max(depth, 0);
            graphics.fill(cx - 2, y + VIEW_Y0 + 1, cx + 2, bottom, 0xA09BE04A);
            for (int i = 0; i < 6; i++) {
                int dy = (int) ((time * 3 + i * 9) % Math.max(1, bottom - y - VIEW_Y0 - 1));
                graphics.fill(cx - 1, y + VIEW_Y0 + 1 + dy, cx + 1, y + VIEW_Y0 + 4 + dy, 0xFFC8F890);
            }
            for (int i = 0; i < 6; i++) {
                float phase = (time * 0.15F + i / 6.0F) % 1.0F;
                int sx = cx + (int) ((i % 2 == 0 ? -1 : 1) * (2 + phase * (6 + i)));
                int sy = bottom - 1 - (int) (4 * Mth.sin(phase * Mth.PI));
                graphics.fill(sx, sy, sx + 1, sy + 1, 0xFFE4FFB0);
            }
        }
        if (dissolving) {
            arcs(graphics, x + (VIEW_X0 + VIEW_X1) / 2, y + VIEW_Y0 + 10, y + VIEW_Y1 - 6, (int) (time / 2));
        }

        // Status in the window's corner, the gas made in the other
        Component status;
        int color;
        if (!menu.isFormed()) {
            status = Component.translatable("gui.cyberfarm.extractor.incomplete");
            color = WARM;
        } else if (menu.isFilling()) {
            status = Component.translatable("gui.cyberfarm.extractor.status.filling");
            color = ACID_COLOR;
        } else if (mob == null) {
            status = Component.translatable("gui.cyberfarm.extractor.status.empty");
            color = TEXT_DIM;
        } else if (!dissolving) {
            status = Component.translatable("gui.cyberfarm.extractor.status.no_acid");
            color = WARM;
        } else {
            status = Component.translatable("gui.cyberfarm.extractor.status.dissolving");
            color = ACID_COLOR;
        }
        graphics.text(font, status, x + VIEW_X0 + 4, y + VIEW_Y0 + 3, color, true);
        if (dissolving) {
            Component rate = Component.translatable("gui.cyberfarm.mb_per_tick", menu.getRate());
            graphics.text(font, rate, x + VIEW_X1 - 4 - font.width(rate), y + VIEW_Y0 + 3, ACCENT, true);
        }

        if (menu.isFormed() && menu.getAcidAmount() <= 0) {
            wrapped(graphics, Component.translatable("gui.cyberfarm.extractor.acid_hint"), x + VIEW_X0 + 4, y + VIEW_Y1 - 21, WARM, true);
        }

        // Under the window: what is inside and how much of it is left
        if (mob != null) {
            float left = mob.getHealth() / Math.max(1.0F, mob.getMaxHealth());
            graphics.text(font, mob.getDisplayName(), x + VIEW_X0, y + 97, TEXT, false);
            Component percent = Component.translatable("gui.cyberfarm.extractor.integrity", Math.round(left * 100));
            graphics.text(font, percent, x + VIEW_X1 - font.width(percent), y + 97, TEXT_DIM, false);
            drawProgressBar(graphics, x + VIEW_X0, y + 107, VIEW_X1 - VIEW_X0, 3, left, left > 0.3F ? ACID_COLOR : WARM);
        } else {
            Component hint = Component.translatable(menu.isFormed() ? "gui.cyberfarm.extractor.lure" : "gui.cyberfarm.extractor.hint");
            wrapped(graphics, hint, x + VIEW_X0, y + 97, TEXT_DIM, false);
        }
    }

    /** Text broken into lines that fit the window's width, one under the other: two at most, the room there is. */
    private void wrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int color, boolean shadow) {
        for (FormattedCharSequence line : font.split(text, VIEW_X1 - VIEW_X0 - 8).stream().limit(2).toList()) {
            graphics.text(font, line, x, y, color, shadow);
            y += font.lineHeight;
        }
    }

    /** Flickering cyan lightning around the dissolving mob. */
    private static void arcs(GuiGraphicsExtractor graphics, int centreX, int top, int bottom, int seed) {
        Random random = new Random(seed);
        for (int arc = 0; arc < 3; arc++) {
            int ax = centreX + random.nextInt(41) - 20;
            int ay = top + random.nextInt(Math.max(1, bottom - top - 12));
            for (int step = 0; step < 6; step++) {
                int nx = Mth.clamp(ax + random.nextInt(7) - 3, centreX - 30, centreX + 30);
                graphics.fill(Math.min(ax, nx), ay, Math.max(ax, nx) + 1, ay + 1, 0xFFB8F6FF);
                graphics.fill(nx, ay, nx + 1, ay + 3, ACCENT);
                ax = nx;
                ay += 3;
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, ACID_X, TANK_Y, TANK_HEIGHT, menu.getAcidAmount(), MethaneExtractorBlockEntity.TANK_CAPACITY, menu.getAcidFluid());
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getTankAmount(), MethaneExtractorBlockEntity.TANK_CAPACITY, menu.getTankFluid());
    }
}
