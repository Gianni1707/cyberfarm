package com.cyberfarm.client.screen;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.TrinketClient;
import com.cyberfarm.menu.UtilityBeltMenu;
import com.cyberfarm.trinket.EnergySphereItem;
import com.cyberfarm.trinket.PocketDroneItem;
import com.cyberfarm.trinket.UtilityBeltItem;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The Utility Belt's four holders on the belt drawn behind them, named above, with the sphere's charge and the drone's battery under them. */
public class UtilityBeltScreen extends AbstractContainerScreen<UtilityBeltMenu> {
    private static final Identifier BACKGROUND = CyberFarm.id("textures/gui/utility_belt.png");
    private static final int TEXT = 0xFFD8F8FF, MINT = 0xFF5EC2B8, GLOW = 0xFFD1FEE7, EMERALD = 0xFF00B894;

    public UtilityBeltScreen(UtilityBeltMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        ItemStack sphere = UtilityBeltItem.content(minecraft.player, UtilityBeltItem.SPHERE);
        ItemStack drone = UtilityBeltItem.content(minecraft.player, UtilityBeltItem.DRONE);
        // A charge bar under the sphere's holder and under the drone's
        bar(graphics, UtilityBeltItem.SPHERE, EnergySphereItem.energy(sphere) / (float) Math.max(1, EnergySphereItem.capacity(sphere)), !sphere.isEmpty());
        bar(graphics, UtilityBeltItem.DRONE, PocketDroneItem.energy(drone) / (float) PocketDroneItem.CAPACITY, !drone.isEmpty());
    }

    private void bar(GuiGraphicsExtractor graphics, int holder, float charge, boolean present) {
        int x = leftPos + UtilityBeltMenu.SLOT_X + holder * UtilityBeltMenu.SLOT_PITCH;
        int y = topPos + UtilityBeltMenu.SLOT_Y + 19;
        graphics.fill(x, y, x + 16, y + 2, 0xFF0D1C1C);
        int filled = present ? Math.round(16 * charge) : 0;
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 2, EMERALD);
            graphics.fill(x, y, x + filled, y + 1, GLOW);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, MINT, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        // Each holder's name above it
        for (int holder = 0; holder < UtilityBeltItem.SLOTS; holder++) {
            Component name = Component.translatable("gui.cyberfarm.utility_belt.slot_" + holder);
            int centre = UtilityBeltMenu.SLOT_X + holder * UtilityBeltMenu.SLOT_PITCH + 8;
            graphics.text(font, name, centre - font.width(name) / 2, 22, GLOW, false);
        }
        // Under each tool its key, in the panel below the bars (the names above already say what it is)
        // shortcut: a key bound to a long name ("Left Shift") runs past its column, still inside the panel; truncate if it bothers
        KeyMapping[] keys = {null, TrinketClient.PROJECTOR, TrinketClient.SURVEY_TOOL, TrinketClient.DRONE, TrinketClient.CABLE_SWAPPER};
        for (int holder = 1; holder < keys.length; holder++) {
            Component key = Component.translatable("gui.cyberfarm.utility_belt.key", keys[holder].getTranslatedKeyMessage());
            int centre = UtilityBeltMenu.SLOT_X + holder * UtilityBeltMenu.SLOT_PITCH + 8;
            graphics.text(font, key, centre - font.width(key) / 2, 59, TEXT, false);
        }
    }
}
