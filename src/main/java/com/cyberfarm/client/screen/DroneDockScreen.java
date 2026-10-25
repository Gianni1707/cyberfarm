package com.cyberfarm.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.menu.DroneDockMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The dock at a glance: what the drone is doing and where it is, the cargo, the Filter and the
 * Return/Resume button. Details live in two side tabs that open to the left of the panel:
 * the drone's status (modules, area, route) and its battery.
 */
public class DroneDockScreen extends MachineScreen<DroneDockMenu> {
    private static final int BUTTON_Y = DroneDockMenu.CARGO_Y + 3 * 18 + 3;
    private static final int BUTTON_HEIGHT = 14;
    // Filter under the cargo grid, Return/Resume on the left
    private static final int FILTER_X = DroneDockMenu.CARGO_X - 1;
    private static final int FILTER_WIDTH = 3 * 18;
    private static final int RETURN_X = 8;
    private static final int RETURN_WIDTH = 60;

    private static final int TAB_SIZE = 22;
    private static final int TAB_GAP = 2;
    private static final int TAB_STATUS = 0;
    private static final int TAB_BATTERY = 1;
    /** Shown only for a Builder drone, or while a build is linked. */
    private static final int TAB_BUILD = 2;
    private static final int PANEL_WIDTH = 116;
    private static final int LINE = 10;
    /** The Build tab's blocks: icons with their counts, this many to a row. */
    private static final int BLOCK_CELL = 18;
    private static final int BLOCKS_PER_ROW = 5;

    private static final String[] STATUS_KEYS = {"none", "idle", "charging", "working", "returning"};
    /** Transport and Taxi drones follow a route or a destination, never the work area. */
    private static final int NO_AREA_MODULES = DroneTask.TRANSPORT.bit() | DroneTask.TAXI.bit() | DroneTask.BUILD.bit();

    /** The open side tab, or -1; kept between openings like other mods' side tabs. */
    private static int openTab = -1;

    public DroneDockScreen(DroneDockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, DroneDockMenu.INVENTORY_Y + 82);
        energyBarHeight = 70;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (int tab = 0; tab < tabCount(); tab++) {
                if (inTab(tab, event.x(), event.y())) {
                    openTab = openTab == tab ? -1 : tab;
                    playClick();
                    return true;
                }
            }
            if (isHovering(FILTER_X, BUTTON_Y, FILTER_WIDTH, BUTTON_HEIGHT, event.x(), event.y())) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DroneDockMenu.BUTTON_FILTER);
                playClick();
                return true;
            }
            if (isHovering(RETURN_X, BUTTON_Y, RETURN_WIDTH, BUTTON_HEIGHT, event.x(), event.y())) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DroneDockMenu.BUTTON_PAUSE);
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void playClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /** Clicks on the tabs and the open panel are not clicks outside the GUI (which would drop the carried item). */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return super.hasClickedOutside(mouseX, mouseY, left, top) && !inSideAreas(mouseX, mouseY);
    }

    // ---------------------------------------------------------------- side tabs layout

    private int tabX() {
        return leftPos - TAB_SIZE;
    }

    private int tabCount() {
        return (menu.getDroneModules() & DroneTask.BUILD.bit()) != 0 || menu.hasBuild() ? 3 : 2;
    }

    private int tabY(int tab) {
        return topPos + 4 + tab * (TAB_SIZE + TAB_GAP);
    }

    private boolean inTab(int tab, double mouseX, double mouseY) {
        return mouseX >= tabX() && mouseX < leftPos && mouseY >= tabY(tab) && mouseY < tabY(tab) + TAB_SIZE;
    }

    private Rect2i panelRect() {
        int lines = switch (openTab) {
            case TAB_STATUS -> statusLines().size();
            case TAB_BUILD -> buildLines().size();
            default -> 4;
        };
        int height = 12 + LINE + lines * LINE + (openTab == TAB_BATTERY ? 14 : 0)
                + (openTab == TAB_BUILD ? blockGridRows() * BLOCK_CELL + (menu.getBuildMissingMore() > 0 && !buildBlocks().isEmpty() ? LINE : 0) : 0);
        return new Rect2i(tabX() - PANEL_WIDTH + 1, topPos + 2, PANEL_WIDTH, height);
    }

    /** Screen areas the tabs and the open panel cover, for recipe viewers to keep clear of. */
    public List<Rect2i> sideAreas() {
        List<Rect2i> areas = new ArrayList<>();
        areas.add(new Rect2i(tabX(), tabY(0), TAB_SIZE, tabY(tabCount() - 1) + TAB_SIZE - tabY(0)));
        if (openTab >= 0) {
            areas.add(panelRect());
        }
        return areas;
    }

    private boolean inSideAreas(double mouseX, double mouseY) {
        for (Rect2i area : sideAreas()) {
            if (area.contains((int) mouseX, (int) mouseY)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void extractMachineBackground(GuiGraphicsExtractor graphics, int x, int y) {
        int status = menu.getDroneStatus();
        boolean docked = status == DroneEntity.STATUS_IDLE || status == DroneEntity.STATUS_CHARGING;

        graphics.text(font, Component.translatable("gui.cyberfarm.dock.work"), x + 8, y + 20, TEXT_DIM, false);
        graphics.text(font, workText(status, docked), x + 8, y + 30, workColor(status, docked), false);
        graphics.text(font, Component.translatable("gui.cyberfarm.dock.position"), x + 8, y + 46, TEXT_DIM, false);
        graphics.text(font, positionText(status, docked), x + 8, y + 56, status == DroneEntity.STATUS_NONE ? TEXT_DIM : TEXT, false);

        drawButton(graphics, x + FILTER_X, y + BUTTON_Y, FILTER_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.cyberfarm.dock.filter"), PANEL_LIGHT, TEXT);
        boolean paused = menu.isPaused();
        drawButton(graphics, x + RETURN_X, y + BUTTON_Y, RETURN_WIDTH, BUTTON_HEIGHT,
                Component.translatable(paused ? "gui.cyberfarm.dock.resume" : "gui.cyberfarm.dock.return"),
                paused ? ACCENT_DIM : PANEL_LIGHT, paused ? ACCENT : TEXT);

        // The Build tab goes away with the Builder drone
        if (openTab >= tabCount()) {
            openTab = -1;
        }
        if (openTab >= 0) {
            drawPanel(graphics);
        }
        for (int tab = 0; tab < tabCount(); tab++) {
            drawTab(graphics, tab);
        }
    }

    private Component workText(int status, boolean docked) {
        if (status != DroneEntity.STATUS_NONE && docked && menu.isPaused()) {
            return Component.translatable("gui.cyberfarm.dock.work.on_hold");
        }
        DroneTask task = menu.getDroneTask();
        if (status == DroneEntity.STATUS_WORKING && task != null) {
            return Component.translatable("gui.cyberfarm.dock.work." + task.name().toLowerCase(Locale.ROOT));
        }
        return Component.translatable("gui.cyberfarm.dock.status." + STATUS_KEYS[Math.max(0, Math.min(status, STATUS_KEYS.length - 1))]);
    }

    private int workColor(int status, boolean docked) {
        if (status != DroneEntity.STATUS_NONE && docked && menu.isPaused()) {
            return WARM;
        }
        return switch (status) {
            case DroneEntity.STATUS_WORKING -> ACCENT;
            case DroneEntity.STATUS_CHARGING, DroneEntity.STATUS_RETURNING -> WARM;
            case DroneEntity.STATUS_IDLE -> TEXT;
            default -> TEXT_DIM;
        };
    }

    private Component positionText(int status, boolean docked) {
        if (status == DroneEntity.STATUS_NONE) {
            return Component.literal("-");
        }
        if (docked) {
            return Component.translatable("gui.cyberfarm.dock.position.docked");
        }
        BlockPos pos = menu.getDronePos();
        return Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }

    private void drawTab(GuiGraphicsExtractor graphics, int tab) {
        int x = tabX();
        int y = tabY(tab);
        boolean open = openTab == tab;
        // Outline on three sides: the right side joins the panel, the left one the open side panel
        graphics.fill(x, y, leftPos + 1, y + TAB_SIZE, OUTLINE);
        graphics.fill(open ? x : x + 1, y + 1, leftPos + 1, y + TAB_SIZE - 1, open ? PANEL : PANEL_DARK);
        graphics.fill(open ? x : x + 1, y + 1, leftPos, y + 2, PANEL_LIGHT);

        int cx = x + TAB_SIZE / 2;
        int cy = y + TAB_SIZE / 2;
        if (tab == TAB_STATUS) {
            // An "i", in the colour of the drone's state
            int color = workColor(menu.getDroneStatus(), menu.getDroneStatus() == DroneEntity.STATUS_IDLE
                    || menu.getDroneStatus() == DroneEntity.STATUS_CHARGING);
            graphics.fill(cx - 1, cy - 6, cx + 1, cy - 4, color);
            graphics.fill(cx - 1, cy - 2, cx + 1, cy + 6, color);
            graphics.fill(cx - 2, cy + 5, cx + 2, cy + 6, color);
        } else if (tab == TAB_BUILD) {
            // A block under the ghost of the next one, cyan while there is something to build
            int color = !menu.hasBuild() ? TEXT_DIM : menu.getBuildRemaining() > 0 ? ACCENT : TEXT;
            graphics.fill(cx - 6, cy + 1, cx + 6, cy + 7, TEXT_DIM);
            graphics.fill(cx - 5, cy + 2, cx + 5, cy + 6, PANEL_LIGHT);
            for (int i = -5; i <= 4; i += 3) {
                graphics.fill(cx + i, cy - 7, cx + i + 2, cy - 6, color);
                graphics.fill(cx + i, cy - 2, cx + i + 2, cy - 1, color);
            }
            graphics.fill(cx - 5, cy - 6, cx - 4, cy - 2, color);
            graphics.fill(cx + 4, cy - 6, cx + 5, cy - 2, color);
        } else {
            // A battery with its charge
            graphics.fill(cx - 7, cy - 4, cx + 6, cy + 5, TEXT_DIM);
            graphics.fill(cx + 6, cy - 2, cx + 8, cy + 3, TEXT_DIM);
            graphics.fill(cx - 6, cy - 3, cx + 5, cy + 4, SLOT_BG);
            int percent = menu.getDroneStatus() == DroneEntity.STATUS_NONE ? 0 : menu.getDroneEnergyPercent();
            int filled = Math.round(11 * Math.min(percent, 100) / 100.0F);
            if (filled > 0) {
                graphics.fill(cx - 6, cy - 3, cx - 6 + filled, cy + 4, percent < 25 ? WARM : ACCENT);
            }
        }
    }

    private void drawPanel(GuiGraphicsExtractor graphics) {
        Rect2i rect = panelRect();
        int x = rect.getX();
        int y = rect.getY();
        graphics.fill(x, y, x + rect.getWidth(), y + rect.getHeight(), OUTLINE);
        graphics.fill(x + 1, y + 1, x + rect.getWidth() - 1, y + rect.getHeight() - 1, PANEL);
        graphics.fill(x + 1, y + 1, x + rect.getWidth() - 1, y + 2, PANEL_LIGHT);

        int textX = x + 7;
        int textY = y + 6;
        Component title = Component.translatable(switch (openTab) {
            case TAB_STATUS -> "gui.cyberfarm.dock.tab.status";
            case TAB_BUILD -> "gui.cyberfarm.dock.tab.build";
            default -> "gui.cyberfarm.dock.tab.battery";
        });
        graphics.text(font, title, textX, textY, ACCENT, false);
        graphics.fill(textX - 1, textY + 9, x + rect.getWidth() - 6, textY + 10, ACCENT_DIM);
        textY += LINE + 3;

        if (openTab == TAB_STATUS || openTab == TAB_BUILD) {
            for (Line line : openTab == TAB_STATUS ? statusLines() : buildLines()) {
                graphics.text(font, line.text(), textX, textY, line.color(), false);
                textY += LINE;
            }
        }
        if (openTab == TAB_BUILD) {
            List<DroneDockMenu.MissingBlock> blocks = buildBlocks();
            for (int i = 0; i < blocks.size(); i++) {
                DroneDockMenu.MissingBlock missing = blocks.get(i);
                int cellX = textX + (i % BLOCKS_PER_ROW) * BLOCK_CELL + 1;
                int cellY = textY + (i / BLOCKS_PER_ROW) * BLOCK_CELL + 1;
                drawSlot(graphics, cellX, cellY);
                ItemStack stack = new ItemStack(missing.item());
                graphics.item(stack, cellX, cellY);
                graphics.itemDecorations(font, stack, cellX, cellY, missing.count() >= 1000 ? missing.count() / 1000 + "k" : String.valueOf(missing.count()));
            }
            if (menu.getBuildMissingMore() > 0 && !blocks.isEmpty()) {
                graphics.text(font, Component.translatable("gui.cyberfarm.dock.build_more", menu.getBuildMissingMore()),
                        textX, textY + blockGridRows() * BLOCK_CELL + 1, TEXT_DIM, false);
            }
        } else if (openTab == TAB_BATTERY) {
            drawBattery(graphics, textX, textY, rect.getWidth() - 14);
        }
    }

    private record Line(Component text, int color) {
    }

    private List<Line> statusLines() {
        List<Line> lines = new ArrayList<>();
        int status = menu.getDroneStatus();
        boolean docked = status == DroneEntity.STATUS_IDLE || status == DroneEntity.STATUS_CHARGING;
        lines.add(new Line(workText(status, docked), workColor(status, docked)));
        if (status == DroneEntity.STATUS_NONE) {
            return lines;
        }
        lines.add(new Line(Component.translatable("gui.cyberfarm.dock.modules"), TEXT_DIM));
        int modules = menu.getDroneModules();
        for (DroneTask task : DroneTask.values()) {
            if ((modules & task.bit()) != 0) {
                lines.add(new Line(Component.literal(" ").append(Component.translatable("gui.cyberfarm.dock.module." + task.name().toLowerCase(Locale.ROOT))), TEXT));
            }
        }
        if ((modules & ~NO_AREA_MODULES) != 0) {
            lines.add(new Line(Component.translatable("gui.cyberfarm.dock.area", menu.getAreaWidth(), menu.getAreaDepth()), TEXT_DIM));
        }
        if ((modules & DroneTask.TRANSPORT.bit()) != 0) {
            lines.add(new Line(Component.translatable(menu.hasTransportRoute() ? "gui.cyberfarm.dock.route" : "gui.cyberfarm.dock.no_route"),
                    menu.hasTransportRoute() ? TEXT : WARM));
        }
        if ((modules & DroneTask.TAXI.bit()) != 0) {
            lines.add(new Line(Component.translatable(menu.hasTaxiDestination() ? "gui.cyberfarm.dock.taxi" : "gui.cyberfarm.dock.no_taxi"),
                    menu.hasTaxiDestination() ? TEXT : WARM));
        }
        if ((modules & DroneTask.BUILD.bit()) != 0) {
            if (!menu.hasBuild()) {
                lines.add(new Line(Component.translatable("gui.cyberfarm.dock.no_build"), WARM));
            } else if (menu.getBuildRemaining() == 0) {
                lines.add(new Line(Component.translatable("gui.cyberfarm.dock.build_done"), TEXT));
            } else {
                lines.add(new Line(Component.translatable("gui.cyberfarm.dock.build", menu.getBuildRemaining()), TEXT));
            }
        }
        if (menu.isQuarryStarted()) {
            lines.add(new Line(Component.translatable("gui.cyberfarm.dock.quarry", menu.getQuarryLayer()), TEXT_DIM));
        }
        return lines;
    }

    /** The Build tab's text: the layer being built, what is left and what is in the way; the blocks follow as a grid. */
    private List<Line> buildLines() {
        List<Line> lines = new ArrayList<>();
        if (!menu.hasBuild()) {
            lines.add(new Line(Component.translatable("gui.cyberfarm.dock.no_build"), WARM));
            return lines;
        }
        if (menu.getBuildRemaining() == 0) {
            lines.add(new Line(Component.translatable("gui.cyberfarm.dock.build_done"), TEXT));
            return lines;
        }
        if (menu.getBuildLayer() > 0) {
            lines.add(new Line(Component.translatable("gui.cyberfarm.dock.build_layer", menu.getBuildLayer(), menu.getBuildLayers()), TEXT));
        }
        lines.add(new Line(Component.translatable("gui.cyberfarm.dock.build_left", menu.getBuildRemaining()), TEXT_DIM));
        if (menu.getBuildObstacles() > 0) {
            lines.add(new Line(Component.translatable("gui.cyberfarm.dock.build_obstacles", menu.getBuildObstacles()), WARM));
        }
        if (!buildBlocks().isEmpty()) {
            // With no layer left the drones can build, what is left is for the player to place
            lines.add(new Line(Component.translatable(menu.getBuildLayer() > 0 ? "gui.cyberfarm.dock.build_needs" : "gui.cyberfarm.dock.build_by_hand"),
                    menu.getBuildLayer() > 0 ? ACCENT : WARM));
        }
        return lines;
    }

    private List<DroneDockMenu.MissingBlock> buildBlocks() {
        return menu.hasBuild() && menu.getBuildRemaining() > 0 ? menu.getBuildMissing() : List.of();
    }

    private int blockGridRows() {
        return (buildBlocks().size() + BLOCKS_PER_ROW - 1) / BLOCKS_PER_ROW;
    }

    private void drawBattery(GuiGraphicsExtractor graphics, int x, int y, int width) {
        int status = menu.getDroneStatus();
        if (status == DroneEntity.STATUS_NONE) {
            graphics.text(font, Component.translatable("gui.cyberfarm.dock.status.none"), x, y, TEXT_DIM, false);
            return;
        }
        int percent = menu.getDroneEnergyPercent();
        drawProgressBar(graphics, x, y, width, 8, percent / 100.0F, percent < 25 ? WARM : ACCENT);
        y += 14;
        graphics.text(font, percent + "%", x, y, TEXT, false);
        y += LINE;
        graphics.text(font, Component.translatable("gui.cyberfarm.dock.battery.fe", compact(menu.getDroneEnergy()), compact(menu.getDroneCapacity())),
                x, y, TEXT_DIM, false);
        y += LINE;
        String state = switch (status) {
            case DroneEntity.STATUS_CHARGING -> "charging";
            case DroneEntity.STATUS_IDLE -> "docked";
            default -> "flying";
        };
        graphics.text(font, Component.translatable("gui.cyberfarm.dock.battery." + state),
                x, y, status == DroneEntity.STATUS_CHARGING ? WARM : TEXT_DIM, false);
    }

    /** 43,200 → "43.2k", so the biggest batteries still fit the panel. */
    private static String compact(int value) {
        return value >= 10_000 ? String.format("%.1fk", value / 1000.0) : String.format("%,d", value);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (isHovering(FILTER_X, BUTTON_Y, FILTER_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.dock.filter.hint"), mouseX, mouseY);
        } else if (isHovering(RETURN_X, BUTTON_Y, RETURN_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(menu.isPaused()
                    ? "gui.cyberfarm.dock.resume.hint" : "gui.cyberfarm.dock.return.hint"), mouseX, mouseY);
        } else if (inTab(TAB_STATUS, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.dock.tab.status"), mouseX, mouseY);
        } else if (inTab(TAB_BATTERY, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.dock.tab.battery"), mouseX, mouseY);
        } else if (tabCount() > TAB_BUILD && inTab(TAB_BUILD, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.cyberfarm.dock.tab.build"), mouseX, mouseY);
        } else if (openTab == TAB_BUILD) {
            // A block of the grid: its name
            Rect2i rect = panelRect();
            int gridX = rect.getX() + 7;
            int gridY = rect.getY() + 6 + LINE + 3 + buildLines().size() * LINE;
            List<DroneDockMenu.MissingBlock> blocks = buildBlocks();
            int column = mouseX >= gridX ? (mouseX - gridX) / BLOCK_CELL : -1;
            int index = mouseY >= gridY && column >= 0 && column < BLOCKS_PER_ROW ? (mouseY - gridY) / BLOCK_CELL * BLOCKS_PER_ROW + column : -1;
            if (index >= 0 && index < blocks.size()) {
                graphics.setTooltipForNextFrame(font, new ItemStack(blocks.get(index).item()).getHoverName(), mouseX, mouseY);
            }
        }
    }
}
