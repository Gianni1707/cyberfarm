package com.cyberfarm.block;

import com.cyberfarm.block.entity.BioDigesterBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BioDigesterBlock extends MachineBlock {
    public static final MapCodec<BioDigesterBlock> CODEC = simpleCodec(BioDigesterBlock::new);

    public BioDigesterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BioDigesterBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BioDigesterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.BIO_DIGESTER.get());
    }

    /** Bubbles popping on top while it digests. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            Vec3 at = onTop(state, level, pos, random, 0.3);
            level.addParticle(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 0.0, 0.02, 0.0);
        }
    }
}
