package com.cyberfarm.block;

import com.cyberfarm.block.entity.WasteReprocessorBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class WasteReprocessorBlock extends MachineBlock {
    public static final MapCodec<WasteReprocessorBlock> CODEC = simpleCodec(WasteReprocessorBlock::new);
    private static final DustParticleOptions GLOW = new DustParticleOptions(0x6CFF6C, 1.0F);

    public WasteReprocessorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WasteReprocessorBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WasteReprocessorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.WASTE_REPROCESSOR.get());
    }

    /** A radioactive glow and a little smoke off the top while it reprocesses. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            Vec3 at = onTop(state, level, pos, random, 0.3);
            level.addParticle(GLOW, at.x, at.y, at.z, 0.0, 0.02, 0.0);
        }
        if (random.nextInt(10) == 0) {
            Vec3 at = onTop(state, level, pos, random, 0.2);
            level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0.0, 0.03, 0.0);
        }
    }
}
