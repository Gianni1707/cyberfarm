package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CyberFarm.MODID);

    // Base crafting component shared by every drone chassis.
    public static final DeferredItem<Item> DRONE_CORE = ITEMS.registerSimpleItem("drone_core");
}
