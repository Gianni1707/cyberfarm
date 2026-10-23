package com.cyberfarm.compat.rei;

import com.cyberfarm.client.screen.DroneDockScreen;
import com.cyberfarm.compat.AssemblyExample;
import com.cyberfarm.compat.InfoPages;
import com.cyberfarm.compat.MachineExample;
import com.cyberfarm.registry.ModItems;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.client.BuiltinClientPlugin;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.network.chat.Component;

/**
 * REI integration, loaded by REI only when it is installed.
 * Crafting recipes need nothing here: REI shows them on its own.
 */
@REIPluginClient
public class CyberFarmReiPlugin implements REIClientPlugin {
    /** Keeps REI's panels clear of the Drone Dock's side tabs. */
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(DroneDockScreen.class, (DroneDockScreen screen) -> screen.sideAreas().stream()
                .map(area -> new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()))
                .toList());
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new DroneAssemblyReiCategory());
        registry.addWorkstations(DroneAssemblyReiCategory.ID, EntryIngredients.of(ModItems.DRONE_ASSEMBLER.get()));
        for (MachineExample.Machine machine : MachineExample.Machine.values()) {
            registry.add(new MachineReiCategory(machine));
            registry.addWorkstations(MachineDisplay.id(machine), EntryIngredients.of(machine.block()));
        }
        // The generator burns the same fuels as a furnace
        registry.addWorkstations(BuiltinPlugin.FUEL, EntryIngredients.of(ModItems.COMBUSTION_GENERATOR.get()));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (AssemblyExample example : AssemblyExample.all()) {
            registry.add(new DroneAssemblyDisplay(example));
        }
        for (MachineExample example : MachineExample.all()) {
            registry.add(new MachineDisplay(example));
        }
        for (InfoPages.Page page : InfoPages.all()) {
            BuiltinClientPlugin.getInstance().registerInformation(EntryStacks.of(page.item()), Component.translatable(page.item().getDescriptionId()),
                    lines -> {
                        lines.add(page.text());
                        return lines;
                    });
        }
    }
}
