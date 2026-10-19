package com.cyberfarm;

import com.cyberfarm.block.entity.MufflerBlockEntity;
import com.cyberfarm.client.DroneSoundInstance;
import com.cyberfarm.client.model.DroneModel;
import com.cyberfarm.client.renderer.CoolingTowerRenderer;
import com.cyberfarm.client.renderer.DroneRenderer;
import com.cyberfarm.client.screen.BioDigesterScreen;
import com.cyberfarm.client.screen.CombustionGeneratorScreen;
import com.cyberfarm.client.screen.DroneAssemblerScreen;
import com.cyberfarm.client.screen.DroneDockScreen;
import com.cyberfarm.client.screen.GasTurbineScreen;
import com.cyberfarm.client.screen.MethaneExtractorScreen;
import com.cyberfarm.client.screen.CoolantMixerScreen;
import com.cyberfarm.client.screen.MufflerScreen;
import com.cyberfarm.client.screen.ReactorScreen;
import com.cyberfarm.client.screen.WasteReprocessorScreen;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModEntities;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jspecify.annotations.Nullable;

// Client-only entry point: never loaded on a dedicated server.
@Mod(value = CyberFarm.MODID, dist = Dist.CLIENT)
public class CyberFarmClient {
    public CyberFarmClient(IEventBus modEventBus, ModContainer container) {
        // Mods screen > CyberFarm > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        modEventBus.addListener(CyberFarmClient::registerScreens);
        modEventBus.addListener(CyberFarmClient::registerRenderers);
        modEventBus.addListener(CyberFarmClient::registerLayers);
        modEventBus.addListener(CyberFarmClient::registerRenderStateModifiers);
        modEventBus.addListener(CyberFarmClient::registerFluidModels);
        NeoForge.EVENT_BUS.addListener(CyberFarmClient::startDroneSound);
        NeoForge.EVENT_BUS.addListener(CyberFarmClient::muffleSounds);
    }

    private static void startDroneSound(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof DroneEntity drone) {
            Minecraft.getInstance().getSoundManager().queueTickingSound(new DroneSoundInstance(drone));
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
        Identifier id = sound.getIdentifier();
        if (id.getNamespace().equals(CyberFarm.MODID)) {
            // The Geiger Counter is a warning, never muffled
            if (id.getPath().startsWith("geiger.")) {
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

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        event.register(ModMenus.DRONE_ASSEMBLER.get(), DroneAssemblerScreen::new);
        event.register(ModMenus.DRONE_DOCK.get(), DroneDockScreen::new);
        event.register(ModMenus.BIO_DIGESTER.get(), BioDigesterScreen::new);
        event.register(ModMenus.GAS_TURBINE.get(), GasTurbineScreen::new);
        event.register(ModMenus.METHANE_EXTRACTOR.get(), MethaneExtractorScreen::new);
        event.register(ModMenus.MUFFLER.get(), MufflerScreen::new);
        event.register(ModMenus.STEAM_TURBINE.get(), GasTurbineScreen::new);
        event.register(ModMenus.REACTOR.get(), ReactorScreen::new);
        event.register(ModMenus.COOLANT_MIXER.get(), CoolantMixerScreen::new);
        event.register(ModMenus.WASTE_REPROCESSOR.get(), WasteReprocessorScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DRONE.get(), DroneRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.REACTOR_CONTROLLER.get(), context -> new CoolingTowerRenderer());
    }

    /** Players carried by a Taxi drone hang from its hook: arm raised, legs straight. */
    private static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (avatar.getVehicle() instanceof DroneEntity) {
                    // The trident-throw pose lifts the arm straight above the head
                    if (state.mainArm == HumanoidArm.RIGHT) {
                        state.rightArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
                    } else {
                        state.leftArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
                    }
                    state.isPassenger = false;
                }
            }
        });
    }

    /** Gases never appear in the world, but tanks and recipe viewers of any mod draw them with these textures. */
    private static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(gasModel("biogas"), ModFluids.BIOGAS);
        event.register(gasModel("methane"), ModFluids.METHANE);
        event.register(gasModel("steam"), ModFluids.STEAM);
        event.register(liquidModel(0xFF1FB8A8), ModFluids.COOLANT, ModFluids.FLOWING_COOLANT);
        event.register(liquidModel(0xFF9FE4FF), ModFluids.CRYO_COOLANT, ModFluids.FLOWING_CRYO_COOLANT);
    }

    private static FluidModel.Unbaked gasModel(String name) {
        return new FluidModel.Unbaked(new Material(CyberFarm.id("block/" + name + "_still")),
                new Material(CyberFarm.id("block/" + name + "_flow")), null, (FluidTintSource) null);
    }

    /** Coolants look like water, animated and see-through, tinted in their own colour. */
    private static FluidModel.Unbaked liquidModel(int color) {
        return new FluidModel.Unbaked(new Material(Identifier.withDefaultNamespace("block/water_still")),
                new Material(Identifier.withDefaultNamespace("block/water_flow")),
                new Material(Identifier.withDefaultNamespace("block/water_overlay")), (FluidTintSource) state -> color);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DroneModel.LAYER, DroneModel::createBodyLayer);
    }
}
