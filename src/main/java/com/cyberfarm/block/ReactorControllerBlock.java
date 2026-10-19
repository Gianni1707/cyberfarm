package com.cyberfarm.block;

import com.cyberfarm.block.entity.ReactorControllerBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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

/** Fission Reactor controller: FORMED when its tower is complete, LIT while the core is burning fuel. */
public class ReactorControllerBlock extends MachineBlock {
    public static final MapCodec<ReactorControllerBlock> CODEC = simpleCodec(ReactorControllerBlock::new);
    public static final BooleanProperty FORMED = CyberFrameBlock.FORMED;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ReactorControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false).setValue(LIT, false));
    }

    @Override
    protected MapCodec<ReactorControllerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED, LIT);
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
        // The rim is 5 blocks above the controller, centred 2 blocks behind it
        double x = pos.getX() + 0.5 + back.getStepX() * 2;
        double z = pos.getZ() + 0.5 + back.getStepZ() * 2;
        double y = pos.getY() + 6.2;
        for (int i = 0; i < 2; i++) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    x + (random.nextDouble() - 0.5) * 2.5, y, z + (random.nextDouble() - 0.5) * 2.5,
                    (random.nextDouble() - 0.5) * 0.01, 0.06 + random.nextDouble() * 0.04, (random.nextDouble() - 0.5) * 0.01);
        }
    }
}
