package com.cyberfarm.compat.rei;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.compat.MachineExample;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Label;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

/** REI page of one CyberFarm machine: inputs -> outputs with the machine's numbers below. */
public class MachineReiCategory implements DisplayCategory<MachineDisplay> {
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int PADDING = 5;

    private final MachineExample.Machine machine;

    public MachineReiCategory(MachineExample.Machine machine) {
        this.machine = machine;
    }

    @Override
    public CategoryIdentifier<? extends MachineDisplay> getCategoryIdentifier() {
        return MachineDisplay.id(machine);
    }

    @Override
    public Component getTitle() {
        return machine.title();
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(machine.block());
    }

    @Override
    public int getDisplayHeight() {
        return MachineExample.HEIGHT + PADDING * 2;
    }

    @Override
    public int getDisplayWidth(MachineDisplay display) {
        return MachineExample.WIDTH + PADDING * 2;
    }

    @Override
    public List<Widget> setupDisplay(MachineDisplay display, Rectangle bounds) {
        int x = bounds.x + PADDING;
        int y = bounds.y + PADDING;
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        List<EntryIngredient> inputs = display.getInputEntries();
        for (int i = 0; i < inputs.size(); i++) {
            widgets.add(Widgets.createSlot(new Point(x + MachineExample.inputX(i) + 1, y + 1)).entries(inputs.get(i)).markInput());
        }
        widgets.add(Widgets.createArrow(new Point(x + MachineExample.ARROW_X, y + 1)));
        List<EntryIngredient> outputs = display.getOutputEntries();
        for (int i = 0; i < outputs.size(); i++) {
            widgets.add(Widgets.createSlot(new Point(x + MachineExample.outputX(i) + 1, y + 1)).entries(outputs.get(i)).markOutput());
        }
        List<Component> notes = display.getExample().notes();
        for (int i = 0; i < notes.size(); i++) {
            Label label = Widgets.createLabel(new Point(x, y + MachineExample.NOTES_Y + i * MachineExample.LINE), notes.get(i));
            label.setHorizontalAlignment(Label.LEFT_ALIGNED);
            label.setShadow(false);
            label.setColor(TEXT_COLOR);
            widgets.add(label);
        }
        return widgets;
    }
}
