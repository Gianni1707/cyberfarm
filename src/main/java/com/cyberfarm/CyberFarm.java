package com.cyberfarm;

import org.slf4j.Logger;

import com.cyberfarm.block.FluidTankBlock;
import com.cyberfarm.block.HiddenFrameBlock;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.entity.CrystallizerBlockEntity;
import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.block.entity.MachineStatusPayload;
import com.cyberfarm.block.entity.MechHangarBlockEntity;
import com.cyberfarm.block.entity.MethaneExtractorBlockEntity;
import com.cyberfarm.block.entity.ResonanceCoreBlockEntity;
import com.cyberfarm.effect.EffectPayload;
import com.cyberfarm.energy.ForwardingEnergyHandler;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.entity.MechEntity;
import com.cyberfarm.entity.MechEvents;
import com.cyberfarm.entity.MechExitPayload;
import com.cyberfarm.entity.MechTogglePayload;
import com.cyberfarm.item.FluidBucketItem;
import com.cyberfarm.item.ProjectorSelectPayload;
import com.cyberfarm.item.SurveyModePayload;
import com.cyberfarm.multiblock.Schematics;
import com.cyberfarm.multiblock.SchematicsPayload;
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
import com.cyberfarm.registry.ModVillagers;
import com.cyberfarm.village.MechanistChoicePayload;
import com.cyberfarm.village.MechanistDialogue;
import com.cyberfarm.village.MechanistDialoguePayload;
import com.cyberfarm.village.Blueprints;
import com.cyberfarm.village.BlueprintsPayload;
import com.cyberfarm.village.MechanistTrades;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import com.cyberfarm.port.Caps;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import com.cyberfarm.worldgen.BauxiteOreModifier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import com.cyberfarm.port.net.PortNetwork;
import com.cyberfarm.port.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Mod(CyberFarm.MODID)
public class CyberFarm {
    public static final String MODID = "cyberfarm";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MODID);

    static {
        BIOME_MODIFIERS.register("bauxite_ore", () -> BauxiteOreModifier.CODEC);
    }

    public CyberFarm() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModEntities.DATA_SERIALIZERS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModVillagers.POI_TYPES.register(modEventBus);
        ModVillagers.PROFESSIONS.register(modEventBus);
        BIOME_MODIFIERS.register(modEventBus);

        registerPayloads();
        modEventBus.addListener(CyberFarm::registerCapabilities);
        modEventBus.addListener(CyberFarm::registerAttributes);
                DroneDockBlockEntity.ANCHOR_TICKETS.register();
        MinecraftForge.EVENT_BUS.addListener(HandbookGifting::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(Blueprints::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(MechanistTrades::onVillagerTrades);
        MinecraftForge.EVENT_BUS.addListener(MethaneExtractorBlock::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(FluidTankBlock::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(FluidBucketItem::fillFromBlock);
        MinecraftForge.EVENT_BUS.addListener(MechHangarBlockEntity::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(MechEvents::onDismount);
        MinecraftForge.EVENT_BUS.addListener(MechEvents::onDamage);
        MinecraftForge.EVENT_BUS.addListener(MechEvents::onBreakSpeed);
        MinecraftForge.EVENT_BUS.addListener(MechEvents::onHarvestCheck);
        MinecraftForge.EVENT_BUS.addListener(MechEvents::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(Radiation::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(Schematics::onServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(Schematics::onDatapackSync);
        MinecraftForge.EVENT_BUS.addListener(MechanistDialogue::onInteract);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CyberFarmClient.init(modEventBus);
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    // Exposes FE, items and fluids to other mods' cables, pipes and hoppers (answered by PortBlockEntity).
    private static void registerCapabilities(FMLCommonSetupEvent event) {
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.COMBUSTION_GENERATOR.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.COMBUSTION_GENERATOR.get(), (be, side) -> be.getItems());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.DRONE_ASSEMBLER.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.DRONE_ASSEMBLER.get(), (be, side) -> be.getItems());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.DRONE_DOCK.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.DRONE_DOCK.get(), (be, side) -> be.getExternalItems());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.SWARM_DOCK.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.SWARM_DOCK.get(), (be, side) -> be.getExternalItems());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.ENERGY_CONDUIT.get(), (be, side) -> be.getEnergyHandler());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.GAS_PIPE.get(), (be, side) -> be.getFluidHandler());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.BIO_DIGESTER.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.BIOMASS_PRESS.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.BIO_DIGESTER.get(), (be, side) -> be.getItems());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.BIOMASS_PRESS.get(), (be, side) -> be.automationItems());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.BIO_DIGESTER.get(), (be, side) -> be.getTank());
        // A press's frame has its funnel on top: a hopper there feeds the press (the frame carries a block entity for it, see HiddenFrameBlock)
        Caps.registerBlock(Caps.ITEM, (level, pos, state, be, side) -> {
            BlockPos below = pos.below();
            return state.getValue(HiddenFrameBlock.TO_MACHINE) == Direction.DOWN && level.getBlockState(below).is(ModBlocks.BIOMASS_PRESS.get())
                    ? Caps.items(level, below, side) : null;
        }, ModBlocks.HIDDEN_FRAME.get());
        // The extraction chamber takes acid in and gives methane out only through its ports
        Caps.registerBlock(Caps.FLUID, (level, pos, state, be, side) -> {
            MethaneExtractorBlockEntity extractor = MethaneExtractorBlockEntity.ofChamberBlock(level, pos);
            return extractor != null ? extractor.getAcidInlet() : null;
        }, ModBlocks.ACID_INLET.get());
        Caps.registerBlock(Caps.FLUID, (level, pos, state, be, side) -> {
            MethaneExtractorBlockEntity extractor = MethaneExtractorBlockEntity.ofChamberBlock(level, pos);
            return extractor != null ? extractor.getMethaneOutlet() : null;
        }, ModBlocks.METHANE_OUTLET.get());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.GAS_TURBINE.get(), (be, side) -> be.getTank());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.GAS_TURBINE.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.TURBINE_CONTROLLER.get(), (be, side) -> be.isFormed() ? be.portFluids() : null);
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.TURBINE_CONTROLLER.get(), (be, side) -> be.isFormed() ? be.portEnergy() : null);
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.WATER_PUMP.get(), (be, side) -> be.getTank());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.DOCK_TANK.get(), (be, side) -> be.tank(side));
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.FLUID_TANK.get(), (be, side) -> side != null && be.joined(side) ? null : be.pipes());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.COOLANT_MIXER.get(), (be, side) -> be.getFluids());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.WASTE_REPROCESSOR.get(), (be, side) -> be.automationItems());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.WASTE_REPROCESSOR.get(), (be, side) -> be.getEnergy());
        // The Resonance Core takes and gives power only through its pylons
        Caps.registerBlock(Caps.ENERGY, (level, pos, state, be, side) -> new ForwardingEnergyHandler(() -> {
            ResonanceCoreBlockEntity core = ResonanceCoreBlockEntity.linkedCore(level, pos);
            return core != null ? core.getEnergy() : null;
        }), ModBlocks.STABILIZER_PYLON.get());
        // Neither from behind the boiler, where the row goes
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.STEAM_ENGINE.get(),
                (be, side) -> side == be.getBlockState().getValue(MachineBlock.FACING).getOpposite() ? null : be.getEnergy());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.STEAM_ENGINE.get(),
                (be, side) -> side == be.getBlockState().getValue(MachineBlock.FACING).getOpposite() ? null : be.getTank());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.MECH_HANGAR.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.COOLANT_MIXER.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.COOLANT_MIXER.get(), (be, side) -> be.getItems());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.ACID_SYNTHESIZER.get(), (be, side) -> be.getFluids());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.ACID_SYNTHESIZER.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.ACID_SYNTHESIZER.get(), (be, side) -> be.getItems());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.ELECTROLYZER.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.TRANSMITTER_DISH.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.RECEIVER_DISH.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.ELECTROLYZER.get(), (be, side) -> be.automationItems());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.PYROLYSIS_OVEN.get(), (be, side) -> be.getFluids());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.PYROLYSIS_OVEN.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.PYROLYSIS_OVEN.get(), (be, side) -> be.automationItems());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.FABRICATOR.get(), (be, side) -> be.getFluids());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.FABRICATOR.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.FABRICATOR.get(), (be, side) -> be.automationItems());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.SUPPLY_TERMINAL.get(), (be, side) -> be.getItems());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.WATER_PUMP.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.LASER_TURRET.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.CRYSTALLIZER.get(), (be, side) -> be.portFluids());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.CRYSTALLIZER.get(), (be, side) -> be.getEnergy());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.CRYSTALLIZER.get(), (be, side) -> be.portItems());
        // A Crystallizer Port passes everything on to the formed Crystallizer of its chamber
        Caps.registerBlock(Caps.FLUID, (level, pos, state, be, side) -> {
            CrystallizerBlockEntity crystallizer = CrystallizerBlockEntity.ofChamberBlock(level, pos);
            return crystallizer != null ? crystallizer.portFluids() : null;
        }, ModBlocks.CRYSTALLIZER_PORT.get());
        Caps.registerBlock(Caps.ENERGY, (level, pos, state, be, side) -> {
            CrystallizerBlockEntity crystallizer = CrystallizerBlockEntity.ofChamberBlock(level, pos);
            return crystallizer != null ? crystallizer.getEnergy() : null;
        }, ModBlocks.CRYSTALLIZER_PORT.get());
        Caps.registerBlock(Caps.ITEM, (level, pos, state, be, side) -> {
            CrystallizerBlockEntity crystallizer = CrystallizerBlockEntity.ofChamberBlock(level, pos);
            return crystallizer != null ? crystallizer.portItems() : null;
        }, ModBlocks.CRYSTALLIZER_PORT.get());
        // So do the formed chamber's floor edges and roof middle, without a port
        Caps.registerBlock(Caps.FLUID, (level, pos, state, be, side) -> {
            CrystallizerBlockEntity crystallizer = CrystallizerBlockEntity.ofTap(level, pos, state, side);
            return crystallizer != null ? crystallizer.portFluids() : null;
        }, ModBlocks.CYBER_FRAME.get());
        Caps.registerBlock(Caps.ENERGY, (level, pos, state, be, side) -> {
            CrystallizerBlockEntity crystallizer = CrystallizerBlockEntity.ofTap(level, pos, state, side);
            return crystallizer != null ? crystallizer.getEnergy() : null;
        }, ModBlocks.CYBER_FRAME.get());
        Caps.registerBlock(Caps.ITEM, (level, pos, state, be, side) -> {
            CrystallizerBlockEntity crystallizer = CrystallizerBlockEntity.ofTap(level, pos, state, side);
            return crystallizer != null ? crystallizer.portItems() : null;
        }, ModBlocks.CYBER_FRAME.get());
        // The reactor is reached through its ports (and the controller itself), only while the tower is formed
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.REACTOR_PORT.get(), (be, side) -> be.getItemHandler());
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.REACTOR_PORT.get(), (be, side) -> be.getFluidHandler());
        Caps.registerBlockEntity(Caps.ENERGY, ModBlockEntities.REACTOR_PORT.get(), (be, side) -> be.getEnergyHandler());
        Caps.registerBlockEntity(Caps.ITEM, ModBlockEntities.REACTOR_CONTROLLER.get(),
                (be, side) -> be.isFormed() ? be.portItems() : null);
        Caps.registerBlockEntity(Caps.FLUID, ModBlockEntities.REACTOR_CONTROLLER.get(),
                (be, side) -> be.isFormed() ? be.portFluids() : null);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DRONE.get(), DroneEntity.createAttributes().build());
        event.put(ModEntities.MECH.get(), MechEntity.createAttributes().build());
    }

    private static void registerPayloads() {
        PortNetwork.registrar("1").playToServer(SurveyModePayload.TYPE, SurveyModePayload.STREAM_CODEC, SurveyModePayload::handle)
                .playToServer(ProjectorSelectPayload.TYPE, ProjectorSelectPayload.STREAM_CODEC, ProjectorSelectPayload::handle)
                .playToServer(MechExitPayload.TYPE, MechExitPayload.STREAM_CODEC, MechExitPayload::handle)
                .playToServer(MechTogglePayload.TYPE, MechTogglePayload.STREAM_CODEC, MechTogglePayload::handle)
                .playToServer(MechanistChoicePayload.TYPE, MechanistChoicePayload.STREAM_CODEC, MechanistChoicePayload::handle)
                .playToServer(com.cyberfarm.energy.DishRenamePayload.TYPE, com.cyberfarm.energy.DishRenamePayload.STREAM_CODEC, com.cyberfarm.energy.DishRenamePayload::handle)
                .playToClient(MechanistDialoguePayload.TYPE, MechanistDialoguePayload.STREAM_CODEC, MechanistDialoguePayload::handle)
                .playToClient(SchematicsPayload.TYPE, SchematicsPayload.STREAM_CODEC, SchematicsPayload::handle)
                .playToClient(MachineStatusPayload.TYPE, MachineStatusPayload.STREAM_CODEC, MachineStatusPayload::handle)
                .playToClient(EffectPayload.TYPE, EffectPayload.STREAM_CODEC, EffectPayload::handle)
                .playToClient(BlueprintsPayload.TYPE, BlueprintsPayload.STREAM_CODEC, BlueprintsPayload::handle);
        PortNetwork.register();
    }
}
