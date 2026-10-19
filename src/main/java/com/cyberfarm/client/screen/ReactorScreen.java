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
 * Left: water and coolant tanks. Middle: 8 fuel slots (the ones a smaller tower does not use are
 * dimmed), the core, the waste row, the temperature bar and the power controls. Right: steam.
 */
public class ReactorScreen extends MachineScreen<ReactorMenu> {
    private static final int WATER_X = 8;
    private static final int COOLANT_X = 28;
    private static final int STEAM_X = 152;
    private static final int TANK_Y = 18;
    private static final int TANK_HEIGHT = 98;
    private static final int CORE_X = 125;
    private static final int CORE_Y = 20;
    private static final int TEMP_X = 50;
    private static final int TEMP_Y = 86;
    private static final int TEMP_WIDTH = 96;
    private static final int BUTTON_Y = 106;
    private static final int BUTTON_SIZE = 11;
    private static final int MINUS_X = 50;
    private static final int PLUS_X = 135;
    private static final int DANGER = 0xFFE5483B;
    private static final int LOCKED = 0xC0101418;
    private static final String[] SIZE_NAMES = {"", "small", "medium", "large"};

    public ReactorScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 218);
    }

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        int cap = ReactorControllerBlockEntity.TANK_CAPACITY;
        drawTank(graphics, x + WATER_X, y + TANK_Y, TANK_HEIGHT, menu.getWater(), cap, menu.getWaterFluid());
        drawTank(graphics, x + COOLANT_X, y + TANK_Y, TANK_HEIGHT, menu.getCoolant(), cap, menu.getCoolantFluid());
        drawTank(graphics, x + STEAM_X, y + TANK_Y, TANK_HEIGHT, menu.getSteam(), cap, menu.getSteamFluid());

        // Fuel slots the tower does not burn
        int usable = menu.getSize() == 0 ? ReactorControllerBlockEntity.FUEL_SLOTS : menu.getUsableRods();
        for (int i = usable; i < ReactorControllerBlockEntity.FUEL_SLOTS; i++) {
            int sx = x + ReactorMenu.FUEL_X + (i % 4) * 18;
            int sy = y + ReactorMenu.FUEL_Y + (i / 4) * 18;
            graphics.fill(sx, sy, sx + 16, sy + 16, LOCKED);
            graphics.fill(sx + 4, sy + 7, sx + 12, sy + 9, PANEL_LIGHT);
        }

        // Core: one bar per rod the tower burns, cyan while burning
        int cx = x + CORE_X;
        int cy = y + CORE_Y;
        graphics.fill(cx - 1, cy - 1, cx + 22, cy + 35, SLOT_BORDER);
        graphics.fill(cx, cy, cx + 21, cy + 34, SLOT_BG);
        boolean burning = menu.getHeatProduced() > 0;
        for (int i = 0; i < ReactorControllerBlockEntity.FUEL_SLOTS; i++) {
            int bx = cx + 1 + i * 2 + i / 2;
            int color = i >= usable ? SLOT_BORDER : burning && i < menu.getActiveRods() ? ACCENT : PANEL_LIGHT;
            graphics.fill(bx, cy + 3, bx + 2, cy + 31, color);
        }

        graphics.text(font, Component.translatable("gui.cyberfarm.reactor.waste"), x + ReactorMenu.WASTE_X + 74, y + ReactorMenu.WASTE_Y + 4, TEXT_DIM, false);

        // Temperature: green, then amber past half, red past the alarm threshold
        int temperature = menu.getTemperature();
        boolean alarm = temperature >= ReactorControllerBlockEntity.ALARM_PERMILLE;
        int barColor = alarm ? DANGER : temperature >= 500 ? WARM : 0xFF3FBF6F;
        drawProgressBar(graphics, x + TEMP_X, y + TEMP_Y, TEMP_WIDTH, 6, temperature / 1000.0F, barColor);
        int alarmX = x + TEMP_X + TEMP_WIDTH * ReactorControllerBlockEntity.ALARM_PERMILLE / 1000;
        graphics.fill(alarmX, y + TEMP_Y - 2, alarmX + 1, y + TEMP_Y + 8, DANGER);

        Component status;
        int color;
        if (menu.getSize() == 0) {
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
        graphics.text(font, status, x + TEMP_X, y + 95, color, false);

        // Power controls: [-] 250% [+], with the tower's maximum
        drawButton(graphics, x + MINUS_X, y + BUTTON_Y, "-");
        drawButton(graphics, x + PLUS_X, y + BUTTON_Y, "+");
        Component boost = menu.getBoost() == 0
                ? Component.translatable("gui.cyberfarm.reactor.power_off")
                : Component.translatable("gui.cyberfarm.reactor.power", menu.getBoost());
        int middle = x + (MINUS_X + BUTTON_SIZE + PLUS_X) / 2;
        graphics.text(font, boost, middle - font.width(boost) / 2, y + BUTTON_Y + 2, TEXT, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        if (menu.getSize() > 0) {
            Component size = Component.translatable("gui.cyberfarm.reactor.size." + SIZE_NAMES[menu.getSize()]);
            graphics.text(font, size, imageWidth - 8 - font.width(size), titleLabelY, TEXT_DIM, false);
        }
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
        if (isHovering(CORE_X - 1, CORE_Y - 1, 23, 36, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.reactor.core",
                    menu.getHeatProduced(), menu.getSteamRate()), mouseX, mouseY);
        }
        if (isHovering(MINUS_X, BUTTON_Y, PLUS_X + BUTTON_SIZE - MINUS_X, BUTTON_SIZE, mouseX, mouseY) && menu.getSize() > 0) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.reactor.max_power", menu.getMaxBoost()), mouseX, mouseY);
        }
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < ReactorControllerBlockEntity.FUEL_SLOTS + ReactorControllerBlockEntity.WASTE_SLOTS) {
            boolean fuel = hoveredSlot.index < ReactorControllerBlockEntity.FUEL_SLOTS;
            boolean unused = fuel && menu.getSize() > 0 && hoveredSlot.index >= menu.getUsableRods();
            String key = unused ? "unused" : fuel ? "fuel" : "waste";
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.reactor.slot." + key), mouseX, mouseY);
        }
    }
}
