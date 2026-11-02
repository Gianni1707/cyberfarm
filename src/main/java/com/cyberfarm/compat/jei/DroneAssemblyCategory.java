package com.cyberfarm.compat.jei;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.compat.AssemblyExample;
import com.cyberfarm.registry.ModItems;

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
import net.minecraft.world.item.ItemStack;

/** JEI page of the Drone Assembler: frame + modules + addons -> drone. */
public class DroneAssemblyCategory extends AbstractRecipeCategory<AssemblyExample> {
    public static final RecipeType<AssemblyExample> TYPE = RecipeType.create(CyberFarm.MODID, "drone_assembly", AssemblyExample.class);
    private static final int TEXT_COLOR = 0xFF404040;

    private final IDrawable arrow;

    public DroneAssemblyCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("category.cyberfarm.drone_assembly"),
                guiHelper.createDrawableItemLike(ModItems.DRONE_ASSEMBLER.get()), 150, 70);
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AssemblyExample recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT).setPosition(1, 23).setStandardSlotBackground().addItemStack(recipe.frameStack());
        for (int i = 0; i < recipe.moduleSlots().size(); i++) {
            addCycling(builder.addSlot(RecipeIngredientRole.INPUT).setPosition(25, 5 + i * 18).setStandardSlotBackground(),
                    recipe.moduleSlots().get(i));
        }
        for (int i = 0; i < recipe.addonSlots().size(); i++) {
            addCycling(builder.addSlot(RecipeIngredientRole.INPUT).setPosition(49 + (i % 2) * 18, 14 + (i / 2) * 18).setStandardSlotBackground(),
                    recipe.addonSlots().get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT).setPosition(125, 23).setOutputSlotBackground().addItemStack(recipe.output());
    }

    private static void addCycling(IRecipeSlotBuilder slot, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            slot.addItemStack(stack);
        }
    }

    @Override
    public void draw(AssemblyExample recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 94, 23);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("category.cyberfarm.drone_assembly.tier", recipe.frame().getTier(),
                recipe.frame().getModuleSlots(), recipe.frame().getAddonLimit()), 0, 52, TEXT_COLOR, false);
        graphics.drawString(font, Component.translatable("category.cyberfarm.drone_assembly.cost", String.format("%,d", AssemblyExample.ENERGY_COST)),
                0, 62, TEXT_COLOR, false);
    }
}
