package com.cyberfarm.client.screen;

import java.util.List;

import com.cyberfarm.block.entity.ItemFilter;
import com.cyberfarm.menu.ItemPipeMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

/** Filter GUI: 9 ghost slots, three mode switches and a tag picker. */
public class ItemPipeScreen extends MachineScreen<ItemPipeMenu> {
    private static final int BUTTON_Y = 42;
    private static final int BUTTON_WIDTH = 52;
    private static final int BUTTON_HEIGHT = 14;
    private static final int TAG_Y = 62;
    private static final int ARROW = 12;

    public ItemPipeScreen(ItemPipeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    private static int buttonX(int index) {
        return 8 + index * (BUTTON_WIDTH + 4);
    }

    private int buttonAt(double mouseX, double mouseY) {
        for (int i = 0; i < 3; i++) {
            if (isHovering(buttonX(i), BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
                return i;
            }
        }
        if (isHovering(8, TAG_Y, ARROW, ARROW, mouseX, mouseY)) {
            return ItemPipeMenu.BUTTON_TAG_PREVIOUS;
        }
        if (isHovering(imageWidth - 8 - ARROW, TAG_Y, ARROW, ARROW, mouseX, mouseY)) {
            return ItemPipeMenu.BUTTON_TAG_NEXT;
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

    private Component buttonLabel(int index) {
        return switch (index) {
            case 0 -> Component.translatable(menu.isWhitelist() ? "gui.cyberfarm.filter.whitelist" : "gui.cyberfarm.filter.blacklist");
            case 1 -> Component.translatable(menu.matchesComponents() ? "gui.cyberfarm.filter.components_on" : "gui.cyberfarm.filter.components_off");
            default -> Component.translatable(menu.matchesMod() ? "gui.cyberfarm.filter.mod_on" : "gui.cyberfarm.filter.mod_off");
        };
    }

    private boolean buttonOn(int index) {
        return switch (index) {
            case 0 -> menu.isWhitelist();
            case 1 -> menu.matchesComponents();
            default -> menu.matchesMod();
        };
    }

    private Component tagLabel() {
        List<TagKey<Item>> tags = ItemFilter.tagsOf(menu.firstSample());
        int index = menu.getTagIndex();
        if (index >= 0 && index < tags.size()) {
            return Component.literal("#" + tags.get(index).location());
        }
        return Component.translatable(tags.isEmpty() ? "gui.cyberfarm.filter.tag_none_available" : "gui.cyberfarm.filter.tag_none");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        for (int i = 0; i < 3; i++) {
            int left = x + buttonX(i);
            int top = y + BUTTON_Y;
            boolean on = buttonOn(i);
            graphics.fill(left, top, left + BUTTON_WIDTH, top + BUTTON_HEIGHT, SLOT_BORDER);
            graphics.fill(left + 1, top + 1, left + BUTTON_WIDTH - 1, top + BUTTON_HEIGHT - 1, on ? ACCENT_DIM : PANEL_DARK);
            Component label = buttonLabel(i);
            graphics.text(font, label, left + (BUTTON_WIDTH - font.width(label)) / 2, top + 3, on ? TEXT : TEXT_DIM, false);
        }
        // Tag picker: < #namespace:tag >
        int top = y + TAG_Y;
        arrow(graphics, x + 8, top, "<");
        arrow(graphics, x + imageWidth - 8 - ARROW, top, ">");
        graphics.fill(x + 8 + ARROW + 2, top, x + imageWidth - 10 - ARROW, top + ARROW, SLOT_BORDER);
        graphics.fill(x + 9 + ARROW + 2, top + 1, x + imageWidth - 11 - ARROW, top + ARROW - 1, SLOT_BG);
        Component tag = tagLabel();
        int width = imageWidth - 2 * (ARROW + 12);
        String text = font.plainSubstrByWidth(tag.getString(), width - 4);
        boolean set = menu.getTagIndex() >= 0;
        graphics.text(font, text, x + (imageWidth - font.width(text)) / 2, top + 2, set ? ACCENT : TEXT_DIM, false);
    }

    private void arrow(GuiGraphicsExtractor graphics, int x, int y, String symbol) {
        graphics.fill(x, y, x + ARROW, y + ARROW, SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + ARROW - 1, y + ARROW - 1, PANEL_LIGHT);
        graphics.text(font, symbol, x + (ARROW - font.width(symbol)) / 2 + 1, y + 2, TEXT, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int button = buttonAt(mouseX, mouseY);
        if (button >= 0 && button < 3) {
            String[] hints = {"gui.cyberfarm.filter.whitelist.hint", "gui.cyberfarm.filter.components.hint", "gui.cyberfarm.filter.mod.hint"};
            graphics.setTooltipForNextFrame(font, Component.translatable(hints[button]), mouseX, mouseY);
        } else if (isHovering(8, TAG_Y, imageWidth - 16, ARROW, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.filter.tag.hint"), mouseX, mouseY);
        } else if (isHovering(ItemPipeMenu.FILTER_X - 1, ItemPipeMenu.FILTER_Y - 1, ItemFilter.SLOTS * 18, 18, mouseX, mouseY)
                && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.filter.slots.hint"), mouseX, mouseY);
        }
    }
}
