package com.cyberfarm.block;

import com.cyberfarm.block.entity.ReactorControllerBlockEntity;
import com.cyberfarm.multiblock.TowerSize;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Fission Reactor controller: FORMED when its tower is complete, LIT while the core is burning fuel. */
public class ReactorControllerBlock extends MultiblockControllerBlock {
    public static final MapCodec<ReactorControllerBlock> CODEC = simpleCodec(ReactorControllerBlock::new);

    public ReactorControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ReactorControllerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return state.getValue(LIT) ? createTickerHelper(type, ModBlockEntities.REACTOR_CONTROLLER.get(), (lvl, pos, st, be) -> steamPlume(lvl, pos, st)) : null;
        }
        return machineTicker(level, type, ModBlockEntities.REACTOR_CONTROLLER.get());
    }

    /** Clouds of steam rising from the open rim of the tower, like a real cooling tower. */
    private static void steamPlume(Level level, BlockPos pos, BlockState state) {
        RandomSource random = level.getRandom();
        Direction back = state.getValue(FACING).getOpposite();
        TowerSize size = TowerSize.byOrdinal(state.getValue(SIZE));
        // The rim tops the tower, centred one base-width behind the controller (which sits on layer 1)
        double x = pos.getX() + 0.5 + back.getStepX() * size.base();
        double z = pos.getZ() + 0.5 + back.getStepZ() * size.base();
        double y = pos.getY() + size.height() - 0.8;
        double spread = size.radius(size.height()) * 1.4;
        for (int i = 0; i < size.ordinal() + 2; i++) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    x + (random.nextDouble() - 0.5) * spread, y, z + (random.nextDouble() - 0.5) * spread,
                    (random.nextDouble() - 0.5) * 0.01, 0.06 + random.nextDouble() * 0.04, (random.nextDouble() - 0.5) * 0.01);
        }
    }
}
