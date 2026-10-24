package com.cyberfarm.client.screen;

import com.cyberfarm.entity.MechEntity;
import com.cyberfarm.entity.MechModule;
import com.cyberfarm.menu.MechHangarMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The hangar console. On the left, a card for the parked mech: its battery, its integrity and the
 * hangar's own battery as labelled bars, the status, and the Fold button. On the right, the mech's
 * outline in the bay with its four module slots at their mounts: the jet behind the shoulder, the
 * armour on the trunk, the blade and the drill by the arms.
 */
public class MechHangarScreen extends MachineScreen<MechHangarMenu> {
    private static final int GREEN = 0xFF6CFF8C;
    private static final int BAY = 0xFF0B1216;
    private static final int GRID = 0xFF101C22;
    private static final int CARD_X0 = 6;
    private static final int CARD_X1 = 68;
    private static final int BAY_X0 = 72;
    private static final int BAY_X1 = 170;
    private static final int TOP = 17;
    private static final int BOTTOM = 109;
    private static final int BAR_X = 9;
    private static final int BAR_WIDTH = 56;
    private static final int FOLD_Y = 95;
    private static final int FOLD_HEIGHT = 12;

    public MechHangarScreen(MechHangarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 210);
        // The hangar's battery is the third bar of the card; its tooltip spot is there too
        energyBarX = BAR_X;
        energyBarY = 70;
        energyBarHeight = 7;
    }

    /** The card draws the hangar's battery as one of its bars. */
    @Override
    protected void drawEnergyBar(GuiGraphicsExtractor graphics, int x, int y, int height) {
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        boolean parked = menu.getStatus() != 0;

        // The card
        graphics.fill(x + CARD_X0, y + TOP, x + CARD_X1, y + BOTTOM, PANEL_DARK);
        graphics.text(font, Component.translatable("gui.cyberfarm.hangar.mech"), x + BAR_X, y + TOP + 3, ACCENT, false);
        float battery = parked ? (float) menu.getMechEnergy() / MechEntity.CAPACITY : 0.0F;
        float integrity = parked ? menu.getMechIntegrity() / 100.0F : 0.0F;
        float hangar = menu.getEnergyCapacity() > 0 ? (float) menu.getEnergy() / menu.getEnergyCapacity() : 0.0F;
        bar(graphics, x, y + 32, "gui.cyberfarm.hangar.battery", battery, parked, battery < 0.1F ? WARM : ACCENT);
        bar(graphics, x, y + 50, "gui.cyberfarm.hangar.integrity", integrity, parked, integrity < 0.3F ? WARM : GREEN);
        bar(graphics, x, y + 68, "gui.cyberfarm.hangar.power", hangar, true, ACCENT);
        Component status = Component.translatable(!menu.isFormed() ? "gui.cyberfarm.hangar.incomplete"
                : switch (menu.getStatus()) {
                    case 1 -> "gui.cyberfarm.hangar.charging";
                    case 2 -> menu.isMechDisabled() ? "gui.cyberfarm.hangar.restoring" : "gui.cyberfarm.hangar.repairing";
                    case 3 -> "gui.cyberfarm.hangar.ready";
                    default -> "gui.cyberfarm.hangar.empty";
                });
        int statusColor = !menu.isFormed() ? WARM : menu.getStatus() == 0 ? TEXT_DIM : menu.getStatus() == 3 ? GREEN : ACCENT;
        graphics.fill(x + BAR_X, y + 85, x + BAR_X + 3, y + 88, statusColor);
        graphics.text(font, status, x + BAR_X + 6, y + 83, statusColor, false);
        drawButton(graphics, x + BAR_X, y + FOLD_Y, BAR_WIDTH, FOLD_HEIGHT, Component.translatable("gui.cyberfarm.hangar.fold"),
                parked ? PANEL_LIGHT : PANEL, parked ? WARM : TEXT_DIM);

        // The bay: a dark grid, the mech's outline, the slots at its mounts with their callouts
        graphics.fill(x + BAY_X0 - 1, y + TOP - 1, x + BAY_X1 + 1, y + BOTTOM + 1, ACCENT_DIM);
        graphics.fill(x + BAY_X0, y + TOP, x + BAY_X1, y + BOTTOM, BAY);
        for (int gx = BAY_X0 + 8; gx < BAY_X1; gx += 8) {
            graphics.fill(x + gx, y + TOP, x + gx + 1, y + BOTTOM, GRID);
        }
        for (int gy = TOP + 8; gy < BOTTOM; gy += 8) {
            graphics.fill(x + BAY_X0, y + gy, x + BAY_X1, y + gy + 1, GRID);
        }
        int fill = parked ? 0xFF1B3A48 : 0xFF1A2026;
        int edge = parked ? ACCENT_DIM : PANEL_LIGHT;
        part(graphics, x + 116, y + 22, 10, 9, fill, edge);          // head
        part(graphics, x + 110, y + 33, 22, 31, fill, edge);         // trunk
        part(graphics, x + 101, y + 35, 7, 35, fill, edge);          // left arm
        part(graphics, x + 134, y + 35, 7, 35, fill, edge);          // right arm
        part(graphics, x + 112, y + 66, 7, 38, fill, edge);          // legs
        part(graphics, x + 123, y + 66, 7, 38, fill, edge);
        int line = parked ? ACCENT : TEXT_DIM;
        graphics.fill(x + 133, y + 32, x + 147, y + 33, line);       // jet: behind the shoulder
        graphics.fill(x + 104, y + 70, x + 105, y + 84, line);       // blade: down the left arm
        graphics.fill(x + 95, y + 84, x + 105, y + 85, line);
        graphics.fill(x + 137, y + 70, x + 138, y + 84, line);       // drill: down the right arm
        graphics.fill(x + 137, y + 84, x + 147, y + 85, line);
        // The slots again, over the outline
        for (int i = 0; i < MechModule.values().length; i++) {
            Slot slot = menu.slots.get(i);
            drawSlot(graphics, x + slot.x, y + slot.y);
        }
        int label = parked ? TEXT : TEXT_DIM;
        text(graphics, MechModule.JET, x + 145, y + 21, label);
        centred(graphics, MechModule.DRILL, x + 156, y + 96, label);
        centred(graphics, MechModule.BLADE, x + 87, y + 96, label);
        centred(graphics, MechModule.ARMOR, x + 121, y + 36, label);
    }

    /** A labelled bar of the card: the name on its own line, the bar under it with the percentage at its end. */
    private void bar(GuiGraphicsExtractor graphics, int x, int y, String key, float fill, boolean known, int color) {
        graphics.text(font, Component.translatable(key), x + BAR_X, y, TEXT_DIM, false);
        String value = known ? Math.round(fill * 100) + "%" : "--";
        int barWidth = BAR_WIDTH - 26;
        drawProgressBar(graphics, x + BAR_X, y + 11, barWidth, 4, known ? fill : 0.0F, color);
        graphics.text(font, value, x + BAR_X + BAR_WIDTH - font.width(value), y + 9, known ? TEXT : TEXT_DIM, false);
    }

    private static void part(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fill, int edge) {
        graphics.fill(x, y, x + w, y + h, edge);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** A module's name ending at {@code x}, beside its slot. */
    private void text(GuiGraphicsExtractor graphics, MechModule module, int x, int y, int color) {
        Component name = Component.translatable(module.hudKey());
        graphics.text(font, name, x - font.width(name), y, color, false);
    }

    /** A module's name centred on {@code x}, under or over its slot. */
    private void centred(GuiGraphicsExtractor graphics, MechModule module, int x, int y, int color) {
        Component name = Component.translatable(module.hudKey());
        graphics.text(font, name, x - font.width(name) / 2, y, color, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && menu.getStatus() != 0 && isHovering(BAR_X, FOLD_Y, BAR_WIDTH, FOLD_HEIGHT, event.x(), event.y())) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MechHangarMenu.BUTTON_FOLD);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (menu.getStatus() != 0 && isHovering(BAR_X, FOLD_Y, BAR_WIDTH, FOLD_HEIGHT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.hangar.fold.tooltip"), mouseX, mouseY);
        }
        if (menu.getStatus() != 0 && isHovering(BAR_X, 32, BAR_WIDTH, 14, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.hangar.mech_tooltip",
                    String.format("%,d", menu.getMechEnergy()), String.format("%,d", MechEntity.CAPACITY), menu.getMechIntegrity()), mouseX, mouseY);
        }
    }
}
