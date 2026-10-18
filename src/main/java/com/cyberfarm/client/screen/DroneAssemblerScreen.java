package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.menu.DroneAssemblerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DroneAssemblerScreen extends MachineScreen<DroneAssemblerMenu> {
    private static final int PROGRESS_X = 82;
    private static final int PROGRESS_Y = 51;
    private static final int PROGRESS_WIDTH = 36;
    private static final int LOCKED = 0xC0101418;

    private Button assembleButton;

    public DroneAssemblerScreen(DroneAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
        energyBarHeight = 70;
    }

    @Override
    protected void init() {
        super.init();
        assembleButton = addRenderableWidget(Button.builder(Component.translatable("gui.cyberfarm.assembler.assemble"),
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DroneAssemblerMenu.BUTTON_ASSEMBLE))
                .bounds(leftPos + 76, topPos + 67, 70, 18)
                .build());
        updateButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButton();
    }

    private void updateButton() {
        assembleButton.active = menu.canAssemble() && !menu.isAssembling();
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.text(font, Component.translatable("gui.cyberfarm.assembler.frame"), x + 8, y + 34, TEXT_DIM, false);
        graphics.text(font, Component.translatable("gui.cyberfarm.assembler.modules"), x + 44, y + 17, TEXT_DIM, false);

        // Dim the module slots the current frame tier has not unlocked
        int unlocked = menu.getUnlockedModuleSlots();
        for (int i = unlocked; i < DroneAssemblerBlockEntity.MODULE_SLOTS; i++) {
            int sx = x + DroneAssemblerMenu.MODULE_SLOT_X;
            int sy = y + DroneAssemblerMenu.MODULE_SLOT_Y + i * 18;
            graphics.fill(sx, sy, sx + 16, sy + 16, LOCKED);
            graphics.fill(sx + 4, sy + 7, sx + 12, sy + 9, PANEL_LIGHT);
        }

        if (menu.getSlot(DroneAssemblerBlockEntity.FRAME_SLOT).getItem().getItem() instanceof DroneFrameItem frame) {
            graphics.text(font, Component.translatable("gui.cyberfarm.assembler.tier", frame.getTier()), x + 8, y + 66, ACCENT, false);
        }

        drawProgressBar(graphics, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_WIDTH, 6, menu.getProgress(), ACCENT);
        if (menu.isAssembling()) {
            int ox = x + DroneAssemblerMenu.OUTPUT_SLOT_X;
            int oy = y + DroneAssemblerMenu.OUTPUT_SLOT_Y;
            graphics.fill(ox - 2, oy - 2, ox + 18, oy - 1, ACCENT);
            graphics.fill(ox - 2, oy + 17, ox + 18, oy + 18, ACCENT);
        }
        graphics.text(font, Component.translatable("gui.cyberfarm.assembler.cost",
                DroneAssemblerBlockEntity.ASSEMBLY_TIME * DroneAssemblerBlockEntity.ENERGY_PER_TICK), x + 78, y + 30, TEXT_DIM, false);
    }
}
