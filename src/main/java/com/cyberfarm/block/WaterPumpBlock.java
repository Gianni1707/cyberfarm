package com.cyberfarm.block;

import com.cyberfarm.block.entity.WaterPumpBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Has no GUI: right-click tells what the pump is doing. */
public class WaterPumpBlock extends MachineBlock {
    public static final MapCodec<WaterPumpBlock> CODEC = simpleCodec(WaterPumpBlock::new);

    public WaterPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WaterPumpBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WaterPumpBlockEntity pump) {
            player.sendOverlayMessage(pump.status());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterPumpBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.WATER_PUMP.get());
    }

    /** Splashes and drips round its foot while it pumps. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            Vec3 at = onSide(state, level, pos, random);
            level.addParticle(ParticleTypes.SPLASH, at.x, pos.getY() + 0.1, at.z, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(10) == 0) {
            Vec3 at = onSide(state, level, pos, random);
            level.addParticle(ParticleTypes.DRIPPING_WATER, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }
}
