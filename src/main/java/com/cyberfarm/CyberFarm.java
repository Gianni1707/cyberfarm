package com.cyberfarm;

import org.slf4j.Logger;

import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModCreativeTabs;
import com.cyberfarm.registry.ModItems;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(CyberFarm.MODID)
public class CyberFarm {
    public static final String MODID = "cyberfarm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CyberFarm(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
