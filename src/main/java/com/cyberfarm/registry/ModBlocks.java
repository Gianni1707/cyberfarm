package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.BioDigesterBlock;
import com.cyberfarm.block.CombustionGeneratorBlock;
import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.DroneAssemblerBlock;
import com.cyberfarm.block.DroneDockBlock;
import com.cyberfarm.block.EnergyConduitBlock;
import com.cyberfarm.block.GasPipeBlock;
import com.cyberfarm.block.GasTurbineBlock;
import com.cyberfarm.block.MethaneExtractorBlock;
import com.cyberfarm.block.MufflerBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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

    public static final DeferredBlock<EnergyConduitBlock> ENERGY_CONDUIT = BLOCKS.registerBlock("energy_conduit",
            EnergyConduitBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(1.0F, 3.0F).sound(SoundType.COPPER).noOcclusion());

    public static final DeferredBlock<GasPipeBlock> GAS_PIPE = BLOCKS.registerBlock("gas_pipe",
            GasPipeBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(1.0F, 3.0F).sound(SoundType.COPPER).noOcclusion());

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

    private static BlockBehaviour.Properties machine(BlockBehaviour.Properties properties) {
        return properties
                .mapColor(MapColor.METAL)
                .strength(3.5F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}
