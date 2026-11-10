package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.menu.AcidSynthesizerMenu;
import com.cyberfarm.menu.BiomassPressMenu;
import com.cyberfarm.menu.ElectrolyzerMenu;
import com.cyberfarm.menu.FabricatorMenu;
import com.cyberfarm.menu.PyrolysisOvenMenu;
import com.cyberfarm.menu.SupplyTerminalMenu;
import com.cyberfarm.menu.CrystallizerMenu;
import com.cyberfarm.menu.SteamEngineMenu;
import com.cyberfarm.menu.MechHangarMenu;
import com.cyberfarm.menu.BioDigesterMenu;
import com.cyberfarm.menu.CombustionGeneratorMenu;
import com.cyberfarm.menu.CoolantMixerMenu;
import com.cyberfarm.menu.DroneAssemblerMenu;
import com.cyberfarm.menu.DroneDockMenu;
import com.cyberfarm.menu.GasTurbineMenu;
import com.cyberfarm.menu.ItemFilterMenu;
import com.cyberfarm.menu.MethaneExtractorMenu;
import com.cyberfarm.menu.MufflerMenu;
import com.cyberfarm.menu.ReactorMenu;
import com.cyberfarm.menu.TurbineMenu;
import com.cyberfarm.menu.ResonanceCoreMenu;
import com.cyberfarm.menu.WasteReprocessorMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import com.cyberfarm.port.DeferredHolder;
import com.cyberfarm.port.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CyberFarm.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CombustionGeneratorMenu>> COMBUSTION_GENERATOR = MENUS.register("combustion_generator",
            () -> IForgeMenuType.create((id, inventory, buf) -> new CombustionGeneratorMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<DroneAssemblerMenu>> DRONE_ASSEMBLER = MENUS.register("drone_assembler",
            () -> IForgeMenuType.create((id, inventory, buf) -> new DroneAssemblerMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<BioDigesterMenu>> BIO_DIGESTER = MENUS.register("bio_digester",
            () -> IForgeMenuType.create((id, inventory, buf) -> new BioDigesterMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<GasTurbineMenu>> GAS_TURBINE = MENUS.register("gas_turbine",
            () -> IForgeMenuType.create((id, inventory, buf) -> new GasTurbineMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<MethaneExtractorMenu>> METHANE_EXTRACTOR = MENUS.register("methane_extractor",
            () -> IForgeMenuType.create((id, inventory, buf) -> new MethaneExtractorMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<TurbineMenu>> TURBINE = MENUS.register("turbine",
            () -> IForgeMenuType.create((id, inventory, buf) -> new TurbineMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<MechHangarMenu>> MECH_HANGAR = MENUS.register("mech_hangar",
            () -> IForgeMenuType.create((id, inventory, buf) -> new MechHangarMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<SteamEngineMenu>> STEAM_ENGINE = MENUS.register("steam_engine",
            () -> IForgeMenuType.create((id, inventory, buf) -> new SteamEngineMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<BiomassPressMenu>> BIOMASS_PRESS = MENUS.register("biomass_press",
            () -> IForgeMenuType.create((id, inventory, buf) -> new BiomassPressMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<ElectrolyzerMenu>> ELECTROLYZER = MENUS.register("electrolyzer",
            () -> IForgeMenuType.create((id, inventory, buf) -> new ElectrolyzerMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<com.cyberfarm.menu.TransmitterDishMenu>> TRANSMITTER_DISH = MENUS.register("transmitter_dish",
            () -> IForgeMenuType.create(com.cyberfarm.menu.TransmitterDishMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<com.cyberfarm.menu.ReceiverDishMenu>> RECEIVER_DISH = MENUS.register("receiver_dish",
            () -> IForgeMenuType.create(com.cyberfarm.menu.ReceiverDishMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PyrolysisOvenMenu>> PYROLYSIS_OVEN = MENUS.register("pyrolysis_oven",
            () -> IForgeMenuType.create((id, inventory, buf) -> new PyrolysisOvenMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<FabricatorMenu>> FABRICATOR = MENUS.register("fabricator",
            () -> IForgeMenuType.create((id, inventory, buf) -> new FabricatorMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<SupplyTerminalMenu>> SUPPLY_TERMINAL = MENUS.register("supply_terminal",
            () -> IForgeMenuType.create((id, inventory, buf) -> new SupplyTerminalMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<AcidSynthesizerMenu>> ACID_SYNTHESIZER = MENUS.register("acid_synthesizer",
            () -> IForgeMenuType.create((id, inventory, buf) -> new AcidSynthesizerMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<CoolantMixerMenu>> COOLANT_MIXER = MENUS.register("coolant_mixer",
            () -> IForgeMenuType.create((id, inventory, buf) -> new CoolantMixerMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<WasteReprocessorMenu>> WASTE_REPROCESSOR = MENUS.register("waste_reprocessor",
            () -> IForgeMenuType.create((id, inventory, buf) -> new WasteReprocessorMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<CrystallizerMenu>> CRYSTALLIZER = MENUS.register("crystallizer",
            () -> IForgeMenuType.create((id, inventory, buf) -> new CrystallizerMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<ResonanceCoreMenu>> RESONANCE_CORE = MENUS.register("resonance_core",
            () -> IForgeMenuType.create((id, inventory, buf) -> new ResonanceCoreMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorMenu>> REACTOR = MENUS.register("reactor",
            () -> IForgeMenuType.create((id, inventory, buf) -> new ReactorMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<ItemFilterMenu>> ITEM_FILTER = MENUS.register("item_filter",
            () -> IForgeMenuType.create((id, inventory, buf) -> new ItemFilterMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<MufflerMenu>> MUFFLER = MENUS.register("muffler",
            () -> IForgeMenuType.create((id, inventory, buf) -> new MufflerMenu(id, inventory)));

    public static final DeferredHolder<MenuType<?>, MenuType<DroneDockMenu>> DRONE_DOCK = MENUS.register("drone_dock",
            () -> IForgeMenuType.create((id, inventory, buf) -> new DroneDockMenu(id, inventory)));
}
