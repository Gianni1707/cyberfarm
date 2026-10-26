package com.cyberfarm.client;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.MachineStatus;
import com.cyberfarm.entity.MechEntity;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * A small panel beside the crosshair while the player looks at a CyberFarm machine: whether it
 * works, the FE it makes and uses per tick, its buffer and how far its batch is. Read from the
 * {@link MachineStatus} the server sends, so it needs no menu open.
 */
public final class MachineOverlay {
    private static final Identifier LAYER = CyberFarm.id("machine_overlay");
    private static final int CYAN = 0xFF2FE0FF;
    private static final int CYAN_FAINT = 0x302FE0FF;
    private static final int GREEN = 0xFF6CFF6C;
    private static final int AMBER = 0xFFFFB347;
    private static final int GREY = 0xFF8A9AA6;
    private static final int TEXT = 0xFFD8F8FF;
    private static final int BACKGROUND = 0xB0101820;
    private static final int BAR_WIDTH = 90;

    private MachineOverlay() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, MachineOverlay::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.level == null || minecraft.player == null
                || minecraft.player.getVehicle() instanceof MechEntity
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !(minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof MachineBlockEntity machine)) {
            return;
        }
        MachineStatus status = machine.status;
        // Nothing to say about a machine with no buffer, no batches and nothing going on, such as a tank
        if (status.capacity() == 0 && status.progress() < 0 && !status.working()) {
            return;
        }
        Font font = minecraft.font;
        List<Component> lines = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        lines.add(machine.getDisplayName());
        colours.add(TEXT);
        lines.add(Component.translatable(status.working() ? "hud.cyberfarm.machine.working" : "hud.cyberfarm.machine.idle"));
        colours.add(status.working() ? CYAN : GREY);
        if (status.made() > 0) {
            lines.add(Component.translatable("hud.cyberfarm.machine.made", rate(status.made())));
            colours.add(GREEN);
        }
        if (status.used() > 0) {
            lines.add(Component.translatable("hud.cyberfarm.machine.used", rate(status.used())));
            colours.add(AMBER);
        }
        Component energy = Component.translatable("gui.cyberfarm.energy", String.format("%,d", status.energy()), String.format("%,d", status.capacity()));
        int width = BAR_WIDTH;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        width = Math.max(width, font.width(energy));
        int bars = (status.capacity() > 0 ? 1 : 0) + (status.progress() >= 0 ? 1 : 0);
        int height = lines.size() * 10 + bars * 14;
        // Drawn at two screen pixels per unit (three on very large GUI scales) whatever the GUI scale,
        // so it stays small beside the crosshair and its text stays sharp
        double guiScale = minecraft.getWindow().getGuiScale();
        float scale = (float) Math.min(1.0, Math.max(2.0, Math.ceil(guiScale / 2)) / guiScale);
        float panel = (width + 8) * scale;
        float left = graphics.guiWidth() / 2.0F + 10;
        if (left + panel > graphics.guiWidth() - 4) {
            // No room on the right: beside the crosshair on the left instead
            left = graphics.guiWidth() / 2.0F - 10 - panel;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(left + 4 * scale, graphics.guiHeight() / 2.0F - height * scale / 2);
        graphics.pose().scale(scale);
        int x = 0;
        int y = 0;
        graphics.fill(x - 4, y - 4, x + width + 4, y + height + 2, BACKGROUND);
        graphics.fill(x - 4, y - 4, x - 3, y + height + 2, CYAN);
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(font, lines.get(i), x, y, colours.get(i), true);
            y += 10;
        }
        if (status.capacity() > 0) {
            graphics.text(font, energy, x, y, TEXT, true);
            bar(graphics, x, y + 9, width, (float) status.energy() / status.capacity(), CYAN);
            y += 14;
        }
        if (status.progress() >= 0) {
            Component progress = Component.translatable("hud.cyberfarm.machine.progress", Math.round(status.progress() * 100));
            graphics.text(font, progress, x, y, TEXT, true);
            bar(graphics, x + font.width(progress) + 4, y + 2, width - font.width(progress) - 4, status.progress(), status.working() ? CYAN : GREY);
        }
        graphics.pose().popMatrix();
    }

    private static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, float fill, int colour) {
        graphics.fill(x, y, x + width, y + 3, CYAN_FAINT);
        graphics.fill(x, y, x + Math.round(width * Mth.clamp(fill, 0.0F, 1.0F)), y + 3, colour);
    }

    /** FE per tick: one decimal while small, whole numbers above ten. */
    private static String rate(float perTick) {
        return perTick >= 10 || perTick == Math.round(perTick) ? String.format("%,d", Math.round(perTick)) : String.format("%.1f", perTick);
    }
}
