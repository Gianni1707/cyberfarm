package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.menu.DroneAssemblerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

public class DroneAssemblerScreen extends MachineScreen<DroneAssemblerMenu> {
    private static final int PROGRESS_X = 106;
    private static final int PROGRESS_Y = 38;
    private static final int PROGRESS_WIDTH = 20;
    private static final int BUTTON_X = 98;
    private static final int BUTTON_Y = 64;
    private static final int BUTTON_WIDTH = 58;
    private static final int BUTTON_HEIGHT = 18;

    public DroneAssemblerScreen(DroneAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
        flatBackground("drone_assembler");
        energyBarHeight = 64;
    }

    private boolean canPress() {
        return !menu.isAssembling() && (menu.canDisassemble() || menu.canAssemble());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && canPress() && isHovering(BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, event.x(), event.y())) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                    menu.canDisassemble() ? DroneAssemblerMenu.BUTTON_DISASSEMBLE : DroneAssemblerMenu.BUTTON_ASSEMBLE);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        DroneFrameItem frame = menu.getFrame();

        // One button: Assemble, or Disassemble when a finished drone sits alone in the output slot
        boolean active = canPress();
        // Assembly waits for power without any other hint, so the button says it
        boolean noPower = menu.isAssembling() && menu.getEnergy() < DroneAssemblerBlockEntity.ENERGY_PER_TICK;
        Component label = Component.translatable(noPower ? "gui.cyberfarm.no_power"
                : menu.canDisassemble() ? "gui.cyberfarm.assembler.disassemble" : "gui.cyberfarm.assembler.assemble");
        drawButton(graphics, x + BUTTON_X, y + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, label, active ? PANEL_LIGHT : PANEL, noPower ? WARM : active ? TEXT : TEXT_DIM);

        // Dim the slots the current frame does not allow
        int unlockedModules = frame != null ? frame.getModuleSlots() : 0;
        for (int i = unlockedModules; i < DroneAssemblerBlockEntity.MODULE_SLOTS; i++) {
            drawLockedSlot(graphics, x + DroneAssemblerMenu.MODULE_SLOT_X, y + DroneAssemblerMenu.MODULE_SLOT_Y + i * 18);
        }
        if (frame == null) {
            for (int i = 0; i < DroneAssemblerBlockEntity.ADDON_SLOTS; i++) {
                drawLockedSlot(graphics, x + DroneAssemblerMenu.ADDON_SLOT_X + (i % 2) * 18, y + DroneAssemblerMenu.ADDON_SLOT_Y + (i / 2) * 18);
            }
        } else {
            drawNotice(graphics, x + 20, y + 56, Component.translatable("gui.cyberfarm.assembler.tier", frame.getTier()), ACCENT);
        }

        int addonLimit = frame != null ? frame.getAddonLimit() : 0;
        int addons = menu.getAddonCount();
        Component count = Component.translatable("gui.cyberfarm.assembler.addons", addons, addonLimit);
        graphics.fill(x + 60, y + 15, x + 68 + font.width(count), y + 25, 0xFF1B2B2B);
        graphics.text(font, count, x + 64, y + 16, addons > addonLimit ? WARM : TEXT, false);

        // The welding head running along its gantry over the drone being built
        float progress = menu.getProgress();
        int head = x + 134 + Math.round(progress * 12);
        drawSteelBar(graphics, head - 2, y + 20, head + 4, y + 26);
        drawSteelRod(graphics, head, y + 26, head + 2, y + 31);
        if (menu.isAssembling() && Util.getMillis() / 120 % 2 == 0) {
            graphics.fill(head, y + 31, head + 2, y + 33, GLOW);
        }
        if (menu.isAssembling()) {
            int ox = x + DroneAssemblerMenu.OUTPUT_SLOT_X;
            int oy = y + DroneAssemblerMenu.OUTPUT_SLOT_Y;
            graphics.fill(ox - 2, oy - 2, ox + 18, oy - 1, ACCENT);
            graphics.fill(ox - 2, oy + 17, ox + 18, oy + 18, ACCENT);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index <= DroneAssemblerBlockEntity.OUTPUT_SLOT) {
            int index = hoveredSlot.index;
            String key = index == DroneAssemblerBlockEntity.FRAME_SLOT ? "frame"
                    : DroneAssemblerBlockEntity.isModuleSlot(index) ? "module"
                    : DroneAssemblerBlockEntity.isAddonSlot(index) ? "addon"
                    : "output";
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.assembler.slot." + key), mouseX, mouseY);
        }
        if (isHovering(PROGRESS_X - 1, PROGRESS_Y - 1, PROGRESS_WIDTH + 2, 8, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.assembler.cost",
                    DroneAssemblerBlockEntity.ASSEMBLY_TIME * DroneAssemblerBlockEntity.ENERGY_PER_TICK), mouseX, mouseY);
        }
    }
}
