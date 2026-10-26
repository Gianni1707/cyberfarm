package com.cyberfarm.block;

import com.cyberfarm.block.entity.GasTurbineBlockEntity;
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

public class GasTurbineBlock extends LitMachineBlock {
    public static final MapCodec<GasTurbineBlock> CODEC = simpleCodec(GasTurbineBlock::new);

    public GasTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GasTurbineBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasTurbineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.GAS_TURBINE.get());
    }

    /** Exhaust smoke out of the top while it burns gas. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            Vec3 at = onTop(state, level, pos, random, 0.15);
            level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0.0, 0.05, 0.0);
        }
    }
}
