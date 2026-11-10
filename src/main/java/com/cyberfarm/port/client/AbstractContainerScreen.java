package com.cyberfarm.port.client;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * The 26.x container screen (extract background, labels and tooltip; clicks as events) on the 1.20.1
 * one. The background is dimmed and the hovered slot's tooltip drawn here, as 26.x does itself.
 */
public abstract class AbstractContainerScreen<T extends AbstractContainerMenu>
        extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<T> {
    private @Nullable GuiGraphicsExtractor extractor;

    protected AbstractContainerScreen(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title);
        this.imageWidth = width;
        this.imageHeight = height;
        this.inventoryLabelY = height - 94;
    }

    private GuiGraphicsExtractor wrap(GuiGraphics graphics) {
        GuiGraphicsExtractor current = extractor;
        if (current == null || current.vanilla() != graphics) {
            current = extractor = new GuiGraphicsExtractor(graphics);
        }
        return current;
    }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor current = wrap(graphics);
        extractRenderState(current, mouseX, mouseY, partialTick);
        current.flushTooltip();
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics.vanilla());
        super.render(graphics.vanilla(), mouseX, mouseY, partialTick);
        extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        extractBackground(wrap(graphics), mouseX, mouseY, partialTick);
    }

    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    protected final void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        extractLabels(wrap(graphics), mouseX, mouseY);
    }

    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics.vanilla(), mouseX, mouseY);
    }

    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics.vanilla(), mouseX, mouseY);
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseClicked(new MouseButtonEvent(mouseX, mouseY, button), false);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return keyPressed(new KeyEvent(key, scancode, modifiers));
    }

    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }
}
