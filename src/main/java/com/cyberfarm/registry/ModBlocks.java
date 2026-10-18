package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.CombustionGeneratorBlock;
import com.cyberfarm.block.DroneAssemblerBlock;
import com.cyberfarm.block.DroneDockBlock;
import com.cyberfarm.block.EnergyConduitBlock;

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

    private static BlockBehaviour.Properties machine(BlockBehaviour.Properties properties) {
        return properties
                .mapColor(MapColor.METAL)
                .strength(3.5F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}
