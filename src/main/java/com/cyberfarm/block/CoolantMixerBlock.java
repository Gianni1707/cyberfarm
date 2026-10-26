package com.cyberfarm.block;

import com.cyberfarm.block.entity.CoolantMixerBlockEntity;
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

public class CoolantMixerBlock extends LitMachineBlock {
    public static final MapCodec<CoolantMixerBlock> CODEC = simpleCodec(CoolantMixerBlock::new);

    public CoolantMixerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CoolantMixerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoolantMixerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.COOLANT_MIXER.get());
    }

    /** Frost drifting off the top and drops running down its sides while it mixes. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            Vec3 at = onTop(state, level, pos, random, 0.35);
            level.addParticle(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 0.0, 0.01, 0.0);
        }
        if (random.nextInt(12) == 0) {
            Vec3 at = onSide(state, level, pos, random);
            level.addParticle(ParticleTypes.DRIPPING_WATER, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }
}
