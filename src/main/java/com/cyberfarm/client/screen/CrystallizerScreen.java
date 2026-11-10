package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.CrystallizerBlockEntity;
import com.cyberfarm.menu.CrystallizerMenu;
import com.cyberfarm.registry.ModItems;

import com.cyberfarm.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;

/** The acid tank, the isotope and amethyst slots, a progress bar and the crystal out. */
public class CrystallizerScreen extends MachineScreen<CrystallizerMenu> {
    private static final int TANK_X = 48;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public CrystallizerScreen(CrystallizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("crystallizer");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getAcidAmount(), CrystallizerBlockEntity.TANK_CAPACITY, menu.getAcidFluid());
        float progress = menu.getProgress();
        int cx = x + 23;
        // The crystal growing on its plinth, with smaller ones after it
        drawCrystal(graphics, cx, y + 49, 4 + Math.round(21 * progress), 5, 0xFF9B63E4, 0xFFD6B8FF, 0xFF5A2F9E);
        if (progress > 0.3F) {
            int small = Math.round(9 * (progress - 0.3F) / 0.7F) + 2;
            drawCrystal(graphics, cx - 10, y + 49, small, 2, 0xFF8A4FD8, 0xFFB98AF0, 0xFF5A2F9E);
            drawCrystal(graphics, cx + 10, y + 49, small, 2, 0xFF8A4FD8, 0xFFB98AF0, 0xFF5A2F9E);
        }
        drawGhost(graphics, CrystallizerBlockEntity.ISOTOPE, new ItemStack(ModItems.ISOTOPE_SHARD.get()));
        drawGhost(graphics, CrystallizerBlockEntity.AMETHYST, new ItemStack(Items.AMETHYST_SHARD));
        drawGhost(graphics, CrystallizerBlockEntity.OUTPUT, new ItemStack(ModItems.FUSION_CRYSTAL.get()));
        if (!menu.isFormed()) {
            int lineY = y + 15;
            for (FormattedCharSequence line : font.split(Component.translatable("gui.cyberfarm.crystallizer.incomplete"), 54)) {
                int width = font.width(line) + 8;
                int left = x + 124 - width / 2;
                drawTab(graphics, left, lineY, width);
                graphics.text(font, line, left + 4, lineY + 2, WARM, false);
                lineY += 10;
            }
        } else if (menu.getEnergyCapacity() > 0 && menu.getEnergy() < CrystallizerBlockEntity.ENERGY_PER_TICK && progress == 0) {
            drawNotice(graphics, x + 124, y + 56, Component.translatable("gui.cyberfarm.no_power"), WARM);
        }
    }

    /** One upright crystal standing on {@code foot}: pointed on top, lit on its left facet and dark on its right. */
    private static void drawCrystal(GuiGraphicsExtractor graphics, int cx, int foot, int height, int half, int fill, int lit, int dark) {
        int top = foot - height;
        for (int y = top; y < foot; y++) {
            int w = Math.min(half, (y - top) / 2 + 1);
            graphics.fill(cx - w - 1, y, cx + w + 1, y + 1, WELL);
        }
        for (int y = top + 1; y < foot; y++) {
            int w = Math.min(half, (y - top) / 2 + 1) - 1;
            if (w > 0) {
                int facet = Math.max(1, w / 2);
                graphics.fill(cx - w, y, cx + w, y + 1, fill);
                graphics.fill(cx - w, y, cx - w + facet, y + 1, lit);
                graphics.fill(cx + w - facet, y, cx + w, y + 1, dark);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getAcidAmount(), CrystallizerBlockEntity.TANK_CAPACITY, menu.getAcidFluid());
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index <= CrystallizerBlockEntity.OUTPUT) {
            String key = switch (hoveredSlot.index) {
                case CrystallizerBlockEntity.ISOTOPE -> "isotope";
                case CrystallizerBlockEntity.AMETHYST -> "amethyst";
                default -> "crystal";
            };
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.crystallizer.slot." + key), mouseX, mouseY);
        }
    }
}
