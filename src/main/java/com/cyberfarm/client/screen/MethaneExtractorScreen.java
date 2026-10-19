package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;
import com.cyberfarm.menu.MethaneExtractorMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MethaneExtractorScreen extends MachineScreen<MethaneExtractorMenu> {
    private static final int TANK_X = 152;
    private static final int TANK_Y = 17;
    private static final int TANK_HEIGHT = 52;

    public MethaneExtractorScreen(MethaneExtractorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        boolean formed = menu.isFormed();
        graphics.text(font, Component.translatable(formed ? "gui.cyberfarm.extractor.formed" : "gui.cyberfarm.extractor.incomplete"),
                x + 8, y + 22, formed ? ACCENT : WARM, false);
        if (formed) {
            graphics.text(font, Component.translatable("gui.cyberfarm.extractor.mobs", menu.getMobCount()), x + 8, y + 36, TEXT, false);
            graphics.text(font, Component.translatable("gui.cyberfarm.mb_per_tick", menu.getRate()), x + 8, y + 48, TEXT, false);
        } else {
            graphics.text(font, Component.translatable("gui.cyberfarm.extractor.hint"), x + 8, y + 36, TEXT_DIM, false);
        }
        drawTank(graphics, x + TANK_X, y + TANK_Y, TANK_HEIGHT, menu.getTankAmount(), MethaneExtractorBlockEntity.TANK_CAPACITY, menu.getTankFluid());
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        tankTooltip(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_HEIGHT, menu.getTankAmount(), MethaneExtractorBlockEntity.TANK_CAPACITY, menu.getTankFluid());
    }
}
