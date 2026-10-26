package com.cyberfarm.block;

import com.cyberfarm.block.entity.CombustionGeneratorBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModSounds;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public class CombustionGeneratorBlock extends MachineBlock {
    public static final MapCodec<CombustionGeneratorBlock> CODEC = simpleCodec(CombustionGeneratorBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CombustionGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected MapCodec<CombustionGeneratorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CombustionGeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.COMBUSTION_GENERATOR.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ModSounds.GENERATOR_CRACKLE.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        // Exhaust smoke from the vent on top
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.05, z, 0.0, 0.03, 0.0);

        Direction facing = state.getValue(FACING);
        double offset = random.nextDouble() * 0.5 - 0.25;
        double fx = pos.getX() + 0.5 + facing.getStepX() * 0.52 + (facing.getAxis() == Direction.Axis.Z ? offset : 0);
        double fz = pos.getZ() + 0.5 + facing.getStepZ() * 0.52 + (facing.getAxis() == Direction.Axis.X ? offset : 0);
        level.addParticle(ParticleTypes.FLAME, fx, pos.getY() + 0.3, fz, 0.0, 0.0, 0.0);
    }

    /** Nothing extra: it already smokes and flames from {@link #animateTick} while lit. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {}
}
