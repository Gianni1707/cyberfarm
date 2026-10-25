package com.cyberfarm.block;

import com.cyberfarm.block.entity.AcidSynthesizerBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AcidSynthesizerBlock extends MachineBlock {
    public static final MapCodec<AcidSynthesizerBlock> CODEC = simpleCodec(AcidSynthesizerBlock::new);
    private static final DustParticleOptions FUMES = new DustParticleOptions(0x9BE04A, 0.8F);

    public AcidSynthesizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AcidSynthesizerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AcidSynthesizerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.ACID_SYNTHESIZER.get());
    }

    /** Green fumes rising off the top while it works. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            Vec3 at = onTop(state, level, pos, random, 0.3);
            level.addParticle(FUMES, at.x, at.y, at.z, 0.0, 0.03, 0.0);
        }
    }
}
