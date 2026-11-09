package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.ElectrolyzerBlockEntity;
import com.cyberfarm.menu.ElectrolyzerMenu;
import com.cyberfarm.registry.ModItems;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

/** Alumina in on the left, Aluminum Ingots out on the right. */
public class ElectrolyzerScreen extends MachineScreen<ElectrolyzerMenu> {
    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        flatBackground("electrolyzer");
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        float progress = menu.getProgress();
        int ax = x + 5;
        // The aluminium gathering on the floor of the bath
        int pool = Math.round(13 * progress);
        if (pool > 0) {
            graphics.fill(ax + 4, y + 59 - pool, ax + 32, y + 59, 0xFF7C8989);
            graphics.fill(ax + 4, y + 59 - pool, ax + 32, y + 60 - pool, STEEL_TOP);
        }
        if (progress > 0) {
            // A grain falling from the hopper, the arc between the electrodes, bubbles in the melt
            int grain = y + 24 + (int) (Util.getMillis() / 50 % 18);
            graphics.fill(ax + 17, grain, ax + 19, grain + 2, 0xFFFFFFFF);
            int[] arc = {0, 1, 0, 2, 1, 2};
            for (int i = 0; i < arc.length; i++) {
                graphics.fill(ax + 13 + i * 2, y + 38 + arc[i], ax + 15 + i * 2, y + 39 + arc[i], GLOW);
            }
            drawBubbles(graphics, ax + 13, y + 43, 10, 12, 0xFFFFFFFF);
        }
        drawGhost(graphics, ElectrolyzerBlockEntity.INPUT, new ItemStack(ModItems.ALUMINA.get()));
        drawGhost(graphics, ElectrolyzerBlockEntity.OUTPUT, new ItemStack(ModItems.ALUMINUM_INGOT.get()));
        if (menu.getEnergyCapacity() > 0 && menu.getEnergy() < ElectrolyzerBlockEntity.ENERGY_PER_TICK && progress == 0) {
            drawNotice(graphics, x + 97, y + 56, Component.translatable("gui.cyberfarm.no_power"), WARM);
        }
    }
}
