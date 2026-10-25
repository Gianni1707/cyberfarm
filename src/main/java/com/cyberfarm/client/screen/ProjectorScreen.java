package com.cyberfarm.client.screen;

import static com.cyberfarm.client.screen.MachineScreen.ACCENT;
import static com.cyberfarm.client.screen.MachineScreen.ACCENT_DIM;
import static com.cyberfarm.client.screen.MachineScreen.OUTLINE;
import static com.cyberfarm.client.screen.MachineScreen.PANEL;
import static com.cyberfarm.client.screen.MachineScreen.PANEL_DARK;
import static com.cyberfarm.client.screen.MachineScreen.PANEL_LIGHT;
import static com.cyberfarm.client.screen.MachineScreen.SLOT_BORDER;
import static com.cyberfarm.client.screen.MachineScreen.TEXT;
import static com.cyberfarm.client.screen.MachineScreen.TEXT_DIM;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cyberfarm.item.ProjectorItem;
import com.cyberfarm.item.ProjectorSelectPayload;
import com.cyberfarm.multiblock.Blueprint;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Multiblock Projector's list of structures: pick one on the left, read what it is and see the
 * blocks it takes on the right; the "i" in the corner says how to use the projector. Picking one
 * unlocks the projection, so it follows the player's aim again. Opens like a hologram: a beam
 * stretches across, then sweeps down and leaves the panel behind it.
 */
public class ProjectorScreen extends Screen {
    private static final int WIDTH = 248;
    private static final int LIST_X = 6;
    private static final int LIST_WIDTH = 112;
    private static final int TOP = 20;
    private static final int ROW = 14;
    private static final int SIDE_X = 126;
    private static final int SIDE_WIDTH = WIDTH - SIDE_X - 6;
    private static final int LINE = 10;
    private static final int MATERIAL_ROW = 18;
    private static final int INFO_SIZE = 11;
    private static final int HINT_WIDTH = 180;
    /** Opening animation, in milliseconds: the beam stretches across, then sweeps the panel down. */
    private static final float STRETCH_MS = 140.0F;
    private static final float SWEEP_MS = 280.0F;

    private final InteractionHand hand;
    private final long openedAt = Util.getMillis();
    private Blueprint selected;
    private Map<Block, Integer> materials = Map.of();
    private int panelHeight;
    private int left;
    private int top;

    private ProjectorScreen(InteractionHand hand, Blueprint selected) {
        super(Component.translatable("item.cyberfarm.multiblock_projector"));
        this.hand = hand;
        this.selected = selected;
    }

    public static void open(InteractionHand hand, Blueprint selected) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_POWER_SELECT, 1.8F, 0.4F));
        minecraft.setScreen(new ProjectorScreen(hand, selected));
    }

    @Override
    protected void init() {
        // Tall enough for the list and for the longest description and block list, so it never resizes
        int side = 0;
        for (Blueprint blueprint : Blueprint.values()) {
            side = Math.max(side, sideHeight(blueprint));
        }
        panelHeight = TOP + Math.max(Blueprint.values().length * ROW, side) + 6;
        left = (width - WIDTH) / 2;
        top = (height - panelHeight) / 2;
        select(selected);
    }

    private static Component description(Blueprint blueprint) {
        return Component.translatable(blueprint.key() + ".desc");
    }

    private int sideHeight(Blueprint blueprint) {
        return font.split(description(blueprint), SIDE_WIDTH).size() * LINE + 6 + 11 + countMaterials(blueprint).size() * MATERIAL_ROW;
    }

    private static Map<Block, Integer> countMaterials(Blueprint blueprint) {
        Map<Block, Integer> counts = new LinkedHashMap<>();
        for (Blueprint.Ghost ghost : blueprint.ghosts(BlockPos.ZERO, Direction.NORTH)) {
            if (ghost.show() != null) {
                counts.merge(ghost.show(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private void select(Blueprint blueprint) {
        selected = blueprint;
        materials = countMaterials(blueprint);
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
        int shown = Math.max(2, (int) (panelHeight * sweep));
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
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static float ease(float progress) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = left;
        int y = top;
        graphics.fill(x, y, x + WIDTH, y + panelHeight, OUTLINE);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + panelHeight - 1, PANEL);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + 2, PANEL_LIGHT);
        graphics.fill(x + 1, y + panelHeight - 2, x + WIDTH - 1, y + panelHeight - 1, PANEL_DARK);
        graphics.text(font, title, x + 8, y + 5, TEXT, false);
        graphics.fill(x + 6, y + 15, x + WIDTH - 6, y + 16, ACCENT_DIM);

        // The "i" in the corner: hover it for how to use the projector
        int infoX = x + WIDTH - 6 - INFO_SIZE;
        int infoY = y + 3;
        boolean info = isOverInfo(mouseX, mouseY);
        graphics.fill(infoX, infoY, infoX + INFO_SIZE, infoY + INFO_SIZE, SLOT_BORDER);
        graphics.fill(infoX + 1, infoY + 1, infoX + INFO_SIZE - 1, infoY + INFO_SIZE - 1, info ? ACCENT_DIM : PANEL_LIGHT);
        graphics.text(font, "i", infoX + (INFO_SIZE - font.width("i")) / 2, infoY + 2, info ? TEXT : ACCENT, false);

        int hovered = rowAt(mouseX, mouseY);
        for (Blueprint blueprint : Blueprint.values()) {
            int rowY = y + TOP + blueprint.ordinal() * ROW;
            boolean on = blueprint == selected;
            int fill = on ? ACCENT_DIM : blueprint.ordinal() == hovered ? PANEL_LIGHT : PANEL_DARK;
            graphics.fill(x + LIST_X, rowY, x + LIST_X + LIST_WIDTH, rowY + ROW - 1, SLOT_BORDER);
            graphics.fill(x + LIST_X + 1, rowY + 1, x + LIST_X + LIST_WIDTH - 1, rowY + ROW - 2, fill);
            String name = font.plainSubstrByWidth(ProjectorItem.name(blueprint).getString(), LIST_WIDTH - 8);
            graphics.text(font, name, x + LIST_X + 4, rowY + 3, on ? TEXT : TEXT_DIM, false);
        }

        int sideX = x + SIDE_X;
        int lineY = y + TOP;
        for (FormattedCharSequence line : font.split(description(selected), SIDE_WIDTH)) {
            graphics.text(font, line, sideX, lineY, TEXT, false);
            lineY += LINE;
        }
        lineY += 6;
        graphics.text(font, Component.translatable("gui.cyberfarm.projector.materials"), sideX, lineY, ACCENT, false);
        lineY += 11;
        for (Map.Entry<Block, Integer> entry : materials.entrySet()) {
            ItemStack stack = new ItemStack(entry.getKey());
            graphics.item(stack, sideX, lineY);
            String text = font.plainSubstrByWidth(entry.getValue() + "x " + stack.getHoverName().getString(), SIDE_WIDTH - 20);
            graphics.text(font, text, sideX + 19, lineY + 4, TEXT_DIM, false);
            lineY += MATERIAL_ROW;
        }
    }

    private boolean isOverInfo(double mouseX, double mouseY) {
        int infoX = left + WIDTH - 6 - INFO_SIZE;
        int infoY = top + 3;
        return mouseX >= infoX && mouseX < infoX + INFO_SIZE && mouseY >= infoY && mouseY < infoY + INFO_SIZE;
    }

    /** The structure row under the mouse, or -1. */
    private int rowAt(double mouseX, double mouseY) {
        double dx = mouseX - left - LIST_X;
        double dy = mouseY - top - TOP;
        if (dx < 0 || dx >= LIST_WIDTH || dy < 0) {
            return -1;
        }
        int row = (int) (dy / ROW);
        return row < Blueprint.values().length ? row : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int row = event.button() == 0 ? rowAt(event.x(), event.y()) : -1;
        if (row >= 0) {
            select(Blueprint.byOrdinal(row));
            ClientPacketDistributor.sendToServer(new ProjectorSelectPayload(hand, row));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
