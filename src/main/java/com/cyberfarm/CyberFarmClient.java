package com.cyberfarm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Client-only entry point: never loaded on a dedicated server.
@Mod(value = CyberFarm.MODID, dist = Dist.CLIENT)
public class CyberFarmClient {
    public CyberFarmClient(ModContainer container) {
        // Mods screen > CyberFarm > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
