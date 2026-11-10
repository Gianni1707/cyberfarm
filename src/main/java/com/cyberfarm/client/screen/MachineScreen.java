package com.cyberfarm.client.screen;

import com.cyberfarm.CyberFarm;
import java.util.List;

import com.cyberfarm.menu.MachineMenu;

import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Base screen for CyberFarm machines: an enamel plate of the machine itself, drawn with flat fills in
 * the machines' own palette, so no GUI textures are needed. Machine slots and gauges are steel-rimmed
 * recesses, while the inventory's slots stay quiet so the items read first. It moves a little, like the
 * mech's HUD: the panel powers on with a sweep of light when it opens, a spark runs along the header
 * line, the slot under the mouse lights up, a band of light climbs the tanks' glass, the energy bar
 * shimmers and turns amber then red as it empties, and progress bars pulse with a bright leading edge.
 */
public abstract class MachineScreen<T extends MachineMenu> extends AbstractContainerScreen<T> {
    public static final int OUTLINE = 0xFF0D1C1C;
    public static final int PANEL = 0xFF1B312F;
    public static final int PANEL_LIGHT = 0xFF2E4B47;
    public static final int PANEL_DARK = 0xFF0D1C1C;
    public static final int PANEL_EDGE = 0xFF4A6762;
    public static final int SLOT_BORDER = 0xFF0D1C1C;
    public static final int SLOT_BG = 0xFF030909;
    public static final int WELL = 0xFF071314;
    public static final int STEEL_LIGHT = 0xFFBDC6C5;
    public static final int STEEL_MID = 0xFFB1BEBC;
    public static final int STEEL_DARK = 0xFF7C8989;
    public static final int STEEL_DIM = 0xFF496973;
    public static final int MINT = 0xFFA5EAD4;
    public static final int MINT_MID = 0xFF5EC2B8;
    public static final int MINT_DARK = 0xFF488D91;
    public static final int GLOW = 0xFFD1FEE7;
    public static final int ACCENT = 0xFF00B894;
    public static final int ACCENT_DIM = 0xFF055B57;
    public static final int WARM = 0xFFFFB347;
    public static final int TEXT = 0xFFD1FEE7;
    public static final int TEXT_DIM = 0xFFB1BEBC;
    public static final int DANGER = 0xFFFF5040;
    /** How long the power-on sweep takes when the screen opens, and how often the header spark runs, in ms. */
    private static final long POWER_ON_MS = 800;
    private static final long GLINT_MS = 4500;

    protected static final int ENERGY_BAR_WIDTH = 10;

    protected int energyBarX = 158;
    protected int energyBarY = 17;
    protected int energyBarHeight = 52;
    /** False for settings-only GUIs that have no player inventory. */
    protected boolean showInventory = true;
    /** A full flat picture of the machine, as big as the GUI, drawn instead of the plate and the slots; {@code tools/guigen.py} makes them. */
    protected Identifier background;
    private final long openedAt = Util.getMillis();
    /** The addon tab on the right edge: 28 x 84, from 8 px below the top of the GUI. */
    private static final int RAIL_WIDTH = 28;
    private static final int RAIL_TOP = 8;
    private static final int RAIL_HEIGHT = 84;
    private static final Identifier ADDON_GHOST = CyberFarm.id("textures/gui/addon_ghost.png");

    protected MachineScreen(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    /** Uses the machine's picture from {@code textures/gui/}: title in its band, a shorter energy gauge under the bolt. */
    protected void flatBackground(String name) {
        background = CyberFarm.id("textures/gui/" + name + ".png");
        energyBarY = 19;
        energyBarHeight = 49;
    }

    @Override
    protected void init() {
        super.init();
        if (background != null) {
            titleLabelX = 6;
            titleLabelY = 4;
            inventoryLabelX = 8;
            inventoryLabelY = imageHeight - 92;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;

        if (background != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, background, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        } else {
            drawPlate(graphics, x, y, imageWidth, imageHeight);
            // Header band under the title, with a spark running along its rule, and a groove above the player inventory
            graphics.fill(x + 6, y + 4, x + imageWidth - 6, y + 15, PANEL_DARK);
            graphics.fill(x + 6, y + 15, x + imageWidth - 6, y + 16, MINT_DARK);
            spark(graphics, x + 6, y + 15, imageWidth - 12, (Util.getMillis() - openedAt) % GLINT_MS / (float) GLINT_MS);
            if (showInventory) {
                graphics.fill(x + 6, y + inventoryLabelY - 3, x + imageWidth - 6, y + inventoryLabelY - 2, PANEL_DARK);
                graphics.fill(x + 6, y + inventoryLabelY - 2, x + imageWidth - 6, y + inventoryLabelY - 1, PANEL_LIGHT);
            }
        }

        if (menu.hasAddons()) {
            drawPlate(graphics, x + imageWidth, y + RAIL_TOP, RAIL_WIDTH, RAIL_HEIGHT);
        }
        for (Slot slot : menu.slots) {
            boolean inventorySlot = slot.container instanceof Inventory;
            if (isAddonSlot(slot)) {
                // Drawn on the tab whatever the background, with a faded chip while empty
                drawSlot(graphics, x + slot.x, y + slot.y);
                if (!slot.hasItem()) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, ADDON_GHOST, x + slot.x, y + slot.y, 0, 0, 16, 16, 16, 16);
                }
            } else if (background == null) {
                if (inventorySlot) {
                    drawInventorySlot(graphics, x + slot.x, y + slot.y);
                } else {
                    drawSlot(graphics, x + slot.x, y + slot.y);
                }
            }
            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                // The slot under the mouse lights up
                graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, ACCENT);
                graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, inventorySlot ? WELL : SLOT_BG);
                graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0x4000B894);
            }
        }
        // Machines that use no power (Bio-Digester, Methane Extractor) have no energy bar
        if (menu.getEnergyCapacity() > 0) {
            drawEnergyBar(graphics, x + energyBarX, y + energyBarY, energyBarHeight);
        }
        extractMachineBackground(graphics, x, y);
        powerOn(graphics, x, y);
    }

    /** The addon tab, in screen coordinates, for JEI and REI to keep clear; empty without one. */
    public List<Rect2i> addonArea() {
        return menu.hasAddons() ? List.of(new Rect2i(leftPos + imageWidth, topPos + RAIL_TOP, RAIL_WIDTH, RAIL_HEIGHT)) : List.of();
    }

    private boolean isAddonSlot(Slot slot) {
        return menu.hasAddons() && slot.x == MachineMenu.ADDON_X && !(slot.container instanceof Inventory);
    }

    /** The addon tab counts as part of the GUI, so a click on it does not throw the carried item out. */
    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        boolean onRail = menu.hasAddons() && mx >= xo + imageWidth && mx < xo + imageWidth + RAIL_WIDTH
                && my >= yo + RAIL_TOP && my < yo + RAIL_TOP + RAIL_HEIGHT;
        return !onRail && super.hasClickedOutside(mx, my, xo, yo);
    }

    /** The enamel plate: a dark rim, a lit edge on the top and left, a shadow along the bottom and four steel rivets. */
    static void drawPlate(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, OUTLINE);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, PANEL_LIGHT);
        graphics.fill(x + 2, y + 2, right - 2, bottom - 2, PANEL);
        graphics.fill(x + 1, y + 1, right - 1, y + 2, PANEL_EDGE);
        graphics.fill(x + 1, y + 1, x + 2, bottom - 1, PANEL_EDGE);
        graphics.fill(x + 2, bottom - 3, right - 2, bottom - 2, PANEL_DARK);
        rivet(graphics, x + 3, y + 3);
        rivet(graphics, right - 5, y + 3);
        rivet(graphics, x + 3, bottom - 6);
        rivet(graphics, right - 5, bottom - 6);
    }

    private static void rivet(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 2, y + 2, STEEL_MID);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, STEEL_DARK);
    }

    /** A short bright dash with fading tails, {@code t} (0 to 1) of the way along a one-pixel line. */
    private static void spark(GuiGraphicsExtractor graphics, int x, int y, int width, float t) {
        int middle = x + Math.round(width * t);
        for (int i = -8; i <= 8; i++) {
            int px = middle + i;
            if (px >= x && px < x + width) {
                int alpha = 0xD8 * (9 - Math.abs(i)) / 9;
                graphics.fill(px, y, px + 1, y + 1, alpha << 24 | ((Math.abs(i) > 3 ? MINT_MID : GLOW) & 0xFFFFFF));
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
        graphics.fill(x + 1, line, x + imageWidth - 1, y + imageHeight - 1, 0xE6030909);
        graphics.fill(x + 1, Math.max(y + 1, line - 2), x + imageWidth - 1, line, ACCENT);
    }

    /** Machine-specific widgets drawn above the panel. {@code x}/{@code y} are the GUI origin. */
    protected abstract void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y);

    /** A small dark plaque for a label on a flat background. */
    protected static void drawTab(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 11, PANEL_DARK);
        graphics.fill(x, y, x + width, y + 10, 0xFF1B2B2B);
        graphics.fill(x, y, x + width, y + 1, 0xFF2E4240);
    }

    /** A short message on its own plaque, centred on {@code centreX}, so it reads on a flat background's bright inlay. */
    protected void drawNotice(GuiGraphicsExtractor graphics, int centreX, int y, Component text, int color) {
        int width = font.width(text) + 8;
        drawTab(graphics, centreX - width / 2, y, width);
        graphics.text(font, text, centreX - width / 2 + 4, y + 2, color, false);
    }

    /** What a slot takes or gives, faded, while it is empty. */
    protected void drawGhost(GuiGraphicsExtractor graphics, int slotIndex, ItemStack stack) {
        Slot slot = menu.slots.get(slotIndex);
        if (!slot.hasItem()) {
            graphics.item(stack, leftPos + slot.x, topPos + slot.y);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0x80071314);
        }
    }

    protected static final int STEEL_TOP = 0xFFE4EAEA;
    protected static final int STEEL_LOW = 0xFF96A3A2;
    protected static final int STEEL_EDGE = 0xFF6A7878;
    protected static final int STEEL_FOOT = 0xFF465353;

    /** A steel bar lying flat, for the moving parts of a process drawing. */
    protected static void drawSteelBar(GuiGraphicsExtractor graphics, int x, int y, int x2, int y2) {
        int third = Math.max(1, (y2 - y) / 3);
        graphics.fill(x - 1, y - 1, x2 + 1, y2 + 1, WELL);
        graphics.fill(x, y, x2, y2, STEEL_LOW);
        graphics.fill(x, y, x2, y + third, STEEL_TOP);
        graphics.fill(x, y + third, x2, y + Math.max(2, (y2 - y) / 2), STEEL_LIGHT);
        graphics.fill(x, y2 - 1, x2, y2, STEEL_FOOT);
    }

    /** A steel rod standing up, lit from the left. */
    protected static void drawSteelRod(GuiGraphicsExtractor graphics, int x, int y, int x2, int y2) {
        graphics.fill(x - 1, y - 1, x2 + 1, y2 + 1, WELL);
        int width = x2 - x;
        for (int i = 0; i < width; i++) {
            float t = (float) i / Math.max(1, width - 1);
            int color = t < 0.2F ? STEEL_LIGHT : t < 0.4F ? STEEL_TOP : t < 0.65F ? STEEL_LIGHT : t < 0.85F ? STEEL_LOW : STEEL_EDGE;
            graphics.fill(x + i, y, x + i + 1, y2, color);
        }
    }

    /** Between two colours, for a liquid changing as the machine works. */
    protected static int mixColor(int from, int to, float t) {
        int r = (int) ((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * t);
        int g = (int) ((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * t);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** The colour a tank shows for a fluid code, or 0 for none. */
    protected static int tankColor(int fluid) {
        return fluid > 0 && fluid < TANK_COLORS.length ? TANK_COLORS[fluid] : 0;
    }

    /** Six blades turning round a hub, for fans and rotors. */
    protected static void drawSpinner(GuiGraphicsExtractor graphics, int cx, int cy, int radius, double angle) {
        for (int blade = 0; blade < 6; blade++) {
            double a = angle + blade * Math.PI / 3;
            for (int r = 2; r <= radius; r++) {
                int px = cx + (int) Math.round(r * Math.cos(a));
                int py = cy + (int) Math.round(r * Math.sin(a));
                graphics.fill(px, py, px + 2, py + 2, r > radius - 2 ? STEEL_LOW : STEEL_LIGHT);
            }
        }
        graphics.fill(cx - 1, cy - 1, cx + 3, cy + 3, WELL);
        graphics.fill(cx, cy, cx + 2, cy + 2, STEEL_TOP);
    }

    /** A few bubbles rising through the given area. */
    protected static void drawBubbles(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        long now = Util.getMillis();
        for (int i = 0; i < 4; i++) {
            float phase = (now + i * 430L) % 1600 / 1600.0F;
            int bx = x + (i * 2 + 1) * width / 8;
            int by = y + height - 2 - Math.round((height - 2) * phase);
            graphics.fill(bx, by, bx + 2, by + 2, color);
        }
    }

    /** A raised button, lit along its top and left; emerald-lit when {@code fill} is the "on" colour, {@link #ACCENT_DIM}. */
    protected void drawButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height, Component label, int fill, int textColor) {
        int lit = fill == ACCENT_DIM ? ACCENT : STEEL_MID;
        graphics.fill(x, y, x + width, y + height, SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, lit);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, lit);
        graphics.text(font, label, x + (width - font.width(label)) / 2, y + (height - 7) / 2, textColor, false);
    }

    /** A machine slot: a steel rim, lit on the lower right, round a deep recess. */
    protected static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, STEEL_DARK);
        graphics.fill(x - 1, y + 16, x + 17, y + 17, STEEL_LIGHT);
        graphics.fill(x + 16, y - 1, x + 17, y + 17, STEEL_LIGHT);
        graphics.fill(x, y, x + 16, y + 16, SLOT_BG);
        graphics.fill(x, y, x + 16, y + 1, PANEL_DARK);
        graphics.fill(x, y, x + 1, y + 16, PANEL_DARK);
    }

    /** Covers a slot the machine can't use right now, marked with a dash. */
    protected static void drawLockedSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 16, y + 16, 0xBF0D1C1C);
        graphics.fill(x + 4, y + 7, x + 12, y + 9, STEEL_DIM);
    }

    /** A steel-rimmed recess around the given area, for views and gauges. */
    protected static void drawWell(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, STEEL_DARK);
        graphics.fill(x - 1, y - 1, x + width + 1, y, STEEL_LIGHT);
        graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, PANEL_DARK);
        graphics.fill(x, y, x + width, y + height, SLOT_BG);
    }

    /** A player inventory slot: quiet, so a full inventory doesn't read as a bright grid. */
    private static void drawInventorySlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, PANEL_DARK);
        graphics.fill(x, y, x + 16, y + 16, WELL);
        graphics.fill(x - 1, y + 16, x + 17, y + 17, STEEL_DIM);
        graphics.fill(x + 16, y - 1, x + 17, y + 17, STEEL_DIM);
    }

    /** A sight glass's frame. Only its sides grow past the gauge, by a 1 px shadow, so nothing above or below can meet text. */
    private static void drawGlass(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x - 2, y - 1, x - 1, y + height + 1, PANEL_DARK);
        graphics.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, PANEL_DARK);
        graphics.fill(x - 1, y - 1, x + width + 1, y, STEEL_LIGHT);
        graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, STEEL_DARK);
        graphics.fill(x - 1, y, x, y + height, STEEL_MID);
        graphics.fill(x + width, y, x + width + 1, y + height, STEEL_DARK);
        graphics.fill(x, y, x + width, y + height, SLOT_BG);
        graphics.fill(x, y, x + width, y + 1, PANEL_DARK);
        graphics.fill(x, y, x + 1, y + height, PANEL_DARK);
    }

    protected void drawEnergyBar(GuiGraphicsExtractor graphics, int x, int y, int height) {
        drawGlass(graphics, x, y, ENERGY_BAR_WIDTH, height);
        int capacity = menu.getEnergyCapacity();
        int filled = capacity <= 0 ? 0 : (int) ((long) height * Math.min(menu.getEnergy(), capacity) / capacity);
        if (filled > 0) {
            // Emerald while there is plenty, amber when it runs low, red when nearly empty
            float charge = (float) filled / height;
            int core = charge > 0.5F ? ACCENT : charge > 0.2F ? WARM : DANGER;
            int edge = charge > 0.5F ? ACCENT_DIM : (core & 0x00FFFFFF) | 0x80000000;
            graphics.fill(x, y + height - filled, x + ENERGY_BAR_WIDTH, y + height, edge);
            graphics.fill(x + 1, y + height - filled, x + ENERGY_BAR_WIDTH - 1, y + height, core);
            // A shimmer rising through the charge
            int band = y + height - Math.round(filled * (Util.getMillis() % 2500 / 2500.0F));
            graphics.fill(x + 1, Math.max(y + height - filled, band - 3), x + ENERGY_BAR_WIDTH - 1, band, 0x38D1FEE7);
        }
    }

    protected static final int TANK_WIDTH = 16;
    private static final int[] TANK_COLORS = {0, 0xFF6FBF3F, 0xFF7FD4E8, 0xFF8090A0, 0xFFE4EAEE, 0xFF3F6FE4, 0xFF1FB8A8, 0xFFA8E8FF, 0xFF9BE04A};
    private static final String[] TANK_NAMES = {"gui.cyberfarm.tank.empty", "fluid.cyberfarm.biogas", "fluid.cyberfarm.methane", "gui.cyberfarm.tank.other",
            "fluid.cyberfarm.steam", "block.minecraft.water", "fluid.cyberfarm.coolant", "fluid.cyberfarm.cryo_coolant",
            "fluid.cyberfarm.acid"};

    /** Fluid gauge; {@code fluid} is the code sent by {@code GasTank#fluidCode}. */
    protected static void drawTank(GuiGraphicsExtractor graphics, int x, int y, int height, int amount, int capacity, int fluid) {
        drawGlass(graphics, x, y, TANK_WIDTH, height);
        int filled = capacity <= 0 ? 0 : (int) ((long) height * Math.min(amount, capacity) / capacity);
        if (filled > 0 && fluid > 0 && fluid < TANK_COLORS.length) {
            // The fluid stays 1 px off the steel: on pale fluids (steam, cryo) that dark gap is what keeps the frame readable
            int top = Math.min(y + height - filled, y + height - 2);
            int bottom = y + height - 1;
            graphics.fill(x + 1, top, x + TANK_WIDTH - 1, bottom, TANK_COLORS[fluid]);
            graphics.fill(x + 2, top, x + 4, bottom, 0x40FFFFFF);
            graphics.fill(x + 1, top, x + TANK_WIDTH - 1, top + 1, 0x99D1FEE7);
            // A soft band of light climbing the glass; the tank's position keeps side-by-side tanks out of step
            int band = y + height - Math.round(filled * ((Util.getMillis() + x * 40L) % 3000 / 3000.0F));
            int bandTop = Math.max(top + 1, band - 3);
            int bandBottom = Math.min(band, bottom);
            if (bandTop < bandBottom) {
                graphics.fill(x + 4, bandTop, x + TANK_WIDTH - 1, bandBottom, 0x29D1FEE7);
            }
        }
        for (int i = 1; i < 4; i++) {
            int tickY = y + height * i / 4;
            // Dark under the fluid, or the ticks vanish on pale ones
            graphics.fill(x + TANK_WIDTH - 5, tickY, x + TANK_WIDTH - 1, tickY + 1, tickY >= y + height - filled ? 0x8C071314 : 0x99A5EAD4);
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
        drawWell(graphics, x, y, width, height);
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, progress)));
        // The bar stays 1 px off the steel, like a tank's fluid: some bars are pale (Electrolyzer, a Dock Tank of steam)
        int left = x + 1;
        int right = Math.min(x + filled, x + width - 1);
        if (right > left) {
            graphics.fill(left, y + 1, right, y + height, color);
            int pulse = x + Math.round(filled * (Util.getMillis() % 2000 / 2000.0F));
            int pulseRight = Math.min(pulse, right);
            if (pulseRight > left) {
                graphics.fill(Math.max(left, pulse - 4), y + 1, pulseRight, y + height, 0x33D1FEE7);
            }
            if (filled < width) {
                graphics.fill(right - 1, y + 1, right, y + height, GLOW);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, MINT, false);
        if (showInventory) {
            graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, background != null ? TEXT : TEXT_DIM, false);
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
