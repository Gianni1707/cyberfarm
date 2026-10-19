package com.cyberfarm.client.screen;

import com.cyberfarm.ClientConfig;
import com.cyberfarm.block.entity.MufflerBlockEntity.Category;
import com.cyberfarm.menu.MufflerMenu;
import com.cyberfarm.registry.ModItems;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** Settings panel: one row per kind of sound, each with an icon and an on/off switch. */
public class MufflerScreen extends MachineScreen<MufflerMenu> {
    private static final int ROW_X = 7;
    private static final int ROW_Y = 20;
    private static final int ROW_WIDTH = 162;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final int SWITCH_WIDTH = 24;
    private static final int SWITCH_HEIGHT = 10;

    public MufflerScreen(MufflerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, ROW_Y + Category.values().length * (ROW_HEIGHT + ROW_GAP) + 16);
        showInventory = false;
    }

    private static ItemStack icon(Category category) {
        return new ItemStack(switch (category) {
            case MACHINES -> ModItems.COMBUSTION_GENERATOR.get();
            case DRONES -> ModItems.DRONE.get();
            case MONSTERS -> Items.ZOMBIE_HEAD;
            case ANIMALS -> Items.WHEAT;
            case OTHER_MACHINES -> Items.FURNACE;
        });
    }

    private static int rowTop(Category category) {
        return ROW_Y + category.ordinal() * (ROW_HEIGHT + ROW_GAP);
    }

    private @Nullable Category rowAt(double mouseX, double mouseY) {
        for (Category category : Category.values()) {
            if (isHovering(ROW_X, rowTop(category), ROW_WIDTH, ROW_HEIGHT, mouseX, mouseY)) {
                return category;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Category category = event.button() == 0 ? rowAt(event.x(), event.y()) : null;
        if (category != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, category.ordinal());
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        for (Category category : Category.values()) {
            int top = y + rowTop(category);
            int left = x + ROW_X;
            boolean on = menu.isMuting(category);

            // Row: recessed strip, lit edge when its sound is silenced
            graphics.fill(left, top, left + ROW_WIDTH, top + ROW_HEIGHT, SLOT_BORDER);
            graphics.fill(left + 1, top + 1, left + ROW_WIDTH - 1, top + ROW_HEIGHT - 1, PANEL_DARK);
            graphics.fill(left + 1, top + 1, left + 3, top + ROW_HEIGHT - 1, on ? ACCENT : PANEL_LIGHT);

            graphics.item(icon(category), left + 6, top + 2);
            graphics.text(font, Component.translatable(category.translationKey()), left + 26, top + 6, on ? TEXT : TEXT_DIM, false);

            // Switch: knob on the right and cyan track when on
            int sx = left + ROW_WIDTH - SWITCH_WIDTH - 6;
            int sy = top + (ROW_HEIGHT - SWITCH_HEIGHT) / 2;
            graphics.fill(sx - 1, sy - 1, sx + SWITCH_WIDTH + 1, sy + SWITCH_HEIGHT + 1, SLOT_BORDER);
            graphics.fill(sx, sy, sx + SWITCH_WIDTH, sy + SWITCH_HEIGHT, on ? ACCENT_DIM : SLOT_BG);
            int knob = on ? sx + SWITCH_WIDTH - SWITCH_HEIGHT : sx;
            graphics.fill(knob, sy, knob + SWITCH_HEIGHT, sy + SWITCH_HEIGHT, on ? ACCENT : PANEL_LIGHT);
        }
        Component range = Component.translatable("gui.cyberfarm.muffler.range", ClientConfig.MUFFLER_RANGE.get());
        graphics.text(font, range, x + (imageWidth - font.width(range)) / 2, y + imageHeight - 12, TEXT_DIM, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        Category category = rowAt(mouseX, mouseY);
        if (category != null) {
            graphics.setTooltipForNextFrame(font, Component.translatable(category.translationKey() + ".hint"), mouseX, mouseY);
        }
    }
}
