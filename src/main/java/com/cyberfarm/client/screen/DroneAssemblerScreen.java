package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.menu.DroneAssemblerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DroneAssemblerScreen extends MachineScreen<DroneAssemblerMenu> {
    private static final int PROGRESS_X = 106;
    private static final int PROGRESS_Y = 42;
    private static final int PROGRESS_WIDTH = 20;
    private static final int LOCKED = 0xC0101418;

    private Button assembleButton;

    public DroneAssemblerScreen(DroneAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
        energyBarHeight = 70;
    }

    @Override
    protected void init() {
        super.init();
        // One button: Assemble, or Disassemble when a finished drone sits alone in the output slot
        assembleButton = addRenderableWidget(Button.builder(Component.translatable("gui.cyberfarm.assembler.assemble"),
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        menu.canDisassemble() ? DroneAssemblerMenu.BUTTON_DISASSEMBLE : DroneAssemblerMenu.BUTTON_ASSEMBLE))
                .bounds(leftPos + 100, topPos + 62, 56, 18)
                .build());
        updateButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButton();
    }

    private void updateButton() {
        boolean disassemble = menu.canDisassemble();
        assembleButton.setMessage(Component.translatable(disassemble ? "gui.cyberfarm.assembler.disassemble" : "gui.cyberfarm.assembler.assemble"));
        assembleButton.active = !menu.isAssembling() && (disassemble || menu.canAssemble());
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        DroneFrameItem frame = menu.getFrame();

        // Dim the slots the current frame does not allow
        int unlockedModules = frame != null ? frame.getModuleSlots() : 0;
        for (int i = unlockedModules; i < DroneAssemblerBlockEntity.MODULE_SLOTS; i++) {
            dimSlot(graphics, x + DroneAssemblerMenu.MODULE_SLOT_X, y + DroneAssemblerMenu.MODULE_SLOT_Y + i * 18);
        }
        if (frame == null) {
            for (int i = 0; i < DroneAssemblerBlockEntity.ADDON_SLOTS; i++) {
                dimSlot(graphics, x + DroneAssemblerMenu.ADDON_SLOT_X + (i % 2) * 18, y + DroneAssemblerMenu.ADDON_SLOT_Y + (i / 2) * 18);
            }
        } else {
            graphics.text(font, Component.translatable("gui.cyberfarm.assembler.tier", frame.getTier()), x + 8, y + 58, ACCENT, false);
        }

        int addonLimit = frame != null ? frame.getAddonLimit() : 0;
        int addons = menu.getAddonCount();
        graphics.text(font, Component.translatable("gui.cyberfarm.assembler.addons", addons, addonLimit),
                x + DroneAssemblerMenu.ADDON_SLOT_X - 2, y + 17, addons > addonLimit ? WARM : TEXT_DIM, false);

        drawProgressBar(graphics, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_WIDTH, 6, menu.getProgress(), ACCENT);
        if (menu.isAssembling()) {
            int ox = x + DroneAssemblerMenu.OUTPUT_SLOT_X;
            int oy = y + DroneAssemblerMenu.OUTPUT_SLOT_Y;
            graphics.fill(ox - 2, oy - 2, ox + 18, oy - 1, ACCENT);
            graphics.fill(ox - 2, oy + 17, ox + 18, oy + 18, ACCENT);
        }
    }

    private static void dimSlot(GuiGraphicsExtractor graphics, int sx, int sy) {
        graphics.fill(sx, sy, sx + 16, sy + 16, LOCKED);
        graphics.fill(sx + 4, sy + 7, sx + 12, sy + 9, PANEL_LIGHT);
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
