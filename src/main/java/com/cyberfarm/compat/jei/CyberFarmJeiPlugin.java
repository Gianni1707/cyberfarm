package com.cyberfarm.compat.jei;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.client.screen.DroneDockScreen;
import com.cyberfarm.client.screen.MachineScreen;
import com.cyberfarm.compat.AssemblyExample;
import com.cyberfarm.compat.InfoPages;
import com.cyberfarm.compat.MachineExample;
import com.cyberfarm.menu.TrinketTableMenu;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModMenus;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
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

    /** Keeps JEI's bookmarks clear of the Drone Dock's side tabs. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(DroneDockScreen.class, new IGuiContainerHandler<DroneDockScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(DroneDockScreen screen) {
                return screen.sideAreas();
            }
        });
        // The Network / Manage tabs beside the Transmitter Dish's panel
        registration.addGuiContainerHandler(com.cyberfarm.client.screen.TransmitterDishScreen.class,
                new IGuiContainerHandler<com.cyberfarm.client.screen.TransmitterDishScreen>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(com.cyberfarm.client.screen.TransmitterDishScreen screen) {
                        return screen.sideAreas();
                    }
                });
        // The Inventory / Trinkets tabs down the creative inventory's right side
        registration.addGlobalGuiHandler(new mezz.jei.api.gui.handlers.IGlobalGuiHandler() {
            @Override
            public java.util.Collection<Rect2i> getGuiExtraAreas() {
                return com.cyberfarm.client.TrinketClient.creativeTabArea(net.minecraft.client.Minecraft.getInstance().gui.screen());
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

    /**
     * The "+" on a Trinket Table page: it fills the table's grid from the inventory. On a crafting table it lays the
     * items in the grid too, with no result: only the Trinket Table makes trinkets (the user's choice).
     */
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        var table = MachineCategory.type(MachineExample.Machine.TRINKET_TABLE);
        registration.addRecipeTransferHandler(TrinketTableMenu.class, ModMenus.TRINKET_TABLE.get(), table, 1, 9, 10, 36);
        registration.addRecipeTransferHandler(CraftingMenu.class, MenuType.CRAFTING, table, 1, 9, 10, 36);
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
