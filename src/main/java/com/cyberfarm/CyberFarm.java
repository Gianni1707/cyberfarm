package com.cyberfarm;

import org.slf4j.Logger;

import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.item.FluidBucketItem;
import com.cyberfarm.item.SurveyModePayload;
import com.cyberfarm.radiation.Radiation;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModCreativeTabs;
import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModEffects;
import com.cyberfarm.registry.ModEntities;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModMenus;
import com.cyberfarm.registry.ModSounds;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(CyberFarm.MODID)
public class CyberFarm {
    public static final String MODID = "cyberfarm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CyberFarm(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);

        modEventBus.addListener(CyberFarm::registerCapabilities);
        modEventBus.addListener(CyberFarm::registerAttributes);
        modEventBus.addListener(CyberFarm::registerPayloads);
        NeoForge.EVENT_BUS.addListener(HandbookGifting::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(MethaneExtractorBlock::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(FluidBucketItem::fillFromBlock);
        NeoForge.EVENT_BUS.addListener(Radiation::onLevelTick);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    // Exposes FE and items to other mods' cables, pipes and hoppers.
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.COMBUSTION_GENERATOR.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.COMBUSTION_GENERATOR.get(), (be, side) -> be.getItems());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.DRONE_ASSEMBLER.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.DRONE_ASSEMBLER.get(), (be, side) -> be.getItems());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.DRONE_DOCK.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.DRONE_DOCK.get(), (be, side) -> be.getItems());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ENERGY_CONDUIT.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.GAS_PIPE.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.BIO_DIGESTER.get(), (be, side) -> be.getItems());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.BIO_DIGESTER.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.METHANE_EXTRACTOR.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.GAS_TURBINE.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.GAS_TURBINE.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.TURBINE_CONTROLLER.get(), (be, side) -> be.isFormed() ? be.portFluids() : null);
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.TURBINE_CONTROLLER.get(), (be, side) -> be.isFormed() ? be.portEnergy() : null);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.WATER_PUMP.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_TANK.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.COOLANT_MIXER.get(), (be, side) -> be.getFluids());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.WASTE_REPROCESSOR.get(), (be, side) -> be.getItems());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.WASTE_REPROCESSOR.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.COOLANT_MIXER.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.COOLANT_MIXER.get(), (be, side) -> be.getItems());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.WATER_PUMP.get(), (be, side) -> be.getEnergy());
        // The reactor is reached through its ports (and the controller itself), only while the tower is formed
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.REACTOR_PORT.get(), (be, side) -> be.getItemHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.REACTOR_PORT.get(), (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.REACTOR_PORT.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.REACTOR_CONTROLLER.get(),
                (be, side) -> be.isFormed() ? be.portItems() : null);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.REACTOR_CONTROLLER.get(),
                (be, side) -> be.isFormed() ? be.portFluids() : null);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DRONE.get(), DroneEntity.createAttributes().build());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(SurveyModePayload.TYPE, SurveyModePayload.STREAM_CODEC, SurveyModePayload::handle);
    }
}
