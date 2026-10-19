package com.cyberfarm.compat.jei;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.compat.AssemblyExample;
import com.cyberfarm.compat.InfoPages;
import com.cyberfarm.compat.MachineExample;
import com.cyberfarm.registry.ModItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

/**
 * JEI integration, loaded by JEI only when it is installed (also read by EMI).
 * Crafting recipes need nothing here: JEI shows them on its own.
 */
@JeiPlugin
public class CyberFarmJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return CyberFarm.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new DroneAssemblyCategory(registration.getJeiHelpers().getGuiHelper()));
        for (MachineExample.Machine machine : MachineExample.Machine.values()) {
            registration.addRecipeCategories(new MachineCategory(registration.getJeiHelpers().getGuiHelper(), machine));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(DroneAssemblyCategory.TYPE, AssemblyExample.all());
        for (MachineExample example : MachineExample.all()) {
            registration.addRecipes(MachineCategory.type(example.machine()), List.of(example));
        }
        for (InfoPages.Page page : InfoPages.all()) {
            registration.addIngredientInfo(page.item(), page.text());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(DroneAssemblyCategory.TYPE, ModItems.DRONE_ASSEMBLER.get());
        for (MachineExample.Machine machine : MachineExample.Machine.values()) {
            registration.addCraftingStation(MachineCategory.type(machine), machine.block());
        }
        // The generator burns the same fuels as a furnace
        registration.addCraftingStation(RecipeTypes.SMELTING_FUEL, ModItems.COMBUSTION_GENERATOR.get());
    }
}
