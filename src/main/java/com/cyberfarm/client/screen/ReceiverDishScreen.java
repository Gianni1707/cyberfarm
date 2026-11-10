package com.cyberfarm.client.screen;

import java.util.List;

import com.cyberfarm.block.entity.ReceiverDishBlockEntity;
import com.cyberfarm.energy.DishNetworks;
import com.cyberfarm.menu.ReceiverDishMenu;

import com.cyberfarm.port.client.GuiGraphicsExtractor;
import com.cyberfarm.port.client.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Receiver Dish (design R1): the networks it may link to as wide rows (colour chip, name, distance or dimension, seats),
 * the linked one lit, a full one greyed out; one click links. No inventory: the dish has no slots.
 */
public class ReceiverDishScreen extends MachineScreen<ReceiverDishMenu> {
    private static final int ROW_X = 7, ROW_Y = 31, ROW_W = 162, ROW_H = 20, ROWS = 3;
    private static final int BAR_X = 7, BAR_Y = 98, BAR_W = 98, BAR_H = 8;
    private static final int DISCONNECT_X = 110, DISCONNECT_Y = 96, DISCONNECT_W = 59;
    private static final int GREY = 0xFF5A6B69;

    private int scroll;

    public ReceiverDishScreen(ReceiverDishMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 128);
        showInventory = false;
        // No glass gauge: its hover area would sit over the rows
        energyBarX = -1000;
    }

    /** The energy shows on the horizontal bar instead of the usual glass. */
    @Override
    protected void drawEnergyBar(GuiGraphicsExtractor graphics, int x, int y, int height) {
    }

    private List<ReceiverDishBlockEntity.Choice> choices() {
        return menu.choices();
    }

    private int buttonAt(double mouseX, double mouseY) {
        for (int i = 0; i < ROWS && scroll + i < choices().size(); i++) {
            if (isHovering(ROW_X, ROW_Y + i * (ROW_H + 2), ROW_W, ROW_H, mouseX, mouseY)) {
                int row = scroll + i;
                return row != menu.linked() && menu.seats(row) < DishNetworks.MAX_RECEIVERS ? row : -1;
            }
        }
        if (menu.linked() >= 0 && isHovering(DISCONNECT_X, DISCONNECT_Y, DISCONNECT_W, 12, mouseX, mouseY)) {
            return ReceiverDishMenu.BUTTON_DISCONNECT;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button() == 0 ? buttonAt(event.x(), event.y()) : -1;
        if (button >= 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        int max = Math.max(0, choices().size() - ROWS);
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, max);
        return true;
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawWell(graphics, x + 6, y + 17, 164, 11);
        if (choices().isEmpty()) {
            drawWell(graphics, x + ROW_X, y + ROW_Y, ROW_W, 3 * ROW_H + 4);
        }
        for (int i = 0; i < ROWS && scroll + i < choices().size(); i++) {
            int row = scroll + i;
            int ry = y + ROW_Y + i * (ROW_H + 2);
            boolean linked = row == menu.linked();
            boolean full = !linked && menu.seats(row) >= DishNetworks.MAX_RECEIVERS;
            graphics.fill(x + ROW_X, ry, x + ROW_X + ROW_W, ry + ROW_H, linked ? MINT_MID : SLOT_BORDER);
            graphics.fill(x + ROW_X + 1, ry + 1, x + ROW_X + ROW_W - 1, ry + ROW_H - 1, linked ? PANEL_LIGHT : WELL);
            if (linked) {
                graphics.fill(x + ROW_X + 1, ry + 1, x + ROW_X + 2, ry + ROW_H - 1, GLOW);
            }
            // Colour chip: a small octagon in the network's colour, grey when the network is full
            int chip = full ? GREY : 0xFF000000 | choices().get(row).rgb();
            int cx = x + ROW_X + 6, cy = ry + 5;
            graphics.fill(cx + 1, cy, cx + 9, cy + 10, chip);
            graphics.fill(cx, cy + 1, cx + 10, cy + 9, chip);
            // Seats as four pips
            for (int s = 0; s < DishNetworks.MAX_RECEIVERS; s++) {
                int px = x + ROW_X + 116 + s * 5;
                graphics.fill(px, ry + 4, px + 4, ry + 9, SLOT_BORDER);
                graphics.fill(px + 1, ry + 5, px + 3, ry + 8, s < menu.seats(row) ? (full ? GREY : MINT_MID) : PANEL_DARK);
            }
            if (full) {
                graphics.fill(x + ROW_X + 114, ry + 11, x + ROW_X + 140, ry + 19, 0xFF3A1210);
            }
        }
        // Energy bar
        graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, SLOT_BORDER);
        graphics.fill(x + BAR_X + 1, y + BAR_Y + 1, x + BAR_X + BAR_W - 1, y + BAR_Y + BAR_H - 1, SLOT_BG);
        int capacity = menu.getEnergyCapacity();
        int filled = capacity <= 0 ? 0 : (int) ((long) (BAR_W - 2) * Math.min(menu.getEnergy(), capacity) / capacity);
        if (filled > 0) {
            graphics.fill(x + BAR_X + 1, y + BAR_Y + 1, x + BAR_X + 1 + filled, y + BAR_Y + BAR_H - 1, ACCENT);
        }
        drawButton(graphics, x + DISCONNECT_X, y + DISCONNECT_Y, DISCONNECT_W, 12, Component.translatable("gui.cyberfarm.dish.disconnect"),
                menu.linked() >= 0 ? PANEL_LIGHT : PANEL_DARK, menu.linked() >= 0 ? TEXT : GREY);
    }

    /** The stored energy on hover, for the horizontal bar the usual glass gives way to. */
    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.energy",
                    String.format("%,d", menu.getEnergy()), String.format("%,d", menu.getEnergyCapacity())), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int linked = menu.linked();
        Component status = linked >= 0 ? Component.translatable("gui.cyberfarm.dish.linked_to", choices().get(linked).name())
                : Component.translatable("gui.cyberfarm.dish.not_linked");
        graphics.text(font, status, 10, 19, linked >= 0 ? TEXT : TEXT_DIM, false);
        if (choices().isEmpty()) {
            graphics.text(font, Component.translatable("gui.cyberfarm.dish.none_in_range"), ROW_X + 6, ROW_Y + 8, TEXT, false);
            graphics.text(font, Component.translatable("gui.cyberfarm.dish.none_hint_1"), ROW_X + 6, ROW_Y + 24, TEXT_DIM, false);
            graphics.text(font, Component.translatable("gui.cyberfarm.dish.none_hint_2"), ROW_X + 6, ROW_Y + 34, TEXT_DIM, false);
            graphics.text(font, Component.translatable("gui.cyberfarm.dish.none_hint_3"), ROW_X + 6, ROW_Y + 44, TEXT_DIM, false);
        }
        for (int i = 0; i < ROWS && scroll + i < choices().size(); i++) {
            int row = scroll + i;
            int ry = ROW_Y + i * (ROW_H + 2);
            ReceiverDishBlockEntity.Choice choice = choices().get(row);
            boolean linkedRow = row == linked;
            boolean full = !linkedRow && menu.seats(row) >= DishNetworks.MAX_RECEIVERS;
            graphics.text(font, Component.literal(choice.name()), ROW_X + 20, ry + 2, full ? GREY : TEXT, false);
            graphics.text(font, Component.literal(choice.where()), ROW_X + 20, ry + 11, full ? GREY : TEXT_DIM, false);
            graphics.text(font, Component.literal(menu.seats(row) + "/" + DishNetworks.MAX_RECEIVERS), ROW_X + 142, ry + 4, full ? GREY : TEXT, false);
            if (linkedRow) {
                graphics.text(font, Component.translatable("gui.cyberfarm.dish.linked"), ROW_X + 116, ry + 11, MINT, false);
            } else if (full) {
                graphics.text(font, Component.translatable("gui.cyberfarm.dish.full"), ROW_X + 116, ry + 11, DANGER, false);
            }
        }
        if (choices().size() > ROWS) {
            graphics.text(font, Component.literal((scroll + 1) + "-" + Math.min(choices().size(), scroll + ROWS) + "/" + choices().size()),
                    ROW_X + ROW_W - 30, ROW_Y + 3 * (ROW_H + 2) - 1, TEXT_DIM, false);
        }
        graphics.text(font, Component.translatable("gui.cyberfarm.dish.receiving", String.format("%,d", menu.received())), BAR_X, 114, TEXT_DIM, false);
    }
}
