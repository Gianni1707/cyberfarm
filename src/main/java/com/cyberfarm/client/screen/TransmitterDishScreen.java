package com.cyberfarm.client.screen;

import java.util.List;

import com.cyberfarm.energy.DishNetworks;
import com.cyberfarm.energy.DishRenamePayload;
import com.cyberfarm.item.RangeModuleItem;
import com.cyberfarm.menu.TransmitterDishMenu;
import com.cyberfarm.registry.ModItems;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Transmitter Dish (design T1): a heading dial whose octagon rings are the range tiers, lit up to the installed module,
 * with a needle from the little dish to the receiver it serves; beside it the module, the range ladder and the
 * receivers; below, the network's name, colour and visibility.
 */
public class TransmitterDishScreen extends MachineScreen<TransmitterDishMenu> {
    private static final int DIAL_X = 17, DIAL_Y = 16, DIAL_SIZE = 72;
    private static final int LADDER_X = 95, LADDER_Y = 37;
    private static final int LIST_X = 95, LIST_Y = 51;
    private static final int PANEL_Y = 94;
    private static final int NAME_X = 37, NAME_Y = 98, NAME_W = 74;
    private static final int PUBLIC_X = 117, PUBLIC_Y = 96, PUBLIC_W = 50;
    private static final int SWATCH_X = 43, SWATCH_Y = 110, SWATCH_STEP = 12, SWATCH_SIZE = 10;
    private static final int OTHER_DIMENSION = -1;
    private static final int RING_DIM = 0xFF2E4B47;

    private EditBox name;

    public TransmitterDishScreen(TransmitterDishMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 218);
        energyBarX = 5;
        energyBarY = DIAL_Y;
        energyBarHeight = DIAL_SIZE + 4;
        inventoryLabelY = 132;
    }

    @Override
    protected void init() {
        super.init();
        name = new EditBox(font, leftPos + NAME_X + 2, topPos + NAME_Y + 1, NAME_W - 4, 9, Component.translatable("gui.cyberfarm.dish.name"));
        name.setBordered(false);
        name.setMaxLength(DishNetworks.MAX_NAME);
        name.setTextColor(TEXT);
        name.setValue(menu.name());
        addRenderableWidget(name);
    }

    /** Sends the typed name when the player leaves the field or the GUI. */
    private void sendName() {
        String typed = name.getValue().strip();
        if (!typed.isEmpty() && !typed.equals(menu.name())) {
            ClientPacketDistributor.sendToServer(new DishRenamePayload(menu.containerId, typed));
        }
    }

    /** Before the menu closes: the server takes a name only from a player who still has it open. */
    @Override
    public void onClose() {
        sendName();
        super.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (name.isFocused() && !event.isEscape()) {
            if (event.key() == 257 || event.key() == 335) {
                // Enter: done typing
                sendName();
                name.setFocused(false);
                return true;
            }
            return name.keyPressed(event) || name.canConsumeInput() || super.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button() == 0 ? buttonAt(event.x(), event.y()) : -1;
        if (button >= 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        boolean wasFocused = name.isFocused();
        boolean handled = super.mouseClicked(event, doubleClick);
        if (wasFocused && !name.isFocused()) {
            sendName();
        }
        return handled;
    }

    private int buttonAt(double mouseX, double mouseY) {
        for (int i = 0; i < DishNetworks.COLOURS.length; i++) {
            if (isHovering(SWATCH_X + i * SWATCH_STEP, SWATCH_Y, SWATCH_SIZE, SWATCH_SIZE, mouseX, mouseY)) {
                return i;
            }
        }
        if (isHovering(PUBLIC_X, PUBLIC_Y, PUBLIC_W, 12, mouseX, mouseY)) {
            return TransmitterDishMenu.BUTTON_PUBLIC;
        }
        return -1;
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawGhost(graphics, 0, new ItemStack(ModItems.RANGE_MODULE_I.get()));
        drawDial(graphics, x + DIAL_X, y + DIAL_Y);
        // Range ladder: five steps, lit up to the module; the last in violet for Infinity
        int tier = menu.tier();
        for (int i = 0; i <= RangeModuleItem.INFINITY; i++) {
            int h = 3 + i * 2;
            int sx = x + LADDER_X + i * 15;
            int lit = i == RangeModuleItem.INFINITY ? 0xFFB88CFF : MINT_MID;
            graphics.fill(sx, y + LADDER_Y + 11 - h, sx + 13, y + LADDER_Y + 11, PANEL_DARK);
            graphics.fill(sx + 1, y + LADDER_Y + 12 - h, sx + 12, y + LADDER_Y + 11, i <= tier ? lit : RING_DIM);
        }
        // Network panel
        drawWell(graphics, x + 7, y + PANEL_Y, 162, 35);
        graphics.fill(x + NAME_X, y + NAME_Y - 1, x + NAME_X + NAME_W, y + NAME_Y + 10, SLOT_BORDER);
        graphics.fill(x + NAME_X + 1, y + NAME_Y, x + NAME_X + NAME_W - 1, y + NAME_Y + 9, SLOT_BG);
        drawButton(graphics, x + PUBLIC_X, y + PUBLIC_Y, PUBLIC_W, 12,
                Component.translatable(menu.isPublic() ? "gui.cyberfarm.dish.public" : "gui.cyberfarm.dish.private"),
                menu.isPublic() ? ACCENT_DIM : PANEL_LIGHT, TEXT);
        for (int i = 0; i < DishNetworks.COLOURS.length; i++) {
            int sx = x + SWATCH_X + i * SWATCH_STEP, sy = y + SWATCH_Y;
            graphics.fill(sx - 1, sy - 1, sx + SWATCH_SIZE + 1, sy + SWATCH_SIZE + 1, i == menu.colour() ? GLOW : SLOT_BORDER);
            graphics.fill(sx, sy, sx + SWATCH_SIZE, sy + SWATCH_SIZE, 0xFF000000 | DishNetworks.COLOURS[i]);
        }
    }

    /** The dial: range rings, the little dish in the middle, the receivers as dots and a needle to the one it serves. */
    private void drawDial(GuiGraphicsExtractor graphics, int x, int y) {
        drawWell(graphics, x, y, DIAL_SIZE + 4, DIAL_SIZE + 4);
        int cx = x + 2 + DIAL_SIZE / 2, cy = y + 2 + DIAL_SIZE / 2;
        int tier = menu.tier();
        for (int ring = 0; ring <= RangeModuleItem.INFINITY; ring++) {
            octagon(graphics, cx, cy, 8 + ring * 7, ring <= tier ? MINT_MID : RING_DIM);
        }
        graphics.fill(cx - 3, cy - 3, cx + 4, cy + 4, STEEL_DARK);
        graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, MINT_MID);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, GLOW);
        List<String> receivers = menu.receivers();
        for (int i = 0; i < receivers.size(); i++) {
            int ring = ringOf(receivers.get(i));
            boolean served = i == menu.served();
            int colour = served ? GLOW : MINT_DARK;
            if (ring == OTHER_DIMENSION) {
                // In another dimension: an arrow in the notch at the top of the frame
                int ax = cx + (i - receivers.size() / 2) * 6;
                graphics.fill(ax - 2, y + 1, ax + 3, y + 2, 0xFFB88CFF);
                graphics.fill(ax - 1, y, ax + 2, y + 1, 0xFFB88CFF);
                graphics.fill(ax, y - 1, ax + 1, y, 0xFFB88CFF);
                if (served) {
                    needle(graphics, cx, cy, ax, y + 3, menu.transmitting());
                }
                continue;
            }
            double angle = Math.toRadians(225 + i * 90);
            int r = 8 + ring * 7;
            int dx = cx + (int) Math.round(Math.cos(angle) * r), dy = cy + (int) Math.round(Math.sin(angle) * r);
            if (served) {
                needle(graphics, cx, cy, dx, dy, menu.transmitting());
            }
            graphics.fill(dx - 2, dy - 2, dx + 3, dy + 3, SLOT_BORDER);
            graphics.fill(dx - 1, dy - 1, dx + 2, dy + 2, colour);
        }
    }

    /** A dotted line from the dish to the receiver: steady while transmitting, faint while it turns. */
    private static void needle(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, boolean steady) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int s = 4; s < steps - 2; s += 2) {
            int px = x0 + (x1 - x0) * s / steps, py = y0 + (y1 - y0) * s / steps;
            graphics.fill(px, py, px + 1, py + 1, steady ? GLOW : MINT_DARK);
        }
    }

    /** An octagon outline of this half-width, its corners cut a third of the way. */
    private static void octagon(GuiGraphicsExtractor graphics, int cx, int cy, int half, int colour) {
        int cut = half / 3;
        graphics.fill(cx - half + cut, cy - half, cx + half - cut + 1, cy - half + 1, colour);
        graphics.fill(cx - half + cut, cy + half, cx + half - cut + 1, cy + half + 1, colour);
        graphics.fill(cx - half, cy - half + cut, cx - half + 1, cy + half - cut + 1, colour);
        graphics.fill(cx + half, cy - half + cut, cx + half + 1, cy + half - cut + 1, colour);
        for (int k = 1; k < cut; k++) {
            graphics.fill(cx - half + cut - k, cy - half + k, cx - half + cut - k + 1, cy - half + k + 1, colour);
            graphics.fill(cx + half - cut + k, cy - half + k, cx + half - cut + k + 1, cy - half + k + 1, colour);
            graphics.fill(cx - half + cut - k, cy + half - k, cx - half + cut - k + 1, cy + half - k + 1, colour);
            graphics.fill(cx + half - cut + k, cy + half - k, cx + half - cut + k + 1, cy + half - k + 1, colour);
        }
    }

    /** Which range ring a receiver sits on, from its "N blocks" label, or {@link #OTHER_DIMENSION}. */
    private static int ringOf(String label) {
        String digits = label.replaceAll("[^0-9]", "");
        if (!label.endsWith(" blocks") || digits.isEmpty()) {
            return OTHER_DIMENSION;
        }
        int distance = Integer.parseInt(digits);
        for (int tier = 0; tier < RangeModuleItem.INFINITY; tier++) {
            if (distance <= RangeModuleItem.range(tier)) {
                return tier;
            }
        }
        return RangeModuleItem.INFINITY;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int range = RangeModuleItem.range(menu.tier());
        graphics.text(font, Component.translatable("gui.cyberfarm.dish.range"), 117, 18, TEXT_DIM, false);
        graphics.text(font, range < 0 ? Component.translatable("gui.cyberfarm.dish.unlimited")
                : Component.translatable("gui.cyberfarm.dish.blocks", String.format("%,d", range)), 117, 27, TEXT, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.dish.receivers", menu.receiverCount(), DishNetworks.MAX_RECEIVERS), LIST_X, LIST_Y, TEXT_DIM, false);
        List<String> receivers = menu.receivers();
        for (int i = 0; i < DishNetworks.MAX_RECEIVERS; i++) {
            int rowY = LIST_Y + 9 + i * 8;
            boolean served = i == menu.served();
            if (served) {
                graphics.fill(LIST_X - 2, rowY - 1, LIST_X - 1, rowY + 8, GLOW);
            }
            graphics.text(font, Component.literal(String.valueOf(i + 1)), LIST_X + 1, rowY, served ? GLOW : MINT_DARK, false);
            Component row = i < receivers.size() ? Component.literal(receivers.get(i)) : Component.translatable("gui.cyberfarm.dish.free");
            graphics.text(font, row, LIST_X + 9, rowY, i < receivers.size() ? (served ? GLOW : TEXT) : TEXT_DIM, false);
        }
        graphics.text(font, Component.translatable("gui.cyberfarm.dish.name"), 10, NAME_Y + 1, TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.dish.colour"), 10, SWATCH_Y + 1, TEXT_DIM, false);
        int power = RangeModuleItem.power(menu.tier());
        Component flow = Component.translatable("gui.cyberfarm.dish.flow",
                power < 0 ? Component.translatable("gui.cyberfarm.dish.unlimited") : Component.literal(String.format("%,d", power)));
        graphics.text(font, flow, 10, PANEL_Y + 27, TEXT_DIM, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (isHovering(PUBLIC_X, PUBLIC_Y, PUBLIC_W, 12, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(menu.isPublic() ? "gui.cyberfarm.dish.public.tip" : "gui.cyberfarm.dish.private.tip"), mouseX, mouseY);
        }
    }
}
