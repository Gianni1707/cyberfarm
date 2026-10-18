package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.menu.CombustionGeneratorMenu;
import com.cyberfarm.menu.DroneAssemblerMenu;
import com.cyberfarm.menu.DroneDockMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CyberFarm.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CombustionGeneratorMenu>> COMBUSTION_GENERATOR = MENUS.register("combustion_generator",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new CombustionGeneratorMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<DroneAssemblerMenu>> DRONE_ASSEMBLER = MENUS.register("drone_assembler",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new DroneAssemblerMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<DroneDockMenu>> DRONE_DOCK = MENUS.register("drone_dock",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new DroneDockMenu(id, inventory)));
}
