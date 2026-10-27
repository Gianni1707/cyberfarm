package com.cyberfarm.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.block.entity.SupplyTerminalBlockEntity;
import com.cyberfarm.client.VillageRange;
import com.cyberfarm.block.entity.SupplyTerminalBlockEntity.Kind;
import com.cyberfarm.menu.SupplyTerminalMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The village's board: three ordinary contracts and the event's, each with its progress, pay and
 * bonus; the delivery and payout slots (named in their tooltips); the mechanist's level and the next
 * contracts on the title line; and the village's level and prosperity under the board, beside the
 * Range switch that outlines the village's bounds in the world.
 */
public class SupplyTerminalScreen extends MachineScreen<SupplyTerminalMenu> {
    private static final int ROW_X = 8;
    private static final int ROW_Y = 16;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_WIDTH = 136;
    private static final int PROSPERITY_Y = ROW_Y + SupplyTerminalBlockEntity.CONTRACTS * ROW_HEIGHT + 1;
    private static final int MARKET = 0xFFE0B020;
    private static final int DEFENSE = 0xFFD04030;
    private static final ItemStack EMERALD = new ItemStack(Items.EMERALD);
    /** Tooltips wrap at this width so they never run off the screen. */
    private static final int TOOLTIP_WIDTH = 200;
    private static final int RANGE_WIDTH = 34;

    public SupplyTerminalScreen(SupplyTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, SupplyTerminalMenu.INVENTORY_Y + 82);
    }

    private static String clock(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        for (int i = 0; i < SupplyTerminalBlockEntity.CONTRACTS; i++) {
            int left = x + ROW_X;
            int top = y + ROW_Y + i * ROW_HEIGHT;
            ItemStack item = menu.contractItem(i);
            Kind kind = menu.contractKind(i);
            int border = item.isEmpty() ? SLOT_BORDER : kind == Kind.MARKET ? MARKET : kind == Kind.DEFENSE ? DEFENSE : SLOT_BORDER;
            graphics.fill(left, top, left + ROW_WIDTH, top + ROW_HEIGHT - 1, border);
            graphics.fill(left + 1, top + 1, left + ROW_WIDTH - 1, top + ROW_HEIGHT - 2, SLOT_BG);
            if (item.isEmpty()) {
                String key = i == SupplyTerminalBlockEntity.EVENT ? "gui.cyberfarm.terminal.no_event" : "gui.cyberfarm.terminal.empty";
                graphics.text(font, Component.translatable(key), left + 5, top + 4, TEXT_DIM, false);
                continue;
            }
            int required = menu.contractRequired(i);
            int delivered = menu.contractDelivered(i);
            graphics.item(item, left + 1, top);
            graphics.text(font, delivered + "/" + required, left + 20, top + 1, TEXT, false);
            drawProgressBar(graphics, left + 20, top + 11, 48, 3, required > 0 ? (float) delivered / required : 0.0F, kind == Kind.NORMAL ? 0xFF3FBF6F : border);
            ItemStack bonus = menu.contractBonus(i);
            if (!bonus.isEmpty()) {
                graphics.item(bonus, left + 72, top);
                graphics.itemDecorations(font, bonus, left + 72, top);
            }
            graphics.item(EMERALD, left + 95, top);
            graphics.text(font, "x" + menu.contractEmeralds(i), left + 112, top + 4, ACCENT, false);
        }
        // The mechanist and the next contracts, right-aligned on the title line
        int level = menu.mechanistLevel();
        Component status = level > 0
                ? Component.translatable("gui.cyberfarm.terminal.status", level, clock(menu.nextContractSeconds()))
                : Component.translatable("gui.cyberfarm.terminal.no_mechanist");
        graphics.text(font, status, x + imageWidth - 8 - font.width(status), y + 6, level > 0 ? TEXT_DIM : WARM, false);
        // The village: its level and the way to the next
        int village = menu.villageLevel();
        float toNext = village < 0 ? 0.0F : progress(menu.prosperity(), village);
        Component label = village < 0 ? Component.translatable("gui.cyberfarm.terminal.village.none")
                : Component.translatable("gui.cyberfarm.terminal.village", village);
        graphics.text(font, label, x + ROW_X, y + PROSPERITY_Y, village < 0 ? TEXT_DIM : ACCENT, false);
        int barWidth = ROW_WIDTH - font.width(label) - 6 - RANGE_WIDTH - 4;
        drawProgressBar(graphics, x + ROW_X + font.width(label) + 6, y + PROSPERITY_Y + 2, barWidth, 4, toNext, MARKET);
        // The Range switch, lit while the bounds show
        boolean on = VillageRange.isShown(menu.terminalPos());
        int left = x + ROW_X + ROW_WIDTH - RANGE_WIDTH;
        int top = y + PROSPERITY_Y - 2;
        graphics.fill(left, top, left + RANGE_WIDTH, top + 11, SLOT_BORDER);
        graphics.fill(left + 1, top + 1, left + RANGE_WIDTH - 1, top + 10, on ? ACCENT_DIM : PANEL_LIGHT);
        Component range = Component.translatable("gui.cyberfarm.terminal.range");
        graphics.text(font, range, left + (RANGE_WIDTH - font.width(range)) / 2, top + 2, on ? TEXT : ACCENT, false);
    }

    private boolean overRange(double mouseX, double mouseY) {
        return isHovering(ROW_X + ROW_WIDTH - RANGE_WIDTH, PROSPERITY_Y - 2, RANGE_WIDTH, 11, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && overRange(event.x(), event.y())) {
            VillageRange.toggle(menu.terminalPos());
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** How far the points are from the last level's mark to the next one's. */
    private static float progress(int points, int level) {
        int[] steps = SupplyTerminalBlockEntity.VILLAGE_LEVELS;
        if (level >= steps.length) {
            return 1.0F;
        }
        int from = level == 0 ? 0 : steps[level - 1];
        return Math.clamp((float) (points - from) / (steps[level] - from), 0.0F, 1.0F);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < SupplyTerminalBlockEntity.CONTRACTS; i++) {
            ItemStack item = menu.contractItem(i);
            if (item.isEmpty() || !isHovering(ROW_X, ROW_Y + i * ROW_HEIGHT, ROW_WIDTH, ROW_HEIGHT - 1, mouseX, mouseY)) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            Kind kind = menu.contractKind(i);
            if (kind != Kind.NORMAL) {
                lines.add(Component.translatable("gui.cyberfarm.terminal.kind." + kind.name().toLowerCase()).withColor(kind == Kind.MARKET ? MARKET : DEFENSE));
            }
            lines.add(Component.translatable("gui.cyberfarm.terminal.contract", menu.contractRequired(i), item.getHoverName(), menu.contractEmeralds(i)));
            ItemStack bonus = menu.contractBonus(i);
            if (!bonus.isEmpty()) {
                lines.add(Component.translatable("gui.cyberfarm.terminal.bonus", bonus.getCount(), bonus.getHoverName()).withColor(MARKET));
            }
            int left = menu.contractSecondsLeft(i);
            if (left >= 0) {
                lines.add(Component.translatable("gui.cyberfarm.terminal.closes", clock(left)).withColor(TEXT_DIM));
            }
            graphics.setTooltipForNextFrame(lines.stream().flatMap(line -> font.split(line, TOOLTIP_WIDTH).stream()).toList(), mouseX, mouseY);
        }
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index <= SupplyTerminalBlockEntity.PAYOUT) {
            String key = hoveredSlot.index == SupplyTerminalBlockEntity.DELIVERY ? "deliver" : "payout";
            tooltip(graphics, Component.translatable("gui.cyberfarm.terminal." + key), mouseX, mouseY);
        }
        if (isHovering(imageWidth - 80, 4, 72, 10, mouseX, mouseY)) {
            int level = menu.mechanistLevel();
            tooltip(graphics, level > 0
                    ? Component.translatable("gui.cyberfarm.terminal.mechanist.hint", Component.translatable("merchant.level." + level))
                    : Component.translatable("gui.cyberfarm.terminal.no_mechanist.hint"), mouseX, mouseY);
        }
        if (overRange(mouseX, mouseY)) {
            tooltip(graphics, Component.translatable("gui.cyberfarm.terminal.range.hint", SupplyTerminalBlockEntity.VILLAGE_RADIUS), mouseX, mouseY);
        } else if (isHovering(ROW_X, PROSPERITY_Y - 1, ROW_WIDTH, 10, mouseX, mouseY)) {
            int points = menu.prosperity();
            int level = menu.villageLevel();
            int[] steps = SupplyTerminalBlockEntity.VILLAGE_LEVELS;
            Component text = level < 0 ? Component.translatable("gui.cyberfarm.terminal.village.none.hint")
                    : level >= steps.length ? Component.translatable("gui.cyberfarm.terminal.prosperity.max", points)
                    : points >= steps[level] ? Component.translatable("gui.cyberfarm.terminal.prosperity.ready", points)
                    : Component.translatable("gui.cyberfarm.terminal.prosperity.hint", points, steps[level]);
            tooltip(graphics, text, mouseX, mouseY);
        }
    }

    private void tooltip(GuiGraphicsExtractor graphics, Component text, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font.split(text, TOOLTIP_WIDTH), mouseX, mouseY);
    }
}
