package com.cyberfarm.client.screen;

import static com.cyberfarm.client.screen.MachineScreen.ACCENT;
import static com.cyberfarm.client.screen.MachineScreen.ACCENT_DIM;
import static com.cyberfarm.client.screen.MachineScreen.OUTLINE;
import static com.cyberfarm.client.screen.MachineScreen.PANEL;
import static com.cyberfarm.client.screen.MachineScreen.PANEL_DARK;
import static com.cyberfarm.client.screen.MachineScreen.PANEL_LIGHT;
import static com.cyberfarm.client.screen.MachineScreen.TEXT;
import static com.cyberfarm.client.screen.MachineScreen.TEXT_DIM;

import java.util.List;

import com.cyberfarm.village.MechanistChoicePayload;
import com.cyberfarm.village.MechanistDialogue;
import com.cyberfarm.village.MechanistDialogue.Choice;
import com.cyberfarm.village.MechanistDialoguePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * A talk with the Drone Mechanist: a panel at the foot of the screen with his name on a tab, his
 * lines typed out one after another (a click, Space or Enter shows the whole line, then the next),
 * and the answers or the Trade and Goodbye buttons once he is done.
 */
public class MechanistScreen extends Screen {
    private static final int MAX_WIDTH = 340;
    private static final int PANEL_HEIGHT = 64;
    private static final int MARGIN = 12;
    private static final int BUTTON_HEIGHT = 18;
    private static final int MS_PER_CHAR = 18;

    private final MechanistDialoguePayload talk;
    private int line;
    private long lineStart;
    private boolean buttonsShown;
    private int left;
    private int top;
    private int panelWidth;

    private MechanistScreen(MechanistDialoguePayload talk) {
        super(talk.name());
        this.talk = talk;
    }

    public static void open(MechanistDialoguePayload talk) {
        if (!talk.lines().isEmpty()) {
            Minecraft.getInstance().setScreen(new MechanistScreen(talk));
        }
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_WIDTH, width - 2 * MARGIN);
        left = (width - panelWidth) / 2;
        top = height - PANEL_HEIGHT - MARGIN;
        if (lineStart == 0) {
            startLine(0);
        }
        buttonsShown = false;
        clearWidgets();
        if (onLastLine() && typed() >= text().length()) {
            showButtons();
        }
    }

    private void startLine(int index) {
        line = index;
        lineStart = Util.getMillis();
        float pitch = 0.9F + 0.2F * (float) Math.random();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_AMBIENT, pitch, 0.35F));
    }

    private String text() {
        return talk.lines().get(line).getString();
    }

    private int typed() {
        return (int) ((Util.getMillis() - lineStart) / MS_PER_CHAR);
    }

    private boolean onLastLine() {
        return line >= talk.lines().size() - 1;
    }

    /** The answers stacked up the right side, over the panel. */
    private void showButtons() {
        buttonsShown = true;
        List<Choice> choices = talk.choices();
        int buttonWidth = 80;
        for (Choice choice : choices) {
            buttonWidth = Math.max(buttonWidth, font.width(label(choice)) + 16);
        }
        int y = top - 4 - choices.size() * (BUTTON_HEIGHT + 2);
        for (Choice choice : choices) {
            addRenderableWidget(Button.builder(label(choice), button -> choose(choice))
                    .bounds(left + panelWidth - buttonWidth, y, buttonWidth, BUTTON_HEIGHT).build());
            y += BUTTON_HEIGHT + 2;
        }
    }

    private static Component label(Choice choice) {
        return switch (choice) {
            case ACCEPT, DECLINE, DOUBT -> Component.translatable(MechanistDialogue.KEY + "choice." + choice.name().toLowerCase());
            case TRADE, BYE -> Component.translatable("gui.cyberfarm.mechanist." + choice.name().toLowerCase());
        };
    }

    private void choose(Choice choice) {
        ClientPacketDistributor.sendToServer(new MechanistChoicePayload(talk.entity(), choice.ordinal()));
        if (choice == Choice.TRADE || choice == Choice.BYE) {
            onClose();
        }
    }

    /** Shows the rest of the line, or moves on to the next. */
    private void advance() {
        if (typed() < text().length()) {
            lineStart = Util.getMillis() - (long) text().length() * MS_PER_CHAR;
        } else if (!onLastLine()) {
            startLine(line + 1);
        }
    }

    @Override
    public void tick() {
        if (!buttonsShown && onLastLine() && typed() >= text().length()) {
            showButtons();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0 && event.y() >= top) {
            advance();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation() || event.key() == 32) {
            advance();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The name on a tab over the panel's left corner
        int tabWidth = font.width(title) + 12;
        graphics.fill(left, top - 13, left + tabWidth, top + 1, OUTLINE);
        graphics.fill(left + 1, top - 12, left + tabWidth - 1, top + 1, PANEL_LIGHT);
        graphics.text(font, title, left + 6, top - 9, ACCENT, false);
        graphics.fill(left, top, left + panelWidth, top + PANEL_HEIGHT, OUTLINE);
        graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + PANEL_HEIGHT - 1, PANEL);
        graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + 2, ACCENT_DIM);
        graphics.fill(left + 1, top + PANEL_HEIGHT - 2, left + panelWidth - 1, top + PANEL_HEIGHT - 1, PANEL_DARK);
        // The line, typed out so far
        String text = text();
        String shown = text.substring(0, Math.min(text.length(), typed()));
        int y = top + 8;
        for (FormattedCharSequence row : font.split(Component.literal(shown), panelWidth - 16)) {
            graphics.text(font, row, left + 8, y, TEXT, false);
            y += font.lineHeight + 2;
        }
        // Where the talk is, and a blinking arrow while there is more to read
        String count = (line + 1) + "/" + talk.lines().size();
        graphics.text(font, count, left + panelWidth - 8 - font.width(count), top + PANEL_HEIGHT - 12, TEXT_DIM, false);
        if (shown.length() >= text.length() && !onLastLine() && Util.getMillis() / 400 % 2 == 0) {
            graphics.text(font, "▼", left + panelWidth - 22 - font.width(count), top + PANEL_HEIGHT - 12, ACCENT, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
