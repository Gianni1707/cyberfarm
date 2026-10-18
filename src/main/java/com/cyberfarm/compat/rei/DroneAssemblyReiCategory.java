package com.cyberfarm.compat.rei;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.compat.AssemblyExample;
import com.cyberfarm.registry.ModItems;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Label;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

/** REI page of the Drone Assembler: frame + modules + addons -> drone. */
public class DroneAssemblyReiCategory implements DisplayCategory<DroneAssemblyDisplay> {
    public static final CategoryIdentifier<DroneAssemblyDisplay> ID = CategoryIdentifier.of(CyberFarm.MODID, "drone_assembly");
    private static final int TEXT_COLOR = 0xFF404040;

    @Override
    public CategoryIdentifier<? extends DroneAssemblyDisplay> getCategoryIdentifier() {
        return ID;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("category.cyberfarm.drone_assembly");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(ModItems.DRONE_ASSEMBLER.get());
    }

    @Override
    public int getDisplayHeight() {
        return 80;
    }

    @Override
    public int getDisplayWidth(DroneAssemblyDisplay display) {
        return 160;
    }

    @Override
    public List<Widget> setupDisplay(DroneAssemblyDisplay display, Rectangle bounds) {
        AssemblyExample example = display.getExample();
        int x = bounds.x + 5;
        int y = bounds.y + 5;
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        widgets.add(Widgets.createSlot(new Point(x + 1, y + 23)).entries(EntryIngredients.of(example.frameStack())).markInput());
        for (int i = 0; i < example.moduleSlots().size(); i++) {
            widgets.add(Widgets.createSlot(new Point(x + 25, y + 5 + i * 18))
                    .entries(DroneAssemblyDisplay.ingredient(example.moduleSlots().get(i))).markInput());
        }
        for (int i = 0; i < example.addonSlots().size(); i++) {
            widgets.add(Widgets.createSlot(new Point(x + 49 + (i % 2) * 18, y + 14 + (i / 2) * 18))
                    .entries(DroneAssemblyDisplay.ingredient(example.addonSlots().get(i))).markInput());
        }
        widgets.add(Widgets.createArrow(new Point(x + 92, y + 23)));
        widgets.add(Widgets.createResultSlotBackground(new Point(x + 125, y + 23)));
        widgets.add(Widgets.createSlot(new Point(x + 125, y + 23)).entries(EntryIngredients.of(example.output())).disableBackground().markOutput());

        widgets.add(label(new Point(x, y + 56), Component.translatable("category.cyberfarm.drone_assembly.tier",
                example.frame().getTier(), example.frame().getModuleSlots(), example.frame().getAddonLimit())));
        widgets.add(label(new Point(x, y + 66), Component.translatable("category.cyberfarm.drone_assembly.cost",
                String.format("%,d", AssemblyExample.ENERGY_COST))));
        return widgets;
    }

    private static Label label(Point point, Component text) {
        Label label = Widgets.createLabel(point, text);
        label.setHorizontalAlignment(Label.LEFT_ALIGNED);
        label.setShadow(false);
        label.setColor(TEXT_COLOR);
        return label;
    }
}
