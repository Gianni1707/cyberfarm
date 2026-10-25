package com.cyberfarm.client.screen;

import static com.cyberfarm.client.screen.MachineScreen.ACCENT;
import static com.cyberfarm.client.screen.MachineScreen.ACCENT_DIM;
import static com.cyberfarm.client.screen.MachineScreen.OUTLINE;
import static com.cyberfarm.client.screen.MachineScreen.PANEL;
import static com.cyberfarm.client.screen.MachineScreen.PANEL_DARK;
import static com.cyberfarm.client.screen.MachineScreen.PANEL_LIGHT;
import static com.cyberfarm.client.screen.MachineScreen.SLOT_BG;
import static com.cyberfarm.client.screen.MachineScreen.SLOT_BORDER;
import static com.cyberfarm.client.screen.MachineScreen.TEXT;
import static com.cyberfarm.client.screen.MachineScreen.TEXT_DIM;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cyberfarm.item.ProjectorSelectPayload;
import com.cyberfarm.multiblock.Blueprint;
import com.cyberfarm.multiblock.BuildPlan;
import com.cyberfarm.multiblock.Structure;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Multiblock Projector's list of structures, the mod's multiblocks and then the schematics:
 * pick one on the left (the wheel scrolls a long list), read what it is and see the blocks it takes
 * on the right; the "i" in the corner says how to use the projector. Picking one unlocks the
 * projection, so it follows the player's aim again. Opens like a hologram: a beam stretches across,
 * then sweeps down and leaves the panel behind it.
 */
public class ProjectorScreen extends Screen {
    private static final int WIDTH = 248;
    private static final int LIST_X = 6;
    private static final int LIST_WIDTH = 112;
    private static final int TOP = 20;
    private static final int ROW = 14;
    private static final int VISIBLE_ROWS = 12;
    private static final int PANEL_HEIGHT = TOP + VISIBLE_ROWS * ROW + 6;
    private static final int SIDE_X = 126;
    private static final int SIDE_WIDTH = WIDTH - SIDE_X - 6;
    private static final int LINE = 10;
    private static final int MAX_DESCRIPTION_LINES = 4;
    private static final int CELL = 18;
    private static final int CELLS_PER_ROW = SIDE_WIDTH / CELL;
    private static final int INFO_SIZE = 11;
    private static final int HINT_WIDTH = 180;
    /** Opening animation, in milliseconds: the beam stretches across, then sweeps the panel down. */
    private static final float STRETCH_MS = 140.0F;
    private static final float SWEEP_MS = 280.0F;

    private final InteractionHand hand;
    private final long openedAt = Util.getMillis();
    private final List<Structure> structures = Structure.all();
    private Structure selected;
    private List<Map.Entry<Item, Integer>> materials = List.of();
    private int scroll;
    private int left;
    private int top;

    private ProjectorScreen(InteractionHand hand, Structure selected) {
        super(Component.translatable("item.cyberfarm.multiblock_projector"));
        this.hand = hand;
        this.selected = selected;
    }

    public static void open(InteractionHand hand, Structure selected) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_POWER_SELECT, 1.8F, 0.4F));
        minecraft.setScreen(new ProjectorScreen(hand, selected));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        // The chosen structure in view
        int index = Math.max(0, structures.indexOf(selected));
        scroll = Mth.clamp(index - VISIBLE_ROWS / 2, 0, maxScroll());
        select(selected);
    }

    private int maxScroll() {
        return Math.max(0, structures.size() - VISIBLE_ROWS);
    }

    private void select(Structure structure) {
        selected = structure;
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Structure.Ghost ghost : structure.ghosts(BlockPos.ZERO, Direction.NORTH)) {
            if (ghost.show() != null && BuildPlan.cost(ghost.show()) != Items.AIR) {
                counts.merge(BuildPlan.cost(ghost.show()), 1, Integer::sum);
            }
        }
        materials = new ArrayList<>(counts.entrySet());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float elapsed = Util.getMillis() - openedAt;
        float stretch = ease(elapsed / STRETCH_MS);
        float sweep = ease((elapsed - STRETCH_MS) / SWEEP_MS);
        if (sweep <= 0.0F) {
            int half = Math.max(1, (int) (WIDTH / 2 * stretch));
            int centre = left + WIDTH / 2;
            graphics.fill(centre - half, top, centre + half, top + 2, ACCENT);
            return;
        }
        int shown = Math.max(2, (int) (PANEL_HEIGHT * sweep));
        graphics.enableScissor(left, top, left + WIDTH, top + shown);
        drawPanel(graphics, mouseX, mouseY);
        graphics.disableScissor();
        if (sweep < 1.0F) {
            // The beam's leading edge, with a soft glow over what it just drew
            int edge = top + shown;
            graphics.fillGradient(left, edge - 8, left + WIDTH, edge, 0x002FE0FF, 0x662FE0FF);
            graphics.fill(left, edge - 1, left + WIDTH, edge + 1, ACCENT);
            return;
        }
        if (isOverInfo(mouseX, mouseY)) {
            List<FormattedCharSequence> hint = font.split(Component.translatable("gui.cyberfarm.projector.hint"), HINT_WIDTH);
            graphics.setTooltipForNextFrame(font, hint, mouseX, mouseY);
        }
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            Map.Entry<Item, Integer> material = materials.get(cell);
            graphics.setTooltipForNextFrame(font, Component.literal(material.getValue() + "x ").append(new ItemStack(material.getKey()).getHoverName()),
                    mouseX, mouseY);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static float ease(float progress) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = left;
        int y = top;
        graphics.fill(x, y, x + WIDTH, y + PANEL_HEIGHT, OUTLINE);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + PANEL_HEIGHT - 1, PANEL);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + 2, PANEL_LIGHT);
        graphics.fill(x + 1, y + PANEL_HEIGHT - 2, x + WIDTH - 1, y + PANEL_HEIGHT - 1, PANEL_DARK);
        graphics.text(font, title, x + 8, y + 5, TEXT, false);
        graphics.fill(x + 6, y + 15, x + WIDTH - 6, y + 16, ACCENT_DIM);

        // The "i" in the corner: hover it for how to use the projector
        int infoX = x + WIDTH - 6 - INFO_SIZE;
        int infoY = y + 3;
        boolean info = isOverInfo(mouseX, mouseY);
        graphics.fill(infoX, infoY, infoX + INFO_SIZE, infoY + INFO_SIZE, SLOT_BORDER);
        graphics.fill(infoX + 1, infoY + 1, infoX + INFO_SIZE - 1, infoY + INFO_SIZE - 1, info ? ACCENT_DIM : PANEL_LIGHT);
        graphics.text(font, "i", infoX + (INFO_SIZE - font.width("i")) / 2, infoY + 2, info ? TEXT : ACCENT, false);

        drawList(graphics, x, y, rowAt(mouseX, mouseY));
        drawSide(graphics, x + SIDE_X, y + TOP);
    }

    private void drawList(GuiGraphicsExtractor graphics, int x, int y, int hovered) {
        boolean scrolls = structures.size() > VISIBLE_ROWS;
        int rowWidth = scrolls ? LIST_WIDTH - 4 : LIST_WIDTH;
        for (int row = 0; row < Math.min(VISIBLE_ROWS, structures.size()); row++) {
            int index = scroll + row;
            Structure structure = structures.get(index);
            int rowY = y + TOP + row * ROW;
            boolean on = structure == selected;
            int fill = on ? ACCENT_DIM : index == hovered ? PANEL_LIGHT : PANEL_DARK;
            graphics.fill(x + LIST_X, rowY, x + LIST_X + rowWidth, rowY + ROW - 1, SLOT_BORDER);
            graphics.fill(x + LIST_X + 1, rowY + 1, x + LIST_X + rowWidth - 1, rowY + ROW - 2, fill);
            // A cyan mark tells the schematics from the multiblocks
            if (!(structure instanceof Blueprint)) {
                graphics.fill(x + LIST_X + 1, rowY + 1, x + LIST_X + 3, rowY + ROW - 2, ACCENT);
            }
            String name = font.plainSubstrByWidth(structure.displayName().getString(), rowWidth - 10);
            graphics.text(font, name, x + LIST_X + 5, rowY + 3, on ? TEXT : TEXT_DIM, false);
        }
        if (scrolls) {
            int trackX = x + LIST_X + LIST_WIDTH - 3;
            int trackTop = y + TOP;
            int trackHeight = VISIBLE_ROWS * ROW - 1;
            int thumb = Math.max(8, trackHeight * VISIBLE_ROWS / structures.size());
            int thumbY = trackTop + (trackHeight - thumb) * scroll / maxScroll();
            graphics.fill(trackX, trackTop, trackX + 3, trackTop + trackHeight, SLOT_BG);
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumb, ACCENT_DIM);
        }
    }

    private void drawSide(GuiGraphicsExtractor graphics, int sideX, int lineY) {
        List<FormattedCharSequence> description = font.split(selected.description(), SIDE_WIDTH);
        for (FormattedCharSequence line : description.subList(0, Math.min(description.size(), MAX_DESCRIPTION_LINES))) {
            graphics.text(font, line, sideX, lineY, TEXT, false);
            lineY += LINE;
        }
        lineY += 6;
        graphics.text(font, Component.translatable("gui.cyberfarm.projector.materials"), sideX, lineY, ACCENT, false);
        lineY += 11;
        int shown = Math.min(materials.size(), gridCapacity(lineY));
        for (int i = 0; i < shown; i++) {
            int cellX = sideX + (i % CELLS_PER_ROW) * CELL + 1;
            int cellY = lineY + (i / CELLS_PER_ROW) * CELL + 1;
            graphics.fill(cellX - 1, cellY - 1, cellX + 17, cellY + 17, SLOT_BORDER);
            graphics.fill(cellX, cellY, cellX + 16, cellY + 16, SLOT_BG);
            ItemStack stack = new ItemStack(materials.get(i).getKey());
            int count = materials.get(i).getValue();
            graphics.item(stack, cellX, cellY);
            graphics.itemDecorations(font, stack, cellX, cellY, count >= 1000 ? count / 1000 + "k" : String.valueOf(count));
        }
        if (shown < materials.size()) {
            graphics.text(font, Component.translatable("gui.cyberfarm.dock.build_more", materials.size() - shown),
                    sideX, lineY + (shown + CELLS_PER_ROW - 1) / CELLS_PER_ROW * CELL + 1, TEXT_DIM, false);
        }
    }

    /** Where the block grid starts, after the description and its header. */
    private int gridTop() {
        int lines = Math.min(font.split(selected.description(), SIDE_WIDTH).size(), MAX_DESCRIPTION_LINES);
        return top + TOP + lines * LINE + 6 + 11;
    }

    /** How many blocks fit between {@code gridY} and the bottom of the panel, leaving a line for "+N more". */
    private int gridCapacity(int gridY) {
        int rows = (top + PANEL_HEIGHT - 6 - gridY) / CELL;
        int capacity = rows * CELLS_PER_ROW;
        return materials.size() > capacity ? Math.max(0, (rows - 1) * CELLS_PER_ROW) : capacity;
    }

    /** The block of the grid under the mouse, or -1. */
    private int cellAt(double mouseX, double mouseY) {
        int gridY = gridTop();
        double dx = mouseX - left - SIDE_X;
        double dy = mouseY - gridY;
        if (dx < 0 || dy < 0 || dx >= CELLS_PER_ROW * CELL) {
            return -1;
        }
        int index = (int) (dy / CELL) * CELLS_PER_ROW + (int) (dx / CELL);
        return index < Math.min(materials.size(), gridCapacity(gridY)) ? index : -1;
    }

    private boolean isOverInfo(double mouseX, double mouseY) {
        int infoX = left + WIDTH - 6 - INFO_SIZE;
        int infoY = top + 3;
        return mouseX >= infoX && mouseX < infoX + INFO_SIZE && mouseY >= infoY && mouseY < infoY + INFO_SIZE;
    }

    /** The structure under the mouse, as an index into the whole list, or -1. */
    private int rowAt(double mouseX, double mouseY) {
        double dx = mouseX - left - LIST_X;
        double dy = mouseY - top - TOP;
        if (dx < 0 || dx >= LIST_WIDTH || dy < 0) {
            return -1;
        }
        int row = (int) (dy / ROW);
        return row < VISIBLE_ROWS && scroll + row < structures.size() ? scroll + row : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int index = event.button() == 0 ? rowAt(event.x(), event.y()) : -1;
        if (index >= 0) {
            select(structures.get(index));
            ClientPacketDistributor.sendToServer(new ProjectorSelectPayload(hand, selected.id()));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (structures.size() > VISIBLE_ROWS) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
