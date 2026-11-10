package com.cyberfarm;

import com.cyberfarm.block.entity.MufflerBlockEntity;
import com.cyberfarm.client.DroneDownwash;
import com.cyberfarm.client.DroneSoundInstance;
import com.cyberfarm.client.EffectRenderer;
import com.cyberfarm.client.MachineOverlay;
import com.cyberfarm.client.VillageRange;
import com.cyberfarm.client.DissolvingMobRenderer;
import com.cyberfarm.client.MechClient;
import com.cyberfarm.client.MechLoopSound;
import com.cyberfarm.client.model.MechModel;
import com.cyberfarm.client.renderer.MechRenderer;
import com.cyberfarm.client.renderer.MovingPartsRenderer;
import com.cyberfarm.client.ProjectionRenderer;
import com.cyberfarm.client.model.DroneModel;
import com.cyberfarm.client.renderer.AcidSynthesizerRenderer;
import com.cyberfarm.client.renderer.AssemblerRenderer;
import com.cyberfarm.client.renderer.CrystallizerRenderer;
import com.cyberfarm.client.renderer.DockRenderer;
import com.cyberfarm.client.renderer.FabricatorRenderer;
import com.cyberfarm.client.renderer.HangarRenderer;
import com.cyberfarm.client.renderer.MethaneChamberRenderer;
import com.cyberfarm.client.renderer.CoolingTowerRenderer;
import com.cyberfarm.client.renderer.DroneRenderer;
import com.cyberfarm.client.renderer.FluidTankRenderer;
import com.cyberfarm.client.renderer.LaserTurretRenderer;
import com.cyberfarm.client.renderer.HologramRenderer;
import com.cyberfarm.client.renderer.ResonanceCoreRenderer;
import com.cyberfarm.client.renderer.SteamEngineRenderer;
import com.cyberfarm.client.renderer.TurbineRenderer;
import com.cyberfarm.client.screen.AcidSynthesizerScreen;
import com.cyberfarm.client.screen.ElectrolyzerScreen;
import com.cyberfarm.client.screen.FabricatorScreen;
import com.cyberfarm.client.screen.PyrolysisOvenScreen;
import com.cyberfarm.client.screen.SupplyTerminalScreen;
import com.cyberfarm.client.screen.CrystallizerScreen;
import com.cyberfarm.client.screen.SteamEngineScreen;
import com.cyberfarm.client.screen.MechHangarScreen;
import com.cyberfarm.client.screen.BioDigesterScreen;
import com.cyberfarm.client.screen.CombustionGeneratorScreen;
import com.cyberfarm.client.screen.CoolantMixerScreen;
import com.cyberfarm.client.screen.DroneAssemblerScreen;
import com.cyberfarm.client.screen.DroneDockScreen;
import com.cyberfarm.client.screen.GasTurbineScreen;
import com.cyberfarm.client.screen.ItemFilterScreen;
import com.cyberfarm.client.screen.MethaneExtractorScreen;
import com.cyberfarm.client.screen.MufflerScreen;
import com.cyberfarm.client.screen.ReactorScreen;
import com.cyberfarm.client.screen.ResonanceCoreScreen;
import com.cyberfarm.client.screen.TurbineScreen;
import com.cyberfarm.client.screen.WasteReprocessorScreen;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.entity.MechEntity;
import com.cyberfarm.entity.MechModule;
import com.cyberfarm.item.DroneItem;
import com.cyberfarm.item.MobCapsuleItem;
import com.cyberfarm.item.SurveyModePayload;
import com.cyberfarm.item.SurveyToolItem;
import com.cyberfarm.port.RenderBoxes;
import com.cyberfarm.port.client.LevelRenderEvents;
import com.cyberfarm.port.net.ClientPacketDistributor;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModEntities;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jspecify.annotations.Nullable;

// Client-only entry point: called by CyberFarm on the client only, never loaded on a dedicated server.
// shortcut: Forge 1.20.1 has no built-in config screen, so the options live in the config files only.
public final class CyberFarmClient {
    private CyberFarmClient() {}

    public static void init(IEventBus modEventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        modEventBus.addListener(CyberFarmClient::setup);
        modEventBus.addListener(CyberFarmClient::registerRenderers);
        modEventBus.addListener(CyberFarmClient::registerLayers);
        modEventBus.addListener(DissolvingMobRenderer::addLayers);
        modEventBus.addListener(MechClient::registerKeys);
        modEventBus.addListener(MechClient::registerHud);
        modEventBus.addListener(MachineOverlay::register);
        MinecraftForge.EVENT_BUS.addListener(MechClient::tick);
        MinecraftForge.EVENT_BUS.addListener(VillageRange::tick);
        MinecraftForge.EVENT_BUS.addListener(MechClient::hideVanillaHud);
        MinecraftForge.EVENT_BUS.addListener(MechClient::hideHand);
        MinecraftForge.EVENT_BUS.addListener(CyberFarmClient::hangFromDrone);
        MinecraftForge.EVENT_BUS.addListener(MechClient::hidePilot);
        MinecraftForge.EVENT_BUS.addListener(MechClient::restorePilot);
        MinecraftForge.EVENT_BUS.addListener(DissolvingMobRenderer::pre);
        MinecraftForge.EVENT_BUS.addListener(DissolvingMobRenderer::post);
        MinecraftForge.EVENT_BUS.addListener(CyberFarmClient::startDroneSound);
        MinecraftForge.EVENT_BUS.addListener(CyberFarmClient::muffleSounds);
        MinecraftForge.EVENT_BUS.addListener(CyberFarmClient::scrollSurveyMode);
        MinecraftForge.EVENT_BUS.addListener(LevelRenderEvents.extract(ProjectionRenderer::extract, EffectRenderer::extract)
                .submit(ProjectionRenderer::submit, EffectRenderer::submit)::onRender);
        MinecraftForge.EVENT_BUS.addListener(DroneDownwash::tick);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerScreens();
            // Item model override "cyberfarm:mini_drone": the drone was built on a Mini Drone Frame, so it gets its own sprite
            ItemProperties.register(ModItems.DRONE.get(), CyberFarm.id("mini_drone"),
                    (stack, level, owner, seed) -> DroneItem.getModules(stack).isMini() ? 1.0F : 0.0F);
            // "cyberfarm:filled": the capsule holds a mob
            ItemProperties.register(ModItems.MOB_CAPSULE.get(), CyberFarm.id("filled"),
                    (stack, level, owner, seed) -> MobCapsuleItem.hasMob(stack) ? 1.0F : 0.0F);
        });
    }

    private static void startDroneSound(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof DroneEntity drone) {
            Minecraft.getInstance().getSoundManager().queueTickingSound(new DroneSoundInstance(drone));
        }
        if (event.getLevel().isClientSide() && event.getEntity() instanceof MechEntity mech) {
            Minecraft.getInstance().getSoundManager().queueTickingSound(new MechLoopSound(mech, MechModule.JET));
            Minecraft.getInstance().getSoundManager().queueTickingSound(new MechLoopSound(mech, MechModule.DRILL));
        }
    }

    /** Sneak + mouse wheel with the Survey Tool in hand changes its mode instead of the hotbar slot. */
    private static void scrollSurveyMode(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.screen == null && minecraft.player.isShiftKeyDown()
                && minecraft.player.getMainHandItem().getItem() instanceof SurveyToolItem && event.getScrollDelta() != 0) {
            // Wheel up goes back, like the hotbar
            ClientPacketDistributor.sendToServer(new SurveyModePayload(event.getScrollDelta() > 0 ? -1 : 1));
            event.setCanceled(true);
        }
    }

    /** Players carried by a Taxi drone hang from its hook: arm raised, legs straight (DroneEntity#shouldRiderSit). */
    private static void hangFromDrone(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() instanceof DroneEntity) {
            PlayerModel<?> model = event.getRenderer().getModel();
            // The spear-throw pose lifts the arm straight above the head
            if (event.getEntity().getMainArm() == HumanoidArm.RIGHT) {
                model.rightArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
            } else {
                model.leftArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
            }
        }
    }

    /** Silences a sound when a Muffler nearby is set to silence its kind. Players' own sounds have no kind. */
    private static void muffleSounds(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        // The drone hum checks the muffler itself every tick, so it can come back when the drone flies away
        if (sound == null || sound.isRelative() || sound instanceof DroneSoundInstance) {
            return;
        }
        MufflerBlockEntity.Category category = categoryOf(sound);
        Level level = Minecraft.getInstance().level;
        if (category != null && level != null && MufflerBlockEntity.isMuffled(level, sound.getX(), sound.getY(), sound.getZ(), category)) {
            event.setSound(null);
        }
    }

    private static MufflerBlockEntity.@Nullable Category categoryOf(SoundInstance sound) {
        ResourceLocation id = sound.getLocation();
        if (id.getNamespace().equals(CyberFarm.MODID)) {
            // The Geiger Counter is a warning, and the mech a vehicle: never muffled
            if (id.getPath().startsWith("geiger.") || id.getPath().startsWith("mech.")) {
                return null;
            }
            return id.getPath().startsWith("drone.") ? MufflerBlockEntity.Category.DRONES : MufflerBlockEntity.Category.MACHINES;
        }
        if (ClientConfig.isExtraMuted(id)) {
            return MufflerBlockEntity.Category.OTHER_MACHINES;
        }
        return switch (sound.getSource()) {
            case HOSTILE -> MufflerBlockEntity.Category.MONSTERS;
            case NEUTRAL -> MufflerBlockEntity.Category.ANIMALS;
            default -> null;
        };
    }

    private static void registerScreens() {
        MenuScreens.register(ModMenus.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        MenuScreens.register(ModMenus.DRONE_ASSEMBLER.get(), DroneAssemblerScreen::new);
        MenuScreens.register(ModMenus.DRONE_DOCK.get(), DroneDockScreen::new);
        MenuScreens.register(ModMenus.BIO_DIGESTER.get(), BioDigesterScreen::new);
        MenuScreens.register(ModMenus.GAS_TURBINE.get(), GasTurbineScreen::new);
        MenuScreens.register(ModMenus.METHANE_EXTRACTOR.get(), MethaneExtractorScreen::new);
        MenuScreens.register(ModMenus.MUFFLER.get(), MufflerScreen::new);
        MenuScreens.register(ModMenus.TURBINE.get(), TurbineScreen::new);
        MenuScreens.register(ModMenus.ITEM_FILTER.get(), ItemFilterScreen::new);
        MenuScreens.register(ModMenus.REACTOR.get(), ReactorScreen::new);
        MenuScreens.register(ModMenus.COOLANT_MIXER.get(), CoolantMixerScreen::new);
        MenuScreens.register(ModMenus.ACID_SYNTHESIZER.get(), AcidSynthesizerScreen::new);
        MenuScreens.register(ModMenus.ELECTROLYZER.get(), ElectrolyzerScreen::new);
        MenuScreens.register(ModMenus.PYROLYSIS_OVEN.get(), PyrolysisOvenScreen::new);
        MenuScreens.register(ModMenus.FABRICATOR.get(), FabricatorScreen::new);
        MenuScreens.register(ModMenus.SUPPLY_TERMINAL.get(), SupplyTerminalScreen::new);
        MenuScreens.register(ModMenus.CRYSTALLIZER.get(), CrystallizerScreen::new);
        MenuScreens.register(ModMenus.STEAM_ENGINE.get(), SteamEngineScreen::new);
        MenuScreens.register(ModMenus.MECH_HANGAR.get(), MechHangarScreen::new);
        MenuScreens.register(ModMenus.WASTE_REPROCESSOR.get(), WasteReprocessorScreen::new);
        MenuScreens.register(ModMenus.RESONANCE_CORE.get(), ResonanceCoreScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DRONE.get(), DroneRenderer::new);
        event.registerEntityRenderer(ModEntities.MECH.get(), MechRenderer::new);
        blockEntity(event, ModBlockEntities.REACTOR_CONTROLLER.get(), context -> new CoolingTowerRenderer());
        blockEntity(event, ModBlockEntities.TURBINE_CONTROLLER.get(), context -> new TurbineRenderer());
        blockEntity(event, ModBlockEntities.FLUID_TANK.get(), context -> new FluidTankRenderer());
        blockEntity(event, ModBlockEntities.LASER_TURRET.get(), context -> new LaserTurretRenderer());
        blockEntity(event, ModBlockEntities.HOLOGRAM_TABLE.get(), context -> new HologramRenderer(context.getFont()));
        blockEntity(event, ModBlockEntities.DRONE_ASSEMBLER.get(), AssemblerRenderer::new);
        blockEntity(event, ModBlockEntities.FABRICATOR.get(), context -> new FabricatorRenderer());
        blockEntity(event, ModBlockEntities.DRONE_DOCK.get(), context -> new DockRenderer<>());
        blockEntity(event, ModBlockEntities.SWARM_DOCK.get(), context -> new DockRenderer<>());
        blockEntity(event, ModBlockEntities.GAS_TURBINE.get(), context -> new MovingPartsRenderer<>(MovingPartsRenderer.Part.FAN));
        blockEntity(event, ModBlockEntities.WATER_PUMP.get(), context -> new MovingPartsRenderer<>(MovingPartsRenderer.Part.PISTON));
        blockEntity(event, ModBlockEntities.BIO_DIGESTER.get(), context -> new MovingPartsRenderer<>(MovingPartsRenderer.Part.PADDLE));
        blockEntity(event, ModBlockEntities.METHANE_EXTRACTOR.get(), context -> new MethaneChamberRenderer());
        blockEntity(event, ModBlockEntities.RESONANCE_CORE.get(), context -> new ResonanceCoreRenderer());
        blockEntity(event, ModBlockEntities.STEAM_ENGINE.get(), context -> new SteamEngineRenderer());
        blockEntity(event, ModBlockEntities.ACID_SYNTHESIZER.get(), context -> new AcidSynthesizerRenderer());
        blockEntity(event, ModBlockEntities.CRYSTALLIZER.get(), context -> new CrystallizerRenderer());
        blockEntity(event, ModBlockEntities.MECH_HANGAR.get(), context -> new HangarRenderer());
    }

    /** Registers a renderer, and its render box with the block entity type, which is what 1.20.1 culls by. */
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> void blockEntity(EntityRenderersEvent.RegisterRenderers event, BlockEntityType<T> type,
            BlockEntityRendererProvider<T> provider) {
        event.registerBlockEntityRenderer(type, context -> {
            var renderer = provider.create(context);
            if (renderer instanceof com.cyberfarm.port.client.BlockEntityRenderer<?, ?> port) {
                var typed = (com.cyberfarm.port.client.BlockEntityRenderer<T, ?>) port;
                RenderBoxes.register(type, entity -> typed.getRenderBoundingBox((T) entity));
            }
            return renderer;
        });
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DroneModel.LAYER, DroneModel::createBodyLayer);
        event.registerLayerDefinition(MechModel.LAYER, MechModel::createBodyLayer);
    }
}
