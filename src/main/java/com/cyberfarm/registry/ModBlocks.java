package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.AcidSynthesizerBlock;
import com.cyberfarm.block.CrystallizerBlock;
import com.cyberfarm.block.SteamEngineBlock;
import com.cyberfarm.block.SwarmDockBlock;
import com.cyberfarm.block.HologramTableBlock;
import com.cyberfarm.block.BioDigesterBlock;
import com.cyberfarm.block.CombustionGeneratorBlock;
import com.cyberfarm.block.CoolantBlock;
import com.cyberfarm.block.CoolantMixerBlock;
import com.cyberfarm.block.ContainmentGlassBlock;
import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.IntakeGateBlock;
import com.cyberfarm.block.MechHangarBlock;
import com.cyberfarm.block.DroneAssemblerBlock;
import com.cyberfarm.block.DroneDockBlock;
import com.cyberfarm.block.EnergyConduitBlock;
import com.cyberfarm.block.FluidTankBlock;
import com.cyberfarm.block.GasPipeBlock;
import com.cyberfarm.block.GasTurbineBlock;
import com.cyberfarm.block.LiquidPipeBlock;
import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.block.MufflerBlock;
import com.cyberfarm.block.ReactorCasingBlock;
import com.cyberfarm.block.ReactorControllerBlock;
import com.cyberfarm.block.ReactorPortBlock;
import com.cyberfarm.block.ResonanceCellBlock;
import com.cyberfarm.block.ResonanceCoreBlock;
import com.cyberfarm.block.StabilizerPylonBlock;
import com.cyberfarm.block.TurbineControllerBlock;
import com.cyberfarm.block.WasteReprocessorBlock;
import com.cyberfarm.block.WaterPumpBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CyberFarm.MODID);

    public static final DeferredBlock<CombustionGeneratorBlock> COMBUSTION_GENERATOR = BLOCKS.registerBlock("combustion_generator",
            CombustionGeneratorBlock::new,
            p -> machine(p).lightLevel(state -> state.getValue(CombustionGeneratorBlock.LIT) ? 13 : 0));

    public static final DeferredBlock<DroneAssemblerBlock> DRONE_ASSEMBLER = BLOCKS.registerBlock("drone_assembler",
            DroneAssemblerBlock::new, ModBlocks::litMachine);

    public static final DeferredBlock<DroneDockBlock> DRONE_DOCK = BLOCKS.registerBlock("drone_dock",
            DroneDockBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> 7));
    public static final DeferredBlock<SwarmDockBlock> SWARM_DOCK = BLOCKS.registerBlock("swarm_dock",
            SwarmDockBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> 7));
    public static final DeferredBlock<HologramTableBlock> HOLOGRAM_TABLE = BLOCKS.registerBlock("hologram_table",
            HologramTableBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> 9));

    // Cables and pipes in three tiers, rates comparable to Immersive Engineering and Mekanism
    public static final DeferredBlock<EnergyConduitBlock> ENERGY_CONDUIT = BLOCKS.registerBlock("energy_conduit",
            p -> new EnergyConduitBlock(p, 2_000),
            p -> p.mapColor(MapColor.METAL).strength(1.0F, 3.0F).sound(SoundType.COPPER).noOcclusion());

    public static final DeferredBlock<EnergyConduitBlock> ADVANCED_ENERGY_CONDUIT = BLOCKS.registerBlock("advanced_energy_conduit",
            p -> new EnergyConduitBlock(p, 16_000), ModBlocks::pipe);
    public static final DeferredBlock<EnergyConduitBlock> ELITE_ENERGY_CONDUIT = BLOCKS.registerBlock("elite_energy_conduit",
            p -> new EnergyConduitBlock(p, 128_000), ModBlocks::pipe);

    public static final DeferredBlock<GasPipeBlock> GAS_PIPE = BLOCKS.registerBlock("gas_pipe",
            p -> new GasPipeBlock(p, 1_000),
            p -> p.mapColor(MapColor.METAL).strength(1.0F, 3.0F).sound(SoundType.COPPER).noOcclusion());

    public static final DeferredBlock<GasPipeBlock> ADVANCED_GAS_PIPE = BLOCKS.registerBlock("advanced_gas_pipe",
            p -> new GasPipeBlock(p, 4_000), ModBlocks::pipe);
    public static final DeferredBlock<GasPipeBlock> ELITE_GAS_PIPE = BLOCKS.registerBlock("elite_gas_pipe",
            p -> new GasPipeBlock(p, 16_000), ModBlocks::pipe);

    public static final DeferredBlock<LiquidPipeBlock> LIQUID_PIPE = BLOCKS.registerBlock("liquid_pipe",
            p -> new LiquidPipeBlock(p, 1_000), ModBlocks::pipe);
    public static final DeferredBlock<LiquidPipeBlock> ADVANCED_LIQUID_PIPE = BLOCKS.registerBlock("advanced_liquid_pipe",
            p -> new LiquidPipeBlock(p, 4_000), ModBlocks::pipe);
    public static final DeferredBlock<LiquidPipeBlock> ELITE_LIQUID_PIPE = BLOCKS.registerBlock("elite_liquid_pipe",
            p -> new LiquidPipeBlock(p, 16_000), ModBlocks::pipe);


    public static final DeferredBlock<BioDigesterBlock> BIO_DIGESTER = BLOCKS.registerBlock("bio_digester",
            BioDigesterBlock::new, ModBlocks::litMachine);

    public static final DeferredBlock<GasTurbineBlock> GAS_TURBINE = BLOCKS.registerBlock("gas_turbine",
            GasTurbineBlock::new, ModBlocks::litMachine);

    public static final DeferredBlock<MethaneExtractorBlock> METHANE_EXTRACTOR = BLOCKS.registerBlock("methane_extractor",
            MethaneExtractorBlock::new,
            p -> machine(p).noOcclusion().lightLevel(state -> state.getValue(MethaneExtractorBlock.FORMED) ? 9 : 0));

    /** Structural sci-fi frame, used by multiblocks such as the Methane Extractor chamber. */
    // The chamber's blocks never hide their neighbours' faces: once formed they are drawn as one smooth tank
    public static final DeferredBlock<CyberFrameBlock> CYBER_FRAME = BLOCKS.registerBlock("cyber_frame",
            CyberFrameBlock::new, ModBlocks::chamberPart);
    public static final DeferredBlock<CyberFrameBlock> ACID_INLET = BLOCKS.registerBlock("acid_inlet",
            CyberFrameBlock::new, ModBlocks::chamberPart);
    public static final DeferredBlock<CyberFrameBlock> METHANE_OUTLET = BLOCKS.registerBlock("methane_outlet",
            CyberFrameBlock::new, ModBlocks::chamberPart);
    public static final DeferredBlock<IntakeGateBlock> INTAKE_GATE = BLOCKS.registerBlock("intake_gate",
            IntakeGateBlock::new, ModBlocks::chamberPart);

    public static final DeferredBlock<MufflerBlock> MUFFLER = BLOCKS.registerBlock("muffler",
            MufflerBlock::new, p -> p.mapColor(MapColor.WOOL).strength(0.8F).sound(SoundType.WOOL));

    // Nuclear
    public static final DeferredBlock<Block> URANIUM_ORE = BLOCKS.registerSimpleBlock("uranium_ore",
            p -> p.mapColor(MapColor.STONE).strength(3.0F, 3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(state -> 3));
    public static final DeferredBlock<Block> DEEPSLATE_URANIUM_ORE = BLOCKS.registerSimpleBlock("deepslate_uranium_ore",
            p -> p.mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops().lightLevel(state -> 3));

    public static final DeferredBlock<ReactorControllerBlock> REACTOR_CONTROLLER = BLOCKS.registerBlock("reactor_controller",
            ReactorControllerBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> state.getValue(ReactorControllerBlock.LIT) ? 12 : 0));
    public static final DeferredBlock<ReactorCasingBlock> REACTOR_CASING = BLOCKS.registerBlock("reactor_casing",
            ReactorCasingBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> state.getValue(CyberFrameBlock.FORMED) ? 4 : 0));
    public static final DeferredBlock<ReactorPortBlock> REACTOR_PORT = BLOCKS.registerBlock("reactor_port",
            ReactorPortBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> state.getValue(ReactorPortBlock.FORMED) ? 6 : 0));
    public static final DeferredBlock<TurbineControllerBlock> TURBINE_CONTROLLER = BLOCKS.registerBlock("turbine_controller",
            TurbineControllerBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> state.getValue(TurbineControllerBlock.LIT) ? 10 : 0));
    public static final DeferredBlock<ReactorCasingBlock> TURBINE_CASING = BLOCKS.registerBlock("turbine_casing",
            ReactorCasingBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> state.getValue(CyberFrameBlock.FORMED) ? 4 : 0));
    public static final DeferredBlock<ReactorPortBlock> TURBINE_PORT = BLOCKS.registerBlock("turbine_port",
            ReactorPortBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> state.getValue(ReactorPortBlock.FORMED) ? 6 : 0));
    public static final DeferredBlock<CoolantMixerBlock> COOLANT_MIXER = BLOCKS.registerBlock("coolant_mixer",
            CoolantMixerBlock::new, ModBlocks::litMachine);

    // Walls of the Methane Extractor's chamber: see-through, tinted, framed
    public static final DeferredBlock<ContainmentGlassBlock> CONTAINMENT_GLASS = BLOCKS.registerBlock("containment_glass",
            ContainmentGlassBlock::new, p -> p.mapColor(MapColor.NONE).strength(1.5F, 6.0F).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn(Blocks::never).isRedstoneConductor((s, l, q) -> false).isSuffocating((s, l, q) -> false)
                    .isViewBlocking((s, l, q) -> false));

    // Sulfur in the Nether's netherrack, and the machine that turns it into acid
    public static final DeferredBlock<Block> NETHER_SULFUR_ORE = BLOCKS.registerSimpleBlock("nether_sulfur_ore",
            p -> p.mapColor(MapColor.NETHER).strength(3.0F, 3.0F).sound(SoundType.NETHER_ORE).requiresCorrectToolForDrops());
    public static final DeferredBlock<AcidSynthesizerBlock> ACID_SYNTHESIZER = BLOCKS.registerBlock("acid_synthesizer",
            AcidSynthesizerBlock::new, ModBlocks::litMachine);
    // Steam Engine: the boiler controls a row of cylinders closed by a dynamo
    // A complete engine is drawn whole by the boiler's renderer: its blocks hide and never cull their neighbours
    public static final DeferredBlock<SteamEngineBlock> STEAM_ENGINE = BLOCKS.registerBlock("steam_engine",
            SteamEngineBlock::new, p -> machine(p).noOcclusion());
    public static final DeferredBlock<CyberFrameBlock> STEAM_ENGINE_CYLINDER = BLOCKS.registerBlock("steam_engine_cylinder",
            CyberFrameBlock::new, p -> machine(p).noOcclusion());
    public static final DeferredBlock<CyberFrameBlock> STEAM_ENGINE_DYNAMO = BLOCKS.registerBlock("steam_engine_dynamo",
            CyberFrameBlock::new, p -> machine(p).noOcclusion());
    // Crystallizer: grows the Fusion Crystal at the heart of a mech's core
    public static final DeferredBlock<CrystallizerBlock> CRYSTALLIZER = BLOCKS.registerBlock("crystallizer",
            CrystallizerBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> 6));
    /** Takes the place of any Cyber Frame of the chamber: acid, power and items in, the crystal out. Stays visible when formed. */
    public static final DeferredBlock<CyberFrameBlock> CRYSTALLIZER_PORT = BLOCKS.registerBlock("crystallizer_port",
            CyberFrameBlock::new, ModBlocks::chamberPart);
    // Mech Hangar: the console at a corner, frame pillars and beams, the charging pad
    public static final DeferredBlock<MechHangarBlock> MECH_HANGAR = BLOCKS.registerBlock("mech_hangar",
            MechHangarBlock::new, ModBlocks::machine);
    public static final DeferredBlock<CyberFrameBlock> HANGAR_FRAME = BLOCKS.registerBlock("hangar_frame",
            CyberFrameBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> state.getValue(CyberFrameBlock.FORMED) ? 5 : 0));
    public static final DeferredBlock<CyberFrameBlock> HANGAR_PAD = BLOCKS.registerBlock("hangar_pad",
            CyberFrameBlock::new, p -> machine(p).lightLevel(state -> state.getValue(CyberFrameBlock.FORMED) ? 8 : 0));
    public static final DeferredBlock<WaterPumpBlock> WATER_PUMP = BLOCKS.registerBlock("water_pump",
            WaterPumpBlock::new, ModBlocks::litMachine);
    public static final DeferredBlock<FluidTankBlock> LIQUID_TANK = BLOCKS.registerBlock("liquid_tank",
            p -> new FluidTankBlock(p, false), ModBlocks::machine);
    public static final DeferredBlock<FluidTankBlock> GAS_TANK = BLOCKS.registerBlock("gas_tank",
            p -> new FluidTankBlock(p, true), ModBlocks::machine);

    public static final DeferredBlock<WasteReprocessorBlock> WASTE_REPROCESSOR = BLOCKS.registerBlock("waste_reprocessor",
            WasteReprocessorBlock::new, ModBlocks::litMachine);
    /** Treated nuclear waste: inert glass, safe to store or build with. */
    public static final DeferredBlock<Block> VITRIFIED_WASTE = BLOCKS.registerSimpleBlock("vitrified_waste",
            p -> p.mapColor(MapColor.COLOR_GREEN).strength(3.0F, 9.0F).sound(SoundType.GLASS).requiresCorrectToolForDrops().lightLevel(state -> 2));

    // Coolant pools in the world; Cryo Coolant slows and freezes harder
    public static final DeferredBlock<CoolantBlock> COOLANT = BLOCKS.registerBlock("coolant",
            p -> new CoolantBlock(ModFluids.COOLANT.get(), p, 1), ModBlocks::liquid);
    public static final DeferredBlock<CoolantBlock> CRYO_COOLANT = BLOCKS.registerBlock("cryo_coolant",
            p -> new CoolantBlock(ModFluids.CRYO_COOLANT.get(), p, 4), ModBlocks::liquid);

    // Energy storage: the core's crystal, its shell cells and the pylons' emitters glow
    public static final DeferredBlock<ResonanceCoreBlock> RESONANCE_CORE = BLOCKS.registerBlock("resonance_core",
            ResonanceCoreBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> 10));
    public static final DeferredBlock<ResonanceCellBlock> RESONANCE_CELL = BLOCKS.registerBlock("resonance_cell",
            ResonanceCellBlock::new, p -> machine(p).noOcclusion().sound(SoundType.GLASS).lightLevel(state -> 5));
    public static final DeferredBlock<StabilizerPylonBlock> STABILIZER_PYLON = BLOCKS.registerBlock("stabilizer_pylon",
            StabilizerPylonBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> 6));

    private static BlockBehaviour.Properties liquid(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.ICE).replaceable().noCollision().strength(100.0F).pushReaction(PushReaction.DESTROY)
                .noLootTable().liquid().sound(SoundType.EMPTY);
    }

    private static BlockBehaviour.Properties pipe(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).strength(1.0F, 3.0F).sound(SoundType.COPPER).noOcclusion();
    }

    private static BlockBehaviour.Properties litMachine(BlockBehaviour.Properties properties) {
        return machine(properties).lightLevel(LitMachineBlock::lightLevel);
    }

    private static BlockBehaviour.Properties machine(BlockBehaviour.Properties properties) {
        return properties
                .mapColor(MapColor.METAL)
                .strength(3.5F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties chamberPart(BlockBehaviour.Properties properties) {
        return machine(properties).noOcclusion().lightLevel(state -> state.getValue(CyberFrameBlock.FORMED) ? 6 : 0);
    }
}
