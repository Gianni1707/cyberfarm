package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.BioDigesterBlock;
import com.cyberfarm.block.CombustionGeneratorBlock;
import com.cyberfarm.block.CoolantBlock;
import com.cyberfarm.block.CoolantMixerBlock;
import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.DroneAssemblerBlock;
import com.cyberfarm.block.DroneDockBlock;
import com.cyberfarm.block.EnergyConduitBlock;
import com.cyberfarm.block.FluidTankBlock;
import com.cyberfarm.block.GasPipeBlock;
import com.cyberfarm.block.GasTurbineBlock;
import com.cyberfarm.block.LiquidPipeBlock;
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
            DroneAssemblerBlock::new, ModBlocks::machine);

    public static final DeferredBlock<DroneDockBlock> DRONE_DOCK = BLOCKS.registerBlock("drone_dock",
            DroneDockBlock::new, p -> machine(p).noOcclusion().lightLevel(state -> 7));

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
            BioDigesterBlock::new, ModBlocks::machine);

    public static final DeferredBlock<GasTurbineBlock> GAS_TURBINE = BLOCKS.registerBlock("gas_turbine",
            GasTurbineBlock::new, ModBlocks::machine);

    public static final DeferredBlock<MethaneExtractorBlock> METHANE_EXTRACTOR = BLOCKS.registerBlock("methane_extractor",
            MethaneExtractorBlock::new,
            p -> machine(p).lightLevel(state -> state.getValue(MethaneExtractorBlock.FORMED) ? 9 : 0));

    /** Structural sci-fi frame, used by multiblocks such as the Methane Extractor chamber. */
    public static final DeferredBlock<CyberFrameBlock> CYBER_FRAME = BLOCKS.registerBlock("cyber_frame",
            CyberFrameBlock::new, p -> machine(p).lightLevel(state -> state.getValue(CyberFrameBlock.FORMED) ? 6 : 0));

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
            CoolantMixerBlock::new, ModBlocks::machine);
    public static final DeferredBlock<WaterPumpBlock> WATER_PUMP = BLOCKS.registerBlock("water_pump",
            WaterPumpBlock::new, ModBlocks::machine);
    public static final DeferredBlock<FluidTankBlock> LIQUID_TANK = BLOCKS.registerBlock("liquid_tank",
            p -> new FluidTankBlock(p, false), ModBlocks::machine);
    public static final DeferredBlock<FluidTankBlock> GAS_TANK = BLOCKS.registerBlock("gas_tank",
            p -> new FluidTankBlock(p, true), ModBlocks::machine);

    public static final DeferredBlock<WasteReprocessorBlock> WASTE_REPROCESSOR = BLOCKS.registerBlock("waste_reprocessor",
            WasteReprocessorBlock::new, ModBlocks::machine);
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

    private static BlockBehaviour.Properties machine(BlockBehaviour.Properties properties) {
        return properties
                .mapColor(MapColor.METAL)
                .strength(3.5F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}
