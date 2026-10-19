package com.cyberfarm.compat.rei;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.compat.MachineExample;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import org.jspecify.annotations.Nullable;

/** REI display of one machine example: items first, then fluids, on each side. Client-only, so it has no serializer. */
public class MachineDisplay extends BasicDisplay {
    private static final Map<MachineExample.Machine, CategoryIdentifier<MachineDisplay>> IDS = new EnumMap<>(MachineExample.Machine.class);

    private final MachineExample example;

    public MachineDisplay(MachineExample example) {
        super(entries(example.itemInputs().stream().map(EntryIngredients::ofItemStacks).toList(), example.fluidInputs()),
                entries(example.itemOutputs().stream().map(EntryIngredients::of).toList(), example.fluidOutputs()), Optional.empty());
        this.example = example;
    }

    public static CategoryIdentifier<MachineDisplay> id(MachineExample.Machine machine) {
        return IDS.computeIfAbsent(machine, key -> CategoryIdentifier.of(CyberFarm.MODID, key.id()));
    }

    private static List<EntryIngredient> entries(List<EntryIngredient> items, List<MachineExample.FluidAmount> fluids) {
        List<EntryIngredient> entries = new ArrayList<>(items);
        fluids.forEach(fluid -> entries.add(EntryIngredients.of(fluid.fluid(), fluid.amount())));
        return entries;
    }

    public MachineExample getExample() {
        return example;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return id(example.machine());
    }

    @Override
    public @Nullable DisplaySerializer<? extends MachineDisplay> getSerializer() {
        return null;
    }
}
