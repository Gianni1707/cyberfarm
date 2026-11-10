package com.cyberfarm.compat.jei;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.screen.DroneDockScreen;
import com.cyberfarm.client.screen.MachineScreen;
import com.cyberfarm.compat.AssemblyExample;
import com.cyberfarm.compat.InfoPages;
import com.cyberfarm.compat.MachineExample;
import com.cyberfarm.registry.ModItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI integration, loaded by JEI only when it is installed (also read by EMI).
 * Crafting recipes need nothing here: JEI shows them on its own.
 */
@JeiPlugin
public class CyberFarmJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return CyberFarm.id("jei");
    }

    /** Each blueprint disc teaches its own building, kept in its NBT: JEI lists them apart. */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.useNbtForSubtypes(ModItems.BLUEPRINT_DISC.get());
    }

    /** Keeps JEI's bookmarks clear of the Drone Dock's side tabs. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(DroneDockScreen.class, new IGuiContainerHandler<DroneDockScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(DroneDockScreen screen) {
                return screen.sideAreas();
            }
        });
        // The addon tab outside the right edge of the machines that take addons
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<MachineScreen<?>> machineScreens = (Class) MachineScreen.class;
        registration.addGuiContainerHandler(machineScreens, new IGuiContainerHandler<MachineScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(MachineScreen<?> screen) {
                return screen.addonArea();
            }
        });
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
        registration.addRecipeCatalyst(ModItems.DRONE_ASSEMBLER.get(), DroneAssemblyCategory.TYPE);
        for (MachineExample.Machine machine : MachineExample.Machine.values()) {
            registration.addRecipeCatalyst(machine.block(), MachineCategory.type(machine));
        }
        // The generator burns the same fuels as a furnace
        registration.addRecipeCatalyst(ModItems.COMBUSTION_GENERATOR.get(), RecipeTypes.FUELING);
    }
}
