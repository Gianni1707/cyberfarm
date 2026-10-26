package com.cyberfarm.client.screen;

import com.cyberfarm.menu.MachineMenu;

import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Base screen for CyberFarm machines. The whole GUI is drawn with flat fills in the mod's
 * clean sci-fi palette, so no GUI textures are needed. It moves a little, like the mech's HUD: the
 * panel powers on with a sweep of light when it opens, a glint runs along the header line, the slot
 * under the mouse lights up, the energy bar shimmers and turns amber then red as it empties, and
 * progress bars pulse with a bright leading edge.
 */
public abstract class MachineScreen<T extends MachineMenu> extends AbstractContainerScreen<T> {
    public static final int OUTLINE = 0xFF0B0E11;
    public static final int PANEL = 0xFF252B32;
    public static final int PANEL_LIGHT = 0xFF3A434D;
    public static final int PANEL_DARK = 0xFF1A1F24;
    public static final int SLOT_BORDER = 0xFF0A0D10;
    public static final int SLOT_BG = 0xFF14181D;
    public static final int ACCENT = 0xFF2FE0FF;
    public static final int ACCENT_DIM = 0xFF0E7C99;
    public static final int WARM = 0xFFFFB347;
    public static final int TEXT = 0xFFC9D3DC;
    public static final int TEXT_DIM = 0xFF7D8A96;
    public static final int DANGER = 0xFFFF5040;
    /** How long the power-on sweep takes when the screen opens, and how often the header glint runs, in ms. */
    private static final long POWER_ON_MS = 800;
    private static final long GLINT_MS = 4500;

    protected static final int ENERGY_BAR_WIDTH = 10;

    protected int energyBarX = 158;
    protected int energyBarY = 17;
    protected int energyBarHeight = 52;
    /** False for settings-only GUIs that have no player inventory. */
    protected boolean showInventory = true;
    private final long openedAt = Util.getMillis();

    protected MachineScreen(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;

        // Panel with a light top bevel and a dark bottom edge
        graphics.fill(x, y, x + imageWidth, y + imageHeight, OUTLINE);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, PANEL_LIGHT);
        graphics.fill(x + 1, y + imageHeight - 2, x + imageWidth - 1, y + imageHeight - 1, PANEL_DARK);
        // Cyan header line under the title, with a glint running along it, and a divider above the player inventory
        graphics.fill(x + 6, y + 15, x + imageWidth - 6, y + 16, ACCENT_DIM);
        glint(graphics, x + 6, y + 15, imageWidth - 12, (Util.getMillis() - openedAt) % GLINT_MS / (float) GLINT_MS);
        if (showInventory) {
            graphics.fill(x + 6, y + inventoryLabelY - 3, x + imageWidth - 6, y + inventoryLabelY - 2, PANEL_LIGHT);
        }

        for (Slot slot : menu.slots) {
            drawSlot(graphics, x + slot.x, y + slot.y);
            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                // The slot under the mouse lights up
                graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, ACCENT);
                graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT_BG);
                graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0x302FE0FF);
            }
        }
        // Machines that use no power (Bio-Digester, Methane Extractor) have no energy bar
        if (menu.getEnergyCapacity() > 0) {
            drawEnergyBar(graphics, x + energyBarX, y + energyBarY, energyBarHeight);
        }
        extractMachineBackground(graphics, x, y);
        powerOn(graphics, x, y);
    }

    /** A short bright dash with fading tails, {@code t} (0 to 1) of the way along a one-pixel line. */
    private static void glint(GuiGraphicsExtractor graphics, int x, int y, int width, float t) {
        int middle = x + Math.round(width * t);
        for (int i = -8; i <= 8; i++) {
            int px = middle + i;
            if (px >= x && px < x + width) {
                int alpha = 0xFF * (9 - Math.abs(i)) / 9;
                graphics.fill(px, y, px + 1, y + 1, alpha << 24 | (ACCENT & 0xFFFFFF));
            }
        }
    }

    /** As the screen opens, a line of light sweeps down the panel, everything below it still dark. */
    private void powerOn(GuiGraphicsExtractor graphics, int x, int y) {
        long elapsed = Util.getMillis() - openedAt;
        if (elapsed >= POWER_ON_MS) {
            return;
        }
        // Eased, so it slows as it reaches the bottom
        float t = (float) elapsed / POWER_ON_MS;
        int line = y + Math.round(imageHeight * (1.0F - (1.0F - t) * (1.0F - t)));
        graphics.fill(x + 1, line, x + imageWidth - 1, y + imageHeight - 1, 0xE0101418);
        graphics.fill(x + 1, Math.max(y + 1, line - 2), x + imageWidth - 1, line, ACCENT);
    }

    /** Machine-specific widgets drawn above the panel. {@code x}/{@code y} are the GUI origin. */
    protected abstract void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y);

    /** A flat button: outlined, filled with {@code fill}, its label centred. */
    protected void drawButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height, Component label, int fill, int textColor) {
        graphics.fill(x, y, x + width, y + height, SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.text(font, label, x + (width - font.width(label)) / 2, y + 3, textColor, false);
    }

    protected static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BORDER);
        graphics.fill(x, y, x + 16, y + 16, SLOT_BG);
        graphics.fill(x, y + 16, x + 17, y + 17, PANEL_LIGHT);
        graphics.fill(x + 16, y, x + 17, y + 17, PANEL_LIGHT);
    }

    protected void drawEnergyBar(GuiGraphicsExtractor graphics, int x, int y, int height) {
        graphics.fill(x - 1, y - 1, x + ENERGY_BAR_WIDTH + 1, y + height + 1, SLOT_BORDER);
        graphics.fill(x, y, x + ENERGY_BAR_WIDTH, y + height, SLOT_BG);
        int capacity = menu.getEnergyCapacity();
        int filled = capacity <= 0 ? 0 : (int) ((long) height * Math.min(menu.getEnergy(), capacity) / capacity);
        if (filled > 0) {
            // Cyan while there is plenty, amber when it runs low, red when nearly empty
            float charge = (float) filled / height;
            int core = charge > 0.5F ? ACCENT : charge > 0.2F ? WARM : DANGER;
            int edge = charge > 0.5F ? ACCENT_DIM : (core & 0x00FFFFFF) | 0x80000000;
            graphics.fill(x, y + height - filled, x + ENERGY_BAR_WIDTH, y + height, edge);
            graphics.fill(x + 2, y + height - filled, x + ENERGY_BAR_WIDTH - 2, y + height, core);
            // A shimmer rising through the charge
            int band = y + height - Math.round(filled * (Util.getMillis() % 2500 / 2500.0F));
            graphics.fill(x + 2, Math.max(y + height - filled, band - 3), x + ENERGY_BAR_WIDTH - 2, band, 0x60FFFFFF);
        }
        // Scale ticks
        for (int i = 1; i < 4; i++) {
            int tickY = y + height * i / 4;
            graphics.fill(x, tickY, x + 3, tickY + 1, PANEL_LIGHT);
        }
    }

    protected static final int TANK_WIDTH = 16;
    private static final int[] TANK_COLORS = {0, 0xFF6FBF3F, 0xFF7FD4E8, 0xFF8090A0, 0xFFE4EAEE, 0xFF3F6FE4, 0xFF1FB8A8, 0xFFA8E8FF, 0xFF9BE04A};
    private static final String[] TANK_NAMES = {"gui.cyberfarm.tank.empty", "fluid.cyberfarm.biogas", "fluid.cyberfarm.methane", "gui.cyberfarm.tank.other",
            "fluid.cyberfarm.steam", "block.minecraft.water", "fluid.cyberfarm.coolant", "fluid.cyberfarm.cryo_coolant",
            "fluid.cyberfarm.acid"};

    /** Fluid gauge; {@code fluid} is the code sent by {@code GasTank#fluidCode}. */
    protected static void drawTank(GuiGraphicsExtractor graphics, int x, int y, int height, int amount, int capacity, int fluid) {
        graphics.fill(x - 1, y - 1, x + TANK_WIDTH + 1, y + height + 1, SLOT_BORDER);
        graphics.fill(x, y, x + TANK_WIDTH, y + height, SLOT_BG);
        int filled = capacity <= 0 ? 0 : (int) ((long) height * Math.min(amount, capacity) / capacity);
        if (filled > 0 && fluid > 0 && fluid < TANK_COLORS.length) {
            graphics.fill(x, y + height - filled, x + TANK_WIDTH, y + height, TANK_COLORS[fluid]);
            graphics.fill(x + 2, y + height - filled, x + 4, y + height, 0x40FFFFFF);
        }
        for (int i = 1; i < 4; i++) {
            int tickY = y + height * i / 4;
            graphics.fill(x, tickY, x + 4, tickY + 1, PANEL_LIGHT);
        }
    }

    protected void tankTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int height, int amount, int capacity, int fluid) {
        if (isHovering(x - 1, y - 1, TANK_WIDTH + 2, height + 2, mouseX, mouseY)) {
            Component name = Component.translatable(TANK_NAMES[Math.max(0, Math.min(fluid, TANK_NAMES.length - 1))]);
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.tank",
                    name, String.format("%,d", amount), String.format("%,d", capacity)), mouseX, mouseY);
        }
    }

    /**
     * Draws a horizontal progress bar ({@code progress} from 0 to 1), with a light pulse running along
     * the filled part and its leading edge lit.
     */
    protected static void drawProgressBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float progress, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, SLOT_BORDER);
        graphics.fill(x, y, x + width, y + height, SLOT_BG);
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, progress)));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + height, color);
            int pulse = x + Math.round(filled * (Util.getMillis() % 2000 / 2000.0F));
            graphics.fill(Math.max(x, pulse - 4), y, pulse, y + height, 0x50FFFFFF);
            if (filled < width) {
                graphics.fill(x + filled - 1, y, x + filled, y + height, 0xC0FFFFFF);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, ACCENT, false);
        if (showInventory) {
            graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (menu.getEnergyCapacity() > 0 && isHovering(energyBarX - 1, energyBarY - 1, ENERGY_BAR_WIDTH + 2, energyBarHeight + 2, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.energy",
                    String.format("%,d", menu.getEnergy()), String.format("%,d", menu.getEnergyCapacity())), mouseX, mouseY);
        }
    }
}
