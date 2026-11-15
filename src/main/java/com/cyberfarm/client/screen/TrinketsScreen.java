package com.cyberfarm.client.screen;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.TrinketClient;
import com.cyberfarm.menu.TrinketsMenu;
import com.cyberfarm.trinket.EnergySphereItem;
import com.cyberfarm.trinket.PocketDroneItem;
import com.cyberfarm.trinket.UtilityBeltItem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The inventory's Trinkets tab, drawn like the game's inventory: the trinket slots where the armor is, the player wearing
 * them, and what is left in the Energy Sphere and the Pocket Drone.
 */
public class TrinketsScreen extends AbstractContainerScreen<TrinketsMenu> {
    private static final Identifier BACKGROUND = CyberFarm.id("textures/gui/trinkets.png");
    /** The game's own label grey. */
    private static final int TEXT = 0xFF404040, TEXT_DIM = 0xFF707070;

    public TrinketsScreen(TrinketsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        TrinketClient.addTabs(this, this::addRenderableWidget, true);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        // Where the game's inventory has its player
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, leftPos + 26, topPos + 8, leftPos + 75, topPos + 78, 30, 0.0625F,
                mouseX, mouseY, minecraft.player);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Like the game's inventory: no "Inventory" line, which would run into the player's window
        graphics.text(font, title, 82, 8, TEXT, false);
        Player player = minecraft.player;
        ItemStack sphere = UtilityBeltItem.content(player, UtilityBeltItem.SPHERE);
        ItemStack drone = UtilityBeltItem.content(player, UtilityBeltItem.DRONE);
        int y = 26;
        graphics.text(font, Component.translatable("gui.cyberfarm.trinkets.sphere"), 82, y, TEXT_DIM, false);
        graphics.text(font, sphere.isEmpty() ? Component.translatable("gui.cyberfarm.trinkets.none")
                : Component.literal(percent(EnergySphereItem.energy(sphere), EnergySphereItem.capacity(sphere))), 82, y + 10, TEXT, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.trinkets.drone"), 82, y + 26, TEXT_DIM, false);
        graphics.text(font, drone.isEmpty() ? Component.translatable("gui.cyberfarm.trinkets.none")
                : Component.literal(percent(PocketDroneItem.energy(drone), PocketDroneItem.CAPACITY)), 82, y + 36, TEXT, false);
    }

    static String percent(int energy, int capacity) {
        return Math.round(100.0F * energy / capacity) + "%";
    }
}
