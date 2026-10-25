package com.cyberfarm.registry;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ItemLore;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.drone.AddonType;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.item.DepletedFuelRodItem;
import com.cyberfarm.item.DroneAddonItem;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.item.DroneItem;
import com.cyberfarm.item.MechFrameItem;
import com.cyberfarm.item.MechModuleItem;
import com.cyberfarm.entity.MechModule;
import com.cyberfarm.item.DroneModuleItem;
import com.cyberfarm.item.FluidBucketItem;
import com.cyberfarm.item.FuelRodItem;
import com.cyberfarm.item.GeigerCounterItem;
import com.cyberfarm.item.HazmatSuitItem;
import com.cyberfarm.item.MobCapsuleItem;
import com.cyberfarm.item.ProjectorItem;
import com.cyberfarm.item.SurveyToolItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CyberFarm.MODID);

    // Machines
    public static final DeferredItem<BlockItem> COMBUSTION_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.COMBUSTION_GENERATOR);
    public static final DeferredItem<BlockItem> DRONE_ASSEMBLER = ITEMS.registerSimpleBlockItem(ModBlocks.DRONE_ASSEMBLER);
    public static final DeferredItem<BlockItem> DRONE_DOCK = ITEMS.registerSimpleBlockItem(ModBlocks.DRONE_DOCK);
    public static final DeferredItem<BlockItem> SWARM_DOCK = ITEMS.registerSimpleBlockItem(ModBlocks.SWARM_DOCK);
    public static final DeferredItem<BlockItem> ENERGY_CONDUIT = ITEMS.registerSimpleBlockItem(ModBlocks.ENERGY_CONDUIT);
    public static final DeferredItem<BlockItem> ADVANCED_ENERGY_CONDUIT = ITEMS.registerSimpleBlockItem(ModBlocks.ADVANCED_ENERGY_CONDUIT);
    public static final DeferredItem<BlockItem> ELITE_ENERGY_CONDUIT = ITEMS.registerSimpleBlockItem(ModBlocks.ELITE_ENERGY_CONDUIT);
    public static final DeferredItem<BlockItem> GAS_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.GAS_PIPE);
    public static final DeferredItem<BlockItem> ADVANCED_GAS_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.ADVANCED_GAS_PIPE);
    public static final DeferredItem<BlockItem> ELITE_GAS_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.ELITE_GAS_PIPE);
    public static final DeferredItem<BlockItem> LIQUID_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.LIQUID_PIPE);
    public static final DeferredItem<BlockItem> ADVANCED_LIQUID_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.ADVANCED_LIQUID_PIPE);
    public static final DeferredItem<BlockItem> ELITE_LIQUID_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.ELITE_LIQUID_PIPE);
    public static final DeferredItem<BlockItem> BIO_DIGESTER = ITEMS.registerSimpleBlockItem(ModBlocks.BIO_DIGESTER);
    public static final DeferredItem<BlockItem> GAS_TURBINE = ITEMS.registerSimpleBlockItem(ModBlocks.GAS_TURBINE);
    public static final DeferredItem<BlockItem> METHANE_EXTRACTOR = ITEMS.registerSimpleBlockItem(ModBlocks.METHANE_EXTRACTOR);
    public static final DeferredItem<BlockItem> CYBER_FRAME = ITEMS.registerSimpleBlockItem(ModBlocks.CYBER_FRAME);
    public static final DeferredItem<BlockItem> ACID_INLET = ITEMS.registerSimpleBlockItem(ModBlocks.ACID_INLET,
            p -> p.component(DataComponents.LORE, lore("tooltip.cyberfarm.acid_inlet", ChatFormatting.GREEN)));
    public static final DeferredItem<BlockItem> METHANE_OUTLET = ITEMS.registerSimpleBlockItem(ModBlocks.METHANE_OUTLET,
            p -> p.component(DataComponents.LORE, lore("tooltip.cyberfarm.methane_outlet", ChatFormatting.AQUA)));
    public static final DeferredItem<BlockItem> INTAKE_GATE = ITEMS.registerSimpleBlockItem(ModBlocks.INTAKE_GATE);
    public static final DeferredItem<BlockItem> MUFFLER = ITEMS.registerSimpleBlockItem(ModBlocks.MUFFLER);
    public static final DeferredItem<MobCapsuleItem> MOB_CAPSULE = ITEMS.registerItem("mob_capsule", MobCapsuleItem::new);

    // Nuclear
    public static final DeferredItem<BlockItem> URANIUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.URANIUM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_URANIUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_URANIUM_ORE);
    public static final DeferredItem<Item> RAW_URANIUM = ITEMS.registerSimpleItem("raw_uranium");
    public static final DeferredItem<Item> URANIUM_INGOT = ITEMS.registerSimpleItem("uranium_ingot");
    public static final DeferredItem<FuelRodItem> FUEL_ROD = ITEMS.registerItem("fuel_rod", FuelRodItem::new);
    public static final DeferredItem<DepletedFuelRodItem> DEPLETED_FUEL_ROD = ITEMS.registerItem("depleted_fuel_rod", DepletedFuelRodItem::new);
    public static final DeferredItem<Item> ISOTOPE_SHARD = ITEMS.registerSimpleItem("isotope_shard", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> REACTOR_CONTROLLER = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_CONTROLLER);
    public static final DeferredItem<BlockItem> REACTOR_CASING = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_CASING);
    public static final DeferredItem<BlockItem> REACTOR_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_PORT);
    public static final DeferredItem<BlockItem> TURBINE_CONTROLLER = ITEMS.registerSimpleBlockItem(ModBlocks.TURBINE_CONTROLLER);
    public static final DeferredItem<BlockItem> TURBINE_CASING = ITEMS.registerSimpleBlockItem(ModBlocks.TURBINE_CASING);
    public static final DeferredItem<BlockItem> TURBINE_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.TURBINE_PORT);
    public static final DeferredItem<BlockItem> WASTE_REPROCESSOR = ITEMS.registerSimpleBlockItem(ModBlocks.WASTE_REPROCESSOR);
    public static final DeferredItem<BlockItem> VITRIFIED_WASTE = ITEMS.registerSimpleBlockItem(ModBlocks.VITRIFIED_WASTE);
    public static final DeferredItem<HazmatSuitItem> HAZMAT_HELMET = ITEMS.registerItem("hazmat_helmet", p -> new HazmatSuitItem(p, ArmorType.HELMET));
    public static final DeferredItem<HazmatSuitItem> HAZMAT_SUIT = ITEMS.registerItem("hazmat_suit", p -> new HazmatSuitItem(p, ArmorType.CHESTPLATE));
    public static final DeferredItem<HazmatSuitItem> HAZMAT_PANTS = ITEMS.registerItem("hazmat_pants", p -> new HazmatSuitItem(p, ArmorType.LEGGINGS));
    public static final DeferredItem<HazmatSuitItem> HAZMAT_BOOTS = ITEMS.registerItem("hazmat_boots", p -> new HazmatSuitItem(p, ArmorType.BOOTS));
    public static final DeferredItem<GeigerCounterItem> GEIGER_COUNTER = ITEMS.registerItem("geiger_counter", GeigerCounterItem::new);
    public static final DeferredItem<BlockItem> COOLANT_MIXER = ITEMS.registerSimpleBlockItem(ModBlocks.COOLANT_MIXER);
    public static final DeferredItem<BlockItem> WATER_PUMP = ITEMS.registerSimpleBlockItem(ModBlocks.WATER_PUMP);
    public static final DeferredItem<BlockItem> LIQUID_TANK = ITEMS.registerSimpleBlockItem(ModBlocks.LIQUID_TANK);
    public static final DeferredItem<BlockItem> GAS_TANK = ITEMS.registerSimpleBlockItem(ModBlocks.GAS_TANK);

    // Buckets of the mod's fluids (creative): gases are held upside down
    public static final DeferredItem<FluidBucketItem> BIOGAS_BUCKET = ITEMS.registerItem("biogas_bucket", p -> new FluidBucketItem(ModFluids.BIOGAS.get(), p));
    public static final DeferredItem<FluidBucketItem> METHANE_BUCKET = ITEMS.registerItem("methane_bucket", p -> new FluidBucketItem(ModFluids.METHANE.get(), p));
    public static final DeferredItem<FluidBucketItem> STEAM_BUCKET = ITEMS.registerItem("steam_bucket", p -> new FluidBucketItem(ModFluids.STEAM.get(), p));
    public static final DeferredItem<FluidBucketItem> COOLANT_BUCKET = ITEMS.registerItem("coolant_bucket", p -> new FluidBucketItem(ModFluids.COOLANT.get(), p));
    public static final DeferredItem<FluidBucketItem> CRYO_COOLANT_BUCKET = ITEMS.registerItem("cryo_coolant_bucket", p -> new FluidBucketItem(ModFluids.CRYO_COOLANT.get(), p));
    public static final DeferredItem<FluidBucketItem> ACID_BUCKET = ITEMS.registerItem("acid_bucket", p -> new FluidBucketItem(ModFluids.ACID.get(), p));

    // Sulfur from the Nether, for the Acid Synthesizer
    public static final DeferredItem<BlockItem> NETHER_SULFUR_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.NETHER_SULFUR_ORE);
    public static final DeferredItem<Item> SULFUR = ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<BlockItem> CONTAINMENT_GLASS = ITEMS.registerSimpleBlockItem(ModBlocks.CONTAINMENT_GLASS);
    public static final DeferredItem<BlockItem> ACID_SYNTHESIZER = ITEMS.registerSimpleBlockItem(ModBlocks.ACID_SYNTHESIZER);
    public static final DeferredItem<BlockItem> STEAM_ENGINE = ITEMS.registerSimpleBlockItem(ModBlocks.STEAM_ENGINE);
    public static final DeferredItem<BlockItem> STEAM_ENGINE_CYLINDER = ITEMS.registerSimpleBlockItem(ModBlocks.STEAM_ENGINE_CYLINDER);
    public static final DeferredItem<BlockItem> STEAM_ENGINE_DYNAMO = ITEMS.registerSimpleBlockItem(ModBlocks.STEAM_ENGINE_DYNAMO);

    // Crafting components
    public static final DeferredItem<Item> DRONE_CORE = ITEMS.registerSimpleItem("drone_core");
    public static final DeferredItem<Item> ROTOR = ITEMS.registerSimpleItem("rotor");
    public static final DeferredItem<Item> BATTERY_CELL = ITEMS.registerSimpleItem("battery_cell");

    // Drone frames: tier, FE capacity, speed %, module slots, addon limit, flight cost FE/t
    public static final DeferredItem<DroneFrameItem> BASIC_FRAME = ITEMS.registerItem("basic_frame",
            p -> new DroneFrameItem(p, 1, 20_000, 80, 1, 4, 2));
    public static final DeferredItem<DroneFrameItem> ADVANCED_FRAME = ITEMS.registerItem("advanced_frame",
            p -> new DroneFrameItem(p, 2, 60_000, 130, 2, 8, 3));
    public static final DeferredItem<DroneFrameItem> ELITE_FRAME = ITEMS.registerItem("elite_frame",
            p -> new DroneFrameItem(p.rarity(Rarity.RARE), 3, 150_000, 160, 3, 16, 4));
    /** Builds a Mini Drone: half size, small battery, one module; up to four share a Builder Swarm Dock. */
    public static final DeferredItem<DroneFrameItem> MINI_FRAME = ITEMS.registerItem("mini_frame",
            p -> new DroneFrameItem(p, 1, 10_000, 100, 1, 2, 1));

    // Energy storage: the core, the cells of its shell and its pylons
    public static final DeferredItem<BlockItem> RESONANCE_CORE = ITEMS.registerSimpleBlockItem(ModBlocks.RESONANCE_CORE);
    public static final DeferredItem<BlockItem> RESONANCE_CELL = ITEMS.registerSimpleBlockItem(ModBlocks.RESONANCE_CELL);
    public static final DeferredItem<BlockItem> STABILIZER_PYLON = ITEMS.registerSimpleBlockItem(ModBlocks.STABILIZER_PYLON);

    // Function modules: job, extra flight cost FE/t
    public static final DeferredItem<DroneModuleItem> HARVESTER_MODULE = ITEMS.registerItem("harvester_module",
            p -> new DroneModuleItem(p, DroneTask.HARVEST, 1));
    public static final DeferredItem<DroneModuleItem> QUARRY_MODULE = ITEMS.registerItem("quarry_module",
            p -> new DroneModuleItem(p, DroneTask.QUARRY, 2));
    public static final DeferredItem<DroneModuleItem> TRANSPORT_MODULE = ITEMS.registerItem("transport_module",
            p -> new DroneModuleItem(p, DroneTask.TRANSPORT, 1));
    public static final DeferredItem<DroneModuleItem> PLANTER_MODULE = ITEMS.registerItem("planter_module",
            p -> new DroneModuleItem(p, DroneTask.PLANT, 1));
    public static final DeferredItem<DroneModuleItem> LOGGER_MODULE = ITEMS.registerItem("logger_module",
            p -> new DroneModuleItem(p, DroneTask.LOG, 2));
    public static final DeferredItem<DroneModuleItem> TAXI_MODULE = ITEMS.registerItem("taxi_module",
            p -> new DroneModuleItem(p, DroneTask.TAXI, 2));
    public static final DeferredItem<DroneModuleItem> RANCHER_MODULE = ITEMS.registerItem("rancher_module",
            p -> new DroneModuleItem(p, DroneTask.RANCH, 1));
    public static final DeferredItem<DroneModuleItem> FERTILIZER_MODULE = ITEMS.registerItem("fertilizer_module",
            p -> new DroneModuleItem(p, DroneTask.FERTILIZE, 1));
    public static final DeferredItem<DroneModuleItem> BUILDER_MODULE = ITEMS.registerItem("builder_module",
            p -> new DroneModuleItem(p, DroneTask.BUILD, 2));
    public static final DeferredItem<DroneModuleItem> FISHER_MODULE = ITEMS.registerItem("fisher_module",
            p -> new DroneModuleItem(p, DroneTask.FISH, 1));
    public static final DeferredItem<DroneModuleItem> BREEDER_MODULE = ITEMS.registerItem("breeder_module",
            p -> new DroneModuleItem(p, DroneTask.BREED, 1));

    // Addons
    public static final DeferredItem<DroneAddonItem> SPEED_ADDON = ITEMS.registerItem("speed_addon",
            p -> new DroneAddonItem(p, AddonType.SPEED));
    public static final DeferredItem<DroneAddonItem> STACK_ADDON = ITEMS.registerItem("stack_addon",
            p -> new DroneAddonItem(p, AddonType.STACK));
    public static final DeferredItem<DroneAddonItem> BATTERY_ADDON = ITEMS.registerItem("battery_addon",
            p -> new DroneAddonItem(p, AddonType.BATTERY));
    public static final DeferredItem<DroneAddonItem> FORTUNE_ADDON = ITEMS.registerItem("fortune_addon",
            p -> new DroneAddonItem(p, AddonType.FORTUNE));
    public static final DeferredItem<DroneAddonItem> SILK_TOUCH_ADDON = ITEMS.registerItem("silk_touch_addon",
            p -> new DroneAddonItem(p, AddonType.SILK_TOUCH));

    // Drone and tools
    public static final DeferredItem<DroneItem> DRONE = ITEMS.registerItem("drone", DroneItem::new);
    public static final DeferredItem<BlockItem> CRYSTALLIZER = ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTALLIZER);
    public static final DeferredItem<BlockItem> CRYSTALLIZER_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.CRYSTALLIZER_PORT,
            p -> p.component(DataComponents.LORE, lore("tooltip.cyberfarm.crystallizer_port", ChatFormatting.LIGHT_PURPLE)));
    public static final DeferredItem<Item> FUSION_CRYSTAL = ITEMS.registerSimpleItem("fusion_crystal", p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<Item> MECH_CORE = ITEMS.registerSimpleItem("mech_core", p -> p.rarity(Rarity.EPIC));
    public static final DeferredItem<MechFrameItem> MECH_FRAME = ITEMS.registerItem("mech_frame", MechFrameItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<BlockItem> MECH_HANGAR = ITEMS.registerSimpleBlockItem(ModBlocks.MECH_HANGAR);
    public static final DeferredItem<BlockItem> HANGAR_FRAME = ITEMS.registerSimpleBlockItem(ModBlocks.HANGAR_FRAME);
    public static final DeferredItem<BlockItem> HANGAR_PAD = ITEMS.registerSimpleBlockItem(ModBlocks.HANGAR_PAD);
    public static final DeferredItem<MechModuleItem> MECH_JET_MODULE = ITEMS.registerItem("mech_jet_module",
            p -> new MechModuleItem(p, MechModule.JET), p -> p.stacksTo(1));
    public static final DeferredItem<MechModuleItem> MECH_DRILL_MODULE = ITEMS.registerItem("mech_drill_module",
            p -> new MechModuleItem(p, MechModule.DRILL), p -> p.stacksTo(1));
    public static final DeferredItem<MechModuleItem> MECH_BLADE_MODULE = ITEMS.registerItem("mech_blade_module",
            p -> new MechModuleItem(p, MechModule.BLADE), p -> p.stacksTo(1));
    public static final DeferredItem<MechModuleItem> MECH_ARMOR_MODULE = ITEMS.registerItem("mech_armor_module",
            p -> new MechModuleItem(p, MechModule.ARMOR), p -> p.stacksTo(1));
    public static final DeferredItem<SurveyToolItem> SURVEY_TOOL = ITEMS.registerItem("survey_tool", SurveyToolItem::new);
    public static final DeferredItem<ProjectorItem> MULTIBLOCK_PROJECTOR = ITEMS.registerItem("multiblock_projector", ProjectorItem::new);

    /** One coloured, non-italic line under an item's name. */
    private static ItemLore lore(String key, ChatFormatting color) {
        return new ItemLore(List.of(Component.translatable(key).withStyle(style -> style.withColor(color).withItalic(false))));
    }
}
