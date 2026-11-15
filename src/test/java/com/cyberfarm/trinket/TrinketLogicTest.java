package com.cyberfarm.trinket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.cyberfarm.registry.ModItems;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

// Item components are bound only on a running server
@ExtendWith(EphemeralTestServerProvider.class)
class TrinketLogicTest {
    @BeforeAll
    static void startServer(MinecraftServer server) {
        // asking for the server is what makes the provider start it
    }

    /** A tool taken off the belt sends the held item into another slot, never the hand's own (which the tool takes). */
    @Test
    void theHeldItemNeedsASlotBesideTheHand() {
        List<ItemStack> slots = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            slots.add(new ItemStack(Items.STONE, 64));
        }
        slots.set(3, ItemStack.EMPTY);
        assertFalse(TrinketKeyPayload.roomBesideHand(slots, 3, new ItemStack(Items.DIAMOND)), "only the hand's slot is free");
        assertTrue(TrinketKeyPayload.roomBesideHand(slots, 4, new ItemStack(Items.DIAMOND)), "another slot is free");
        slots.set(3, new ItemStack(Items.STONE, 64));
        slots.set(20, new ItemStack(Items.DIAMOND, 60));
        assertTrue(TrinketKeyPayload.roomBesideHand(slots, 0, new ItemStack(Items.DIAMOND, 4)), "it tops up a stack");
        assertFalse(TrinketKeyPayload.roomBesideHand(slots, 0, new ItemStack(Items.DIAMOND, 5)), "but not past 64");
    }

    @Test
    void hoverHeightCyclesThroughOffAndBack() {
        ItemStack boots = new ItemStack(ModItems.GRAVITY_BOOTS.get());
        assertEquals(0.5, GravityBootsItem.height(boots), "new boots hover at half a block");
        boots = GravityBootsItem.nextHeight(boots);
        assertEquals(1.0, GravityBootsItem.height(boots));
        boots = GravityBootsItem.nextHeight(boots);
        assertEquals(2.0, GravityBootsItem.height(boots));
        boots = GravityBootsItem.nextHeight(boots);
        assertEquals(0.0, GravityBootsItem.height(boots), "then off");
        assertEquals(0.5, GravityBootsItem.height(GravityBootsItem.nextHeight(boots)));
    }

    @Test
    void aChipTakesEveryProgramOnce() {
        ItemStack chip = new ItemStack(ModItems.NEURAL_CHIP.get());
        chip = NeuralChipItem.install(chip, ChipProgram.ZOOM);
        assertFalse(NeuralChipItem.canInstall(chip, ChipProgram.ZOOM), "no program twice");
        for (ChipProgram program : ChipProgram.values()) {
            if (program != ChipProgram.ZOOM) {
                assertTrue(NeuralChipItem.canInstall(chip, program), program.name());
                chip = NeuralChipItem.install(chip, program);
            }
        }
        assertEquals(List.of(ChipProgram.values()), NeuralChipItem.programs(chip));
    }

    @Test
    void nightVisionStartsOffAndToggles() {
        ItemStack chip = NeuralChipItem.install(new ItemStack(ModItems.NEURAL_CHIP.get()), ChipProgram.NIGHT_VISION);
        assertFalse(NeuralChipItem.isOn(chip, ChipProgram.NIGHT_VISION));
        assertTrue(NeuralChipItem.isOn(NeuralChipItem.toggle(chip, ChipProgram.NIGHT_VISION), ChipProgram.NIGHT_VISION));
    }

    @Test
    void autoFeederEatsOnlyPlainFood() {
        assertEquals(8, TrinketTicker.plainNutrition(new ItemStack(Items.COOKED_BEEF)));
        assertEquals(0, TrinketTicker.plainNutrition(new ItemStack(Items.ROTTEN_FLESH)), "hunger effect");
        assertEquals(0, TrinketTicker.plainNutrition(new ItemStack(Items.GOLDEN_APPLE)), "a golden apple is kept for when it matters");
        assertEquals(0, TrinketTicker.plainNutrition(new ItemStack(Items.MUSHROOM_STEW)), "leaves a bowl");
        assertEquals(0, TrinketTicker.plainNutrition(new ItemStack(Items.STONE)));
    }

    @Test
    void eachBeltHolderTakesOnlyItsItem() {
        ItemStack sphere = new ItemStack(ModItems.ENERGY_SPHERE.get());
        assertTrue(UtilityBeltItem.fits(UtilityBeltItem.SPHERE, sphere));
        assertFalse(UtilityBeltItem.fits(UtilityBeltItem.DRONE, sphere));
        assertTrue(UtilityBeltItem.fits(UtilityBeltItem.PROJECTOR, new ItemStack(ModItems.MULTIBLOCK_PROJECTOR.get())));
        assertTrue(UtilityBeltItem.fits(UtilityBeltItem.SURVEY_TOOL, new ItemStack(ModItems.SURVEY_TOOL.get())));
        assertTrue(UtilityBeltItem.fits(UtilityBeltItem.DRONE, new ItemStack(ModItems.POCKET_DRONE.get())));
        assertFalse(UtilityBeltItem.fits(UtilityBeltItem.SURVEY_TOOL, new ItemStack(Items.STONE)));
    }

    @Test
    void whatGoesInAHolderStaysThere() {
        ItemStack belt = new ItemStack(ModItems.UTILITY_BELT.get());
        ItemStack sphere = EnergySphereItem.withEnergy(new ItemStack(ModItems.ENERGY_SPHERE.get()), 1234);
        belt = UtilityBeltItem.withContent(belt, UtilityBeltItem.SPHERE, sphere);
        belt = UtilityBeltItem.withContent(belt, UtilityBeltItem.DRONE, new ItemStack(ModItems.POCKET_DRONE.get()));
        assertEquals(1234, EnergySphereItem.energy(UtilityBeltItem.content(belt, UtilityBeltItem.SPHERE)));
        assertTrue(UtilityBeltItem.content(belt, UtilityBeltItem.PROJECTOR).isEmpty());
        belt = UtilityBeltItem.withContent(belt, UtilityBeltItem.SPHERE, ItemStack.EMPTY);
        assertTrue(UtilityBeltItem.content(belt, UtilityBeltItem.SPHERE).isEmpty());
        assertFalse(UtilityBeltItem.content(belt, UtilityBeltItem.DRONE).isEmpty(), "the other holders keep theirs");
    }

    @Test
    void theTableMakesARecipeInAnyOrderButNotWithExtras() {
        List<ItemStack> grid = new java.util.ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        grid.set(8, new ItemStack(Items.ENDER_PEARL));
        grid.set(0, new ItemStack(Items.GOLD_INGOT));
        grid.set(4, new ItemStack(ModItems.ADVANCED_CIRCUIT.get()));
        grid.set(2, new ItemStack(Items.GOLD_INGOT));
        grid.set(6, new ItemStack(ModItems.ADVANCED_CIRCUIT.get()));
        grid.set(1, new ItemStack(ModItems.CARBON_FIBER.get()));
        assertTrue(TrinketRecipes.result(grid).isEmpty(), "one short: the diamond");
        grid.set(7, new ItemStack(Items.DIAMOND));
        assertTrue(TrinketRecipes.result(grid).is(ModItems.NEURAL_CHIP.get()));
        grid.set(3, new ItemStack(Items.STONE));
        assertTrue(TrinketRecipes.result(grid).isEmpty(), "an item no recipe wants");
    }

    /** A sphere's next tier keeps the charge it had, up to what the new one holds. */
    @Test
    void anUpgradedSphereKeepsItsCharge() {
        List<ItemStack> grid = new java.util.ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        grid.set(0, EnergySphereItem.withEnergy(new ItemStack(ModItems.ENERGY_SPHERE.get()), 300_000));
        grid.set(1, new ItemStack(ModItems.ADVANCED_CIRCUIT.get()));
        grid.set(2, new ItemStack(ModItems.ADVANCED_CIRCUIT.get()));
        for (int slot = 3; slot < 7; slot++) {
            grid.set(slot, new ItemStack(ModItems.BATTERY_CELL.get()));
        }
        grid.set(7, new ItemStack(ModItems.CARBON_FIBER.get()));
        grid.set(8, new ItemStack(ModItems.CARBON_FIBER.get()));
        ItemStack advanced = TrinketRecipes.result(grid);
        assertTrue(advanced.is(ModItems.ADVANCED_ENERGY_SPHERE.get()));
        assertEquals(300_000, EnergySphereItem.energy(advanced));
        assertEquals(EnergySphereItem.ADVANCED_CAPACITY, EnergySphereItem.capacity(advanced));
    }

    @Test
    void chipAndProgramsInTheGridInstallThem() {
        ItemStack chip = new ItemStack(ModItems.NEURAL_CHIP.get());
        List<ItemStack> grid = List.of(chip, new ItemStack(ModItems.ZOOM_PROGRAM.get()), new ItemStack(ModItems.NIGHT_VISION_PROGRAM.get()));
        ItemStack result = TrinketRecipes.result(grid);
        assertEquals(List.of(ChipProgram.ZOOM, ChipProgram.NIGHT_VISION), NeuralChipItem.programs(result));
        assertTrue(TrinketRecipes.result(List.of(chip, new ItemStack(ModItems.ZOOM_PROGRAM.get()), new ItemStack(ModItems.ZOOM_PROGRAM.get())))
                .isEmpty(), "the same program twice");
        ItemStack zoom = NeuralChipItem.install(chip, ChipProgram.ZOOM);
        assertTrue(TrinketRecipes.result(List.of(zoom, new ItemStack(ModItems.ZOOM_PROGRAM.get()))).isEmpty(), "one it already has");
    }

    @Test
    void savedSlotsAlwaysComeBackComplete() {
        assertEquals(TrinketType.COUNT, Trinkets.Worn.of(List.of()).stacks().size());
        assertEquals(TrinketType.COUNT, Trinkets.Worn.of(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)).stacks().size());
    }
}
