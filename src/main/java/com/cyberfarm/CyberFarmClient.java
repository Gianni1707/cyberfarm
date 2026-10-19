package com.cyberfarm;

import com.cyberfarm.client.model.DroneModel;
import com.cyberfarm.client.renderer.DroneRenderer;
import com.cyberfarm.client.screen.CombustionGeneratorScreen;
import com.cyberfarm.client.screen.DroneAssemblerScreen;
import com.cyberfarm.client.screen.DroneDockScreen;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.registry.ModEntities;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

// Client-only entry point: never loaded on a dedicated server.
@Mod(value = CyberFarm.MODID, dist = Dist.CLIENT)
public class CyberFarmClient {
    public CyberFarmClient(IEventBus modEventBus, ModContainer container) {
        // Mods screen > CyberFarm > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(CyberFarmClient::registerScreens);
        modEventBus.addListener(CyberFarmClient::registerRenderers);
        modEventBus.addListener(CyberFarmClient::registerLayers);
        modEventBus.addListener(CyberFarmClient::registerRenderStateModifiers);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMBUSTION_GENERATOR.get(), CombustionGeneratorScreen::new);
        event.register(ModMenus.DRONE_ASSEMBLER.get(), DroneAssemblerScreen::new);
        event.register(ModMenus.DRONE_DOCK.get(), DroneDockScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DRONE.get(), DroneRenderer::new);
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

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DroneModel.LAYER, DroneModel::createBodyLayer);
    }
}
