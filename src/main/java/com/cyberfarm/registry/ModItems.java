package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.drone.AddonType;
import com.cyberfarm.drone.DroneTask;
import com.cyberfarm.item.DroneAddonItem;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.item.DroneItem;
import com.cyberfarm.item.DroneModuleItem;
import com.cyberfarm.item.MobCapsuleItem;
import com.cyberfarm.item.SurveyToolItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CyberFarm.MODID);

    // Machines
    public static final DeferredItem<BlockItem> COMBUSTION_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.COMBUSTION_GENERATOR);
    public static final DeferredItem<BlockItem> DRONE_ASSEMBLER = ITEMS.registerSimpleBlockItem(ModBlocks.DRONE_ASSEMBLER);
    public static final DeferredItem<BlockItem> DRONE_DOCK = ITEMS.registerSimpleBlockItem(ModBlocks.DRONE_DOCK);
    public static final DeferredItem<BlockItem> ENERGY_CONDUIT = ITEMS.registerSimpleBlockItem(ModBlocks.ENERGY_CONDUIT);
    public static final DeferredItem<BlockItem> GAS_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.GAS_PIPE);
    public static final DeferredItem<BlockItem> BIO_DIGESTER = ITEMS.registerSimpleBlockItem(ModBlocks.BIO_DIGESTER);
    public static final DeferredItem<BlockItem> GAS_TURBINE = ITEMS.registerSimpleBlockItem(ModBlocks.GAS_TURBINE);
    public static final DeferredItem<BlockItem> METHANE_EXTRACTOR = ITEMS.registerSimpleBlockItem(ModBlocks.METHANE_EXTRACTOR);
    public static final DeferredItem<BlockItem> CYBER_FRAME = ITEMS.registerSimpleBlockItem(ModBlocks.CYBER_FRAME);
    public static final DeferredItem<BlockItem> MUFFLER = ITEMS.registerSimpleBlockItem(ModBlocks.MUFFLER);
    public static final DeferredItem<MobCapsuleItem> MOB_CAPSULE = ITEMS.registerItem("mob_capsule", MobCapsuleItem::new);

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
    public static final DeferredItem<SurveyToolItem> SURVEY_TOOL = ITEMS.registerItem("survey_tool", SurveyToolItem::new);
}
