package com.cyberfarm.client.screen;

import com.cyberfarm.block.entity.ReactorControllerBlockEntity;
import com.cyberfarm.menu.ReactorMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * Left: water and coolant tanks. Middle: fuel rods, the core, waste, the temperature bar and the
 * power controls. Right: the steam tank.
 */
public class ReactorScreen extends MachineScreen<ReactorMenu> {
    private static final int WATER_X = 8;
    private static final int COOLANT_X = 28;
    private static final int STEAM_X = 152;
    private static final int TANK_Y = 18;
    private static final int TANK_HEIGHT = 76;
    private static final int CORE_X = 90;
    private static final int TEMP_X = 50;
    private static final int TEMP_Y = 60;
    private static final int TEMP_WIDTH = 96;
    private static final int BUTTON_Y = 84;
    private static final int BUTTON_SIZE = 11;
    private static final int MINUS_X = 50;
    private static final int PLUS_X = 135;
    private static final int DANGER = 0xFFE5483B;

    public ReactorScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 196);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        int cap = ReactorControllerBlockEntity.TANK_CAPACITY;
        drawTank(graphics, x + WATER_X, y + TANK_Y, TANK_HEIGHT, menu.getWater(), cap, menu.getWaterFluid());
        drawTank(graphics, x + COOLANT_X, y + TANK_Y, TANK_HEIGHT, menu.getCoolant(), cap, menu.getCoolantFluid());
        drawTank(graphics, x + STEAM_X, y + TANK_Y, TANK_HEIGHT, menu.getSteam(), cap, menu.getSteamFluid());

        // Core: one bar per rod, cyan while it burns
        int cx = x + CORE_X;
        int cy = y + ReactorMenu.SLOTS_Y;
        graphics.fill(cx - 1, cy - 1, cx + 17, cy + 35, SLOT_BORDER);
        graphics.fill(cx, cy, cx + 16, cy + 34, SLOT_BG);
        boolean burning = menu.getHeatProduced() > 0;
        for (int i = 0; i < ReactorControllerBlockEntity.FUEL_SLOTS; i++) {
            int bx = cx + 2 + i * 4;
            graphics.fill(bx, cy + 3, bx + 2, cy + 31, burning && i < menu.getActiveRods() ? ACCENT : PANEL_LIGHT);
        }

        // Temperature: green, then amber past half, red past the alarm threshold
        int temperature = menu.getTemperature();
        boolean alarm = temperature >= ReactorControllerBlockEntity.ALARM_PERMILLE;
        int barColor = alarm ? DANGER : temperature >= 500 ? WARM : 0xFF3FBF6F;
        drawProgressBar(graphics, x + TEMP_X, y + TEMP_Y, TEMP_WIDTH, 6, temperature / 1000.0F, barColor);
        int alarmX = x + TEMP_X + TEMP_WIDTH * ReactorControllerBlockEntity.ALARM_PERMILLE / 1000;
        graphics.fill(alarmX, y + TEMP_Y - 2, alarmX + 1, y + TEMP_Y + 8, DANGER);

        Component status;
        int color;
        if (!menu.isFormed()) {
            status = Component.translatable("gui.cyberfarm.reactor.incomplete");
            color = WARM;
        } else if (alarm) {
            status = Component.translatable("gui.cyberfarm.reactor.overheating");
            // Blinks while the alarm sounds
            color = (System.currentTimeMillis() / 400) % 2 == 0 ? DANGER : WARM;
        } else if (menu.getBoost() == 0) {
            status = Component.translatable("gui.cyberfarm.reactor.off");
            color = TEXT_DIM;
        } else if (menu.getActiveRods() == 0) {
            status = Component.translatable("gui.cyberfarm.reactor.idle");
            color = TEXT_DIM;
        } else {
            status = Component.translatable("gui.cyberfarm.reactor.running", menu.getActiveRods());
            color = ACCENT;
        }
        graphics.text(font, status, x + TEMP_X, y + 70, color, false);

        // Power controls: [-] 250% [+]
        drawButton(graphics, x + MINUS_X, y + BUTTON_Y, "-");
        drawButton(graphics, x + PLUS_X, y + BUTTON_Y, "+");
        Component boost = menu.getBoost() == 0
                ? Component.translatable("gui.cyberfarm.reactor.power_off")
                : Component.translatable("gui.cyberfarm.reactor.power", menu.getBoost());
        int middle = x + (MINUS_X + BUTTON_SIZE + PLUS_X) / 2;
        graphics.text(font, boost, middle - font.width(boost) / 2, y + BUTTON_Y + 2, TEXT, false);
    }

    private void drawButton(GuiGraphicsExtractor graphics, int bx, int by, String label) {
        graphics.fill(bx - 1, by - 1, bx + BUTTON_SIZE + 1, by + BUTTON_SIZE + 1, SLOT_BORDER);
        graphics.fill(bx, by, bx + BUTTON_SIZE, by + BUTTON_SIZE, PANEL_LIGHT);
        graphics.text(font, Component.literal(label), bx + (BUTTON_SIZE - font.width(label)) / 2 + 1, by + 2, ACCENT, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int button = isHovering(MINUS_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, event.x(), event.y()) ? ReactorMenu.BUTTON_BOOST_DOWN
                    : isHovering(PLUS_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, event.x(), event.y()) ? ReactorMenu.BUTTON_BOOST_UP : -1;
            if (button >= 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int cap = ReactorControllerBlockEntity.TANK_CAPACITY;
        tankTooltip(graphics, mouseX, mouseY, WATER_X, TANK_Y, TANK_HEIGHT, menu.getWater(), cap, menu.getWaterFluid());
        tankTooltip(graphics, mouseX, mouseY, COOLANT_X, TANK_Y, TANK_HEIGHT, menu.getCoolant(), cap, menu.getCoolantFluid());
        tankTooltip(graphics, mouseX, mouseY, STEAM_X, TANK_Y, TANK_HEIGHT, menu.getSteam(), cap, menu.getSteamFluid());
        if (isHovering(TEMP_X - 1, TEMP_Y - 1, TEMP_WIDTH + 2, 8, mouseX, mouseY)) {
            int celsius = 25 + menu.getTemperature() * 1175 / 1000;
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.reactor.temperature",
                    celsius, menu.getHeatProduced(), menu.getTransferCapacity(), menu.getSteamRate()), mouseX, mouseY);
        }
        if (isHovering(CORE_X - 1, ReactorMenu.SLOTS_Y - 1, 18, 36, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.reactor.core",
                    menu.getHeatProduced(), menu.getSteamRate()), mouseX, mouseY);
        }
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < ReactorControllerBlockEntity.FUEL_SLOTS + ReactorControllerBlockEntity.WASTE_SLOTS) {
            String key = hoveredSlot.index < ReactorControllerBlockEntity.FUEL_SLOTS ? "fuel" : "waste";
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.reactor.slot." + key), mouseX, mouseY);
        }
    }
}
