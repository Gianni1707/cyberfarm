package com.cyberfarm.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.cyberfarm.block.entity.BioDigesterBlockEntity;
import com.cyberfarm.block.entity.AcidSynthesizerBlockEntity;
import com.cyberfarm.block.entity.CoolantMixerBlockEntity;
import com.cyberfarm.block.entity.GasTurbineBlockEntity;
import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;
import com.cyberfarm.block.entity.ReactorControllerBlockEntity;
import com.cyberfarm.block.entity.WasteReprocessorBlockEntity;
import com.cyberfarm.block.entity.WaterPumpBlockEntity;
import com.cyberfarm.item.FuelRodItem;
import com.cyberfarm.multiblock.TowerSize;
import com.cyberfarm.multiblock.TurbineSize;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * One thing a CyberFarm machine does, for the recipe viewers' machine pages (JEI, REI): item and
 * fluid inputs, an arrow, item and fluid outputs, and up to {@link #MAX_NOTES} lines of numbers below.
 * Plain data, so every recipe viewer plugin lays it out the same way.
 */
public record MachineExample(Machine machine, List<List<ItemStack>> itemInputs, List<FluidAmount> fluidInputs,
        List<ItemStack> itemOutputs, List<FluidAmount> fluidOutputs, List<Component> notes) {

    public record FluidAmount(Fluid fluid, int amount) {}

    /** A machine page: its id, title and the block shown as icon and workstation. */
    public enum Machine {
        BIO_DIGESTER("bio_digester", ModItems.BIO_DIGESTER),
        METHANE_EXTRACTOR("methane_extractor", ModItems.METHANE_EXTRACTOR),
        GAS_TURBINE("gas_turbine", ModItems.GAS_TURBINE),
        WATER_PUMP("water_pump", ModItems.WATER_PUMP),
        COOLANT_MIXER("coolant_mixer", ModItems.COOLANT_MIXER),
        ACID_SYNTHESIZER("acid_synthesizer", ModItems.ACID_SYNTHESIZER),
        FISSION_REACTOR("fission_reactor", ModItems.REACTOR_CONTROLLER),
        STEAM_TURBINE("steam_turbine", ModItems.TURBINE_CONTROLLER),
        WASTE_REPROCESSOR("waste_reprocessor", ModItems.WASTE_REPROCESSOR);

        private final String id;
        private final Supplier<? extends ItemLike> block;

        Machine(String id, Supplier<? extends ItemLike> block) {
            this.id = id;
            this.block = block;
        }

        public String id() {
            return id;
        }

        public ItemLike block() {
            return block.get();
        }

        public Component title() {
            return Component.translatable("category.cyberfarm." + id);
        }
    }

    // Layout shared by the viewers, in pixels from the page's top-left corner
    public static final int WIDTH = 176;
    public static final int ARROW_X = 58;
    public static final int NOTES_Y = 24;
    public static final int LINE = 10;
    public static final int MAX_NOTES = 3;
    public static final int HEIGHT = NOTES_Y + MAX_NOTES * LINE;

    public static int inputX(int index) {
        return index * 18;
    }

    public static int outputX(int index) {
        return 88 + index * 18;
    }

    public static List<MachineExample> all() {
        List<MachineExample> all = new ArrayList<>();
        bioDigester(all);
        methaneExtractor(all);
        gasTurbine(all);
        all.add(new MachineExample(Machine.WATER_PUMP, List.of(), List.of(), List.of(),
                List.of(new FluidAmount(Fluids.WATER, WaterPumpBlockEntity.RATE)),
                List.of(note("pump"), note("energy", WaterPumpBlockEntity.ENERGY_PER_TICK))));
        coolantMixer(all);
        all.add(new MachineExample(Machine.ACID_SYNTHESIZER, List.of(List.of(new ItemStack(ModItems.SULFUR.get()))),
                List.of(new FluidAmount(Fluids.WATER, AcidSynthesizerBlockEntity.BATCH)), List.of(),
                List.of(new FluidAmount(ModFluids.ACID.get(), AcidSynthesizerBlockEntity.BATCH)),
                List.of(note("time_energy", AcidSynthesizerBlockEntity.PROCESS_TIME / 20, AcidSynthesizerBlockEntity.ENERGY_PER_TICK))));
        fissionReactor(all);
        steamTurbine(all);
        all.add(new MachineExample(Machine.WASTE_REPROCESSOR, List.of(List.of(new ItemStack(ModItems.DEPLETED_FUEL_ROD.get()))), List.of(),
                List.of(new ItemStack(ModItems.VITRIFIED_WASTE.get()), new ItemStack(ModItems.URANIUM_INGOT.get())), List.of(),
                List.of(note("time_energy", WasteReprocessorBlockEntity.PROCESS_TIME / 20, WasteReprocessorBlockEntity.ENERGY_PER_TICK),
                        note("chance", Component.translatable(ModItems.URANIUM_INGOT.get().getDescriptionId()), WasteReprocessorBlockEntity.URANIUM_CHANCE))));
        return all;
    }

    /** Organic items grouped by the biogas they make, one page per amount. */
    private static void bioDigester(List<MachineExample> all) {
        Map<Integer, List<ItemStack>> byAmount = new TreeMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            int amount = BioDigesterBlockEntity.biogasFor(stack);
            if (amount > 0) {
                byAmount.computeIfAbsent(amount, key -> new ArrayList<>()).add(stack);
            }
        }
        byAmount.forEach((amount, items) -> all.add(new MachineExample(Machine.BIO_DIGESTER, List.of(items), List.of(), List.of(),
                List.of(new FluidAmount(ModFluids.BIOGAS.get(), amount)),
                List.of(note("per_item", BioDigesterBlockEntity.PROCESS_TIME / 20)))));
    }

    /**
     * Spawn eggs grouped by methane rate.
     * shortcut: groups by entity type (zombie tag, mob category), while the extractor checks the live
     * mob (Enemy, Animal); a modded mob with an unusual category may show in the wrong group.
     */
    private static void methaneExtractor(List<MachineExample> all) {
        List<ItemStack> zombies = new ArrayList<>();
        List<ItemStack> monsters = new ArrayList<>();
        List<ItemStack> animals = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof SpawnEggItem)) {
                continue;
            }
            ItemStack egg = new ItemStack(item);
            EntityType<?> type = SpawnEggItem.getType(egg);
            if (type == null) {
                continue;
            }
            if (type.builtInRegistryHolder().is(EntityTypeTags.ZOMBIES)) {
                zombies.add(egg);
            } else if (type.getCategory() == MobCategory.MONSTER) {
                monsters.add(egg);
            } else if (type.getCategory() == MobCategory.CREATURE) {
                animals.add(egg);
            }
        }
        methaneGroup(all, "zombies", zombies, MethaneExtractorBlockEntity.ZOMBIE_RATE);
        methaneGroup(all, "monsters", monsters, MethaneExtractorBlockEntity.MONSTER_RATE);
        methaneGroup(all, "animals", animals, MethaneExtractorBlockEntity.ANIMAL_RATE);
    }

    private static void methaneGroup(List<MachineExample> all, String group, List<ItemStack> eggs, int rate) {
        if (eggs.isEmpty()) {
            return;
        }
        all.add(new MachineExample(Machine.METHANE_EXTRACTOR, List.of(eggs), List.of(), List.of(),
                List.of(new FluidAmount(ModFluids.METHANE.get(), rate)),
                List.of(note(group), note("per_mob", rate), note("capsule"))));
    }

    private static void gasTurbine(List<MachineExample> all) {
        int burn = GasTurbineBlockEntity.BURN_PER_TICK;
        for (FluidAmount fuel : List.of(new FluidAmount(ModFluids.BIOGAS.get(), GasTurbineBlockEntity.FE_PER_MB_BIOGAS),
                new FluidAmount(ModFluids.METHANE.get(), GasTurbineBlockEntity.FE_PER_MB_METHANE))) {
            all.add(new MachineExample(Machine.GAS_TURBINE, List.of(), List.of(new FluidAmount(fuel.fluid(), burn)), List.of(), List.of(),
                    List.of(note("fuel", fuel.amount(), fuel.amount() * burn))));
        }
    }

    private static void coolantMixer(List<MachineExample> all) {
        int batch = CoolantMixerBlockEntity.BATCH;
        Component time = note("time_energy", CoolantMixerBlockEntity.PROCESS_TIME / 20, CoolantMixerBlockEntity.ENERGY_PER_TICK);
        all.add(new MachineExample(Machine.COOLANT_MIXER, List.of(List.of(new ItemStack(Items.LAPIS_LAZULI))),
                List.of(new FluidAmount(Fluids.WATER, batch)), List.of(), List.of(new FluidAmount(ModFluids.COOLANT.get(), batch)), List.of(time)));
        all.add(new MachineExample(Machine.COOLANT_MIXER, List.of(List.of(new ItemStack(Items.BLUE_ICE))),
                List.of(new FluidAmount(ModFluids.COOLANT.get(), batch)), List.of(), List.of(new FluidAmount(ModFluids.CRYO_COOLANT.get(), batch)), List.of(time)));
    }

    /** One page per tower size (rods and water in, waste and steam out), then the two coolants of the primary loop. */
    private static void fissionReactor(List<MachineExample> all) {
        for (TowerSize size : TowerSize.values()) {
            int heat = size.baseTransfer();
            all.add(new MachineExample(Machine.FISSION_REACTOR, List.of(List.of(new ItemStack(ModItems.FUEL_ROD.get(), size.rods()))),
                    List.of(new FluidAmount(Fluids.WATER, heat)),
                    List.of(new ItemStack(ModItems.DEPLETED_FUEL_ROD.get(), size.rods())), List.of(new FluidAmount(ModFluids.STEAM.get(), heat)),
                    List.of(note("tower", sizeName(size.name()), size.rods(), size.maxBoost()), note("heat", heat),
                            note("rod_life", FuelRodItem.BURN_SECONDS / 60))));
        }
        coolant(all, ModFluids.COOLANT.get(), ReactorControllerBlockEntity.COOLANT_FACTOR);
        coolant(all, ModFluids.CRYO_COOLANT.get(), ReactorControllerBlockEntity.CRYO_FACTOR);
    }

    private static void coolant(List<MachineExample> all, Fluid coolant, double factor) {
        all.add(new MachineExample(Machine.FISSION_REACTOR, List.of(), List.of(new FluidAmount(coolant, 1)), List.of(), List.of(),
                List.of(note("coolant", factor), note("wear", ReactorControllerBlockEntity.COOLANT_WEAR))));
    }

    private static void steamTurbine(List<MachineExample> all) {
        for (TurbineSize size : TurbineSize.values()) {
            all.add(new MachineExample(Machine.STEAM_TURBINE, List.of(), List.of(new FluidAmount(ModFluids.STEAM.get(), size.maxSteam())),
                    List.of(), List.of(),
                    List.of(note("turbine", sizeName(size.name())), note("fuel", size.fePerMb(), size.maxOutput()))));
        }
    }

    private static Component sizeName(String size) {
        return Component.translatable("gui.cyberfarm.reactor.size." + size.toLowerCase(Locale.ROOT));
    }

    private static Component note(String key, Object... args) {
        return Component.translatable("category.cyberfarm.note." + key, args);
    }
}
