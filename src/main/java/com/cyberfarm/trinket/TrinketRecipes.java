package com.cyberfarm.trinket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import com.cyberfarm.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

/**
 * What the Trinket Table makes, like a crafting table: shapeless, one item from each filled slot of its 3x3 grid. A
 * Neural Chip with programs, and nothing else, comes out with the programs installed.
 */
public final class TrinketRecipes {
    /** One ingredient: what matches, in how many slots, and what a recipe viewer cycles through. */
    public record Input(Predicate<ItemStack> test, int count, Supplier<List<ItemStack>> shown) {
        static Input of(Supplier<? extends Item> item, int count) {
            return new Input(stack -> stack.is(item.get()), count, () -> List.of(new ItemStack(item.get())));
        }

        static Input of(Item item, int count) {
            return new Input(stack -> stack.is(item), count, () -> List.of(new ItemStack(item)));
        }

        /** Any item of a common tag, so another mod's aluminum, gold or leather works as well as the vanilla or ours. */
        static Input of(TagKey<Item> tag, int count) {
            return new Input(stack -> stack.is(tag), count,
                    () -> StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tag).spliterator(), false).map(ItemStack::new).toList());
        }
    }

    /** Aluminum from any mod, as the mod's own crafting recipes take it. */
    private static final TagKey<Item> ALUMINUM = ItemTags.create(Identifier.fromNamespaceAndPath("c", "ingots/aluminum"));

    public record Recipe(List<Input> inputs, Supplier<? extends Item> result) {
        public ItemStack output() {
            return new ItemStack(result.get());
        }

        /** The ingredients one per slot, as a recipe viewer lays them out: each slot the items it takes. */
        public List<List<ItemStack>> slots() {
            List<List<ItemStack>> slots = new ArrayList<>();
            for (Input input : inputs) {
                for (int i = 0; i < input.count(); i++) {
                    slots.add(input.shown().get());
                }
            }
            return slots;
        }
    }

    public static final List<Recipe> RECIPES = List.of(
            new Recipe(List.of(Input.of(Tags.Items.LEATHERS, 4), Input.of(ALUMINUM, 2), Input.of(Tags.Items.STRINGS, 2)), ModItems.UTILITY_BELT),
            new Recipe(List.of(Input.of(ModItems.BATTERY_CELL, 4), Input.of(ModItems.ADVANCED_CIRCUIT, 1), Input.of(Tags.Items.GLASS_BLOCKS, 2),
                    Input.of(ALUMINUM, 2)), ModItems.ENERGY_SPHERE),
            new Recipe(List.of(Input.of(ModItems.CARBON_FIBER, 4), Input.of(ModItems.ROTOR, 2), Input.of(Items.PHANTOM_MEMBRANE, 2),
                    Input.of(ModItems.ADVANCED_CIRCUIT, 1)), ModItems.GRAVITY_BOOTS),
            new Recipe(List.of(Input.of(ModItems.ADVANCED_CIRCUIT, 2), Input.of(Tags.Items.INGOTS_GOLD, 2), Input.of(Tags.Items.ENDER_PEARLS, 1),
                    Input.of(ModItems.CARBON_FIBER, 1), Input.of(Tags.Items.GEMS_DIAMOND, 1)), ModItems.NEURAL_CHIP),
            // The sphere's tiers: the one below, with its charge kept, and the parts of its tier
            new Recipe(List.of(Input.of(ModItems.ENERGY_SPHERE, 1), Input.of(ModItems.ADVANCED_CIRCUIT, 2), Input.of(ModItems.BATTERY_CELL, 4),
                    Input.of(ModItems.CARBON_FIBER, 2)), ModItems.ADVANCED_ENERGY_SPHERE),
            new Recipe(List.of(Input.of(ModItems.ADVANCED_ENERGY_SPHERE, 1), Input.of(ModItems.ELITE_CIRCUIT, 2), Input.of(Tags.Items.GEMS_DIAMOND, 2),
                    Input.of(ModItems.CARBON_FIBER, 2), Input.of(Items.ENDER_EYE, 1)), ModItems.ELITE_ENERGY_SPHERE),
            new Recipe(List.of(Input.of(ModItems.MINI_FRAME, 1), Input.of(ModItems.DRONE_CORE, 1), Input.of(Items.HOPPER, 1),
                    Input.of(ModItems.BASIC_CIRCUIT, 2)), ModItems.POCKET_DRONE),
            program(ModItems.ZOOM_PROGRAM, Input.of(Items.SPYGLASS, 1)),
            program(ModItems.NIGHT_VISION_PROGRAM, Input.of(Items.GOLDEN_CARROT, 2)),
            program(ModItems.MACHINE_LINK_PROGRAM, Input.of(Items.COMPARATOR, 1)),
            program(ModItems.GEIGER_HUD_PROGRAM, Input.of(ModItems.GEIGER_COUNTER, 1)),
            program(ModItems.TAXI_CALL_PROGRAM, Input.of(Items.BELL, 1)),
            program(ModItems.AUTO_FEEDER_PROGRAM, Input.of(Items.HAY_BLOCK, 1)));

    /** A program cartridge: a Basic Circuit, redstone and what the program is about. */
    private static Recipe program(Supplier<? extends Item> result, Input theme) {
        return new Recipe(List.of(Input.of(ModItems.BASIC_CIRCUIT, 1), Input.of(Tags.Items.DUSTS_REDSTONE, 2), theme), result);
    }

    private TrinketRecipes() {}

    /** What the grid makes, or empty: a chip with its new programs, or a recipe's result. One of each slot is used. */
    public static ItemStack result(List<ItemStack> grid) {
        ItemStack chip = installation(grid);
        if (!chip.isEmpty()) {
            return chip;
        }
        for (Recipe recipe : RECIPES) {
            if (matches(recipe, grid)) {
                ItemStack output = recipe.output();
                // A sphere's upgrade keeps the charge it had
                int charge = grid.stream().mapToInt(EnergySphereItem::energy).sum();
                return output.getItem() instanceof EnergySphereItem && charge > 0 ? EnergySphereItem.withEnergy(output, charge) : output;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Every filled slot taken by an ingredient, and every ingredient's slots filled: no more, no less. */
    static boolean matches(Recipe recipe, List<ItemStack> grid) {
        int[] needed = recipe.inputs().stream().mapToInt(Input::count).toArray();
        for (ItemStack stack : grid) {
            if (stack.isEmpty()) {
                continue;
            }
            int found = -1;
            for (int i = 0; i < needed.length && found < 0; i++) {
                if (needed[i] > 0 && recipe.inputs().get(i).test().test(stack)) {
                    found = i;
                }
            }
            if (found < 0) {
                return false;
            }
            needed[found]--;
        }
        for (int left : needed) {
            if (left != 0) {
                return false;
            }
        }
        return true;
    }

    /** One Neural Chip and programs it does not have yet, nothing else: the chip with them installed. */
    static ItemStack installation(List<ItemStack> grid) {
        ItemStack chip = ItemStack.EMPTY;
        List<ChipProgram> programs = new ArrayList<>();
        for (ItemStack stack : grid) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof NeuralChipItem && chip.isEmpty()) {
                chip = stack;
            } else if (stack.getItem() instanceof ProgramItem program && !programs.contains(program.program())) {
                programs.add(program.program());
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (chip.isEmpty() || programs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = chip.copyWithCount(1);
        for (ChipProgram program : programs) {
            if (!NeuralChipItem.canInstall(result, program)) {
                return ItemStack.EMPTY;
            }
            result = NeuralChipItem.install(result, program);
        }
        return result;
    }
}
