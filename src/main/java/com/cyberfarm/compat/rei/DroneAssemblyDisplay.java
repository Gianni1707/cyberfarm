package com.cyberfarm.compat.rei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cyberfarm.compat.AssemblyExample;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** REI display of one Drone Assembler example. Client-only, so it has no serializer. */
public class DroneAssemblyDisplay extends BasicDisplay {
    private final AssemblyExample example;

    public DroneAssemblyDisplay(AssemblyExample example) {
        super(inputs(example), List.of(EntryIngredients.of(example.output())), Optional.empty());
        this.example = example;
    }

    private static List<EntryIngredient> inputs(AssemblyExample example) {
        List<EntryIngredient> inputs = new ArrayList<>();
        inputs.add(EntryIngredients.of(example.frameStack()));
        example.moduleSlots().forEach(slot -> inputs.add(ingredient(slot)));
        example.addonSlots().forEach(slot -> inputs.add(ingredient(slot)));
        return inputs;
    }

    static EntryIngredient ingredient(List<ItemStack> stacks) {
        return EntryIngredients.ofItemStacks(stacks);
    }

    public AssemblyExample getExample() {
        return example;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return DroneAssemblyReiCategory.ID;
    }

    @Override
    public @Nullable DisplaySerializer<? extends DroneAssemblyDisplay> getSerializer() {
        return null;
    }
}
