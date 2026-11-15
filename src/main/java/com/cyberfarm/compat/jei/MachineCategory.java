package com.cyberfarm.compat.jei;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.compat.MachineExample;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** JEI page of one CyberFarm machine: inputs -> outputs with the machine's numbers below. */
public class MachineCategory extends AbstractRecipeCategory<MachineExample> {
    private static final Map<MachineExample.Machine, RecipeType<MachineExample>> TYPES = new EnumMap<>(MachineExample.Machine.class);
    private static final int TEXT_COLOR = 0xFF404040;

    private final IDrawable arrow;

    public static RecipeType<MachineExample> type(MachineExample.Machine machine) {
        return TYPES.computeIfAbsent(machine, key -> RecipeType.create(CyberFarm.MODID, key.id(), MachineExample.class));
    }

    public MachineCategory(IGuiHelper guiHelper, MachineExample.Machine machine) {
        super(type(machine), machine.title(), guiHelper.createDrawableItemLike(machine.block()), MachineExample.WIDTH, MachineExample.height(machine));
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineExample recipe, IFocusGroup focuses) {
        MachineExample.Machine machine = recipe.machine();
        int input = 0;
        for (var stacks : recipe.itemInputs()) {
            IRecipeSlotBuilder slot = slot(builder, RecipeIngredientRole.INPUT, MachineExample.inputX(machine, input),
                    MachineExample.inputY(machine, input++));
            stacks.forEach(slot::addItemStack);
        }
        for (MachineExample.FluidAmount fluid : recipe.fluidInputs()) {
            slot(builder, RecipeIngredientRole.INPUT, MachineExample.inputX(machine, input), MachineExample.inputY(machine, input++))
                    .addFluidStack(fluid.fluid(), fluid.amount());
        }
        int output = 0, y = MachineExample.outputY(machine);
        for (var stack : recipe.itemOutputs()) {
            slot(builder, RecipeIngredientRole.OUTPUT, MachineExample.outputX(machine, output++), y).addItemStack(stack);
        }
        for (MachineExample.FluidAmount fluid : recipe.fluidOutputs()) {
            slot(builder, RecipeIngredientRole.OUTPUT, MachineExample.outputX(machine, output++), y).addFluidStack(fluid.fluid(), fluid.amount());
        }
    }

    private static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role).setPosition(x + 1, y + 1).setStandardSlotBackground();
    }

    @Override
    public void draw(MachineExample recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        MachineExample.Machine machine = recipe.machine();
        arrow.draw(graphics, MachineExample.arrowX(machine), MachineExample.outputY(machine) + 1);
        var font = Minecraft.getInstance().font;
        List<Component> notes = recipe.notes();
        for (int i = 0; i < notes.size(); i++) {
            graphics.drawString(font, notes.get(i), 0, MachineExample.notesY(machine) + i * MachineExample.LINE, TEXT_COLOR, false);
        }
    }
}
