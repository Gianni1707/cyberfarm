package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;
import com.cyberfarm.menu.ResonanceCoreMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The core's tier and shell, its state, what it found on each side (so a missing beam says why),
 * how much each pylon carries and the charge.
 */
public class ResonanceCoreScreen extends MachineScreen<ResonanceCoreMenu> {
    private static final int PYLONS_Y = 48;
    private static final int SIDES_Y = 60;
    private static final int ROW = 10;
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final String[] SIDE_KEYS = {"none", "linked", "too_close", "too_far", "too_short", "blocked"};

    public ResonanceCoreScreen(ResonanceCoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 134);
        showInventory = false;
        energyBarHeight = 106;
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        int tier = menu.getTier();
        if (tier <= 0) {
            return;
        }
        int shell = 2 * tier - 1;
        graphics.text(font, Component.translatable(tier == 1 ? "gui.cyberfarm.core.tier.base" : "gui.cyberfarm.core.tier", tier, shell, shell, shell),
                x + 8, y + 22, TEXT, false);
        boolean online = menu.getStatus() == ResonanceCoreBlockEntity.STATUS_ONLINE;
        graphics.text(font, Component.translatable(online ? "gui.cyberfarm.core.status.online" : "gui.cyberfarm.core.status.unstable"),
                x + 8, y + 34, online ? ACCENT : WARM, false);

        int pylons = menu.getLinkedPylons();
        int required = ResonanceCoreBlockEntity.requiredPylons(tier);
        graphics.text(font, Component.translatable("gui.cyberfarm.core.pylons", pylons, required), x + 8, y + PYLONS_Y,
                pylons < required ? WARM : TEXT_DIM, false);
        for (int i = 0; i < SIDES.length; i++) {
            int code = ResonanceCoreBlockEntity.sideCode(menu.getSide(SIDES[i].get2DDataValue()));
            int color = code == ResonanceCoreBlockEntity.SIDE_LINKED ? ACCENT : code == ResonanceCoreBlockEntity.SIDE_NONE ? TEXT_DIM : WARM;
            graphics.text(font, Component.translatable("gui.cyberfarm.core.side",
                    Component.translatable("gui.cyberfarm.core.side." + SIDES[i].getSerializedName()),
                    Component.translatable("gui.cyberfarm.core.pylon." + SIDE_KEYS[Math.min(code, SIDE_KEYS.length - 1)])),
                    x + 14, y + SIDES_Y + i * ROW, color, false);
        }

        graphics.text(font, Component.translatable("gui.cyberfarm.core.transfer", compact(ResonanceCoreBlockEntity.transfer(tier))),
                x + 8, y + 104, TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.core.stored", compact(menu.getEnergy()), compact(menu.getEnergyCapacity())),
                x + 8, y + 116, TEXT, false);
    }

    /** 43,200,000 → "43.2M", so a full core still fits the line. */
    private static String compact(int value) {
        if (value >= 1_000_000_000) {
            return String.format("%.2fG", value / 1_000_000_000.0);
        }
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        return value >= 10_000 ? String.format("%.1fk", value / 1000.0) : String.format("%,d", value);
    }

    /** The pylons line says what this tier asks for; each side's line gives that column's numbers. */
    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int tier = menu.getTier();
        if (tier <= 0) {
            return;
        }
        if (isHovering(6, PYLONS_Y - 2, 140, ROW, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.core.pylons.hint",
                    ResonanceCoreBlockEntity.requiredPylons(tier), ResonanceCoreBlockEntity.requiredHeight(tier),
                    ResonanceCoreBlockEntity.minDistance(tier), ResonanceCoreBlockEntity.maxDistance(tier)), mouseX, mouseY);
        }
        for (int i = 0; i < SIDES.length; i++) {
            int side = menu.getSide(SIDES[i].get2DDataValue());
            if (ResonanceCoreBlockEntity.sideCode(side) != ResonanceCoreBlockEntity.SIDE_NONE
                    && isHovering(12, SIDES_Y - 2 + i * ROW, 134, ROW, mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.core.pylon.details",
                        ResonanceCoreBlockEntity.sideHeight(side), ResonanceCoreBlockEntity.requiredHeight(tier),
                        ResonanceCoreBlockEntity.sideDistance(side), ResonanceCoreBlockEntity.minDistance(tier),
                        ResonanceCoreBlockEntity.maxDistance(tier)), mouseX, mouseY);
            }
        }
    }
}
