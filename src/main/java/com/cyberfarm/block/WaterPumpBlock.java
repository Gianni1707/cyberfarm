package com.cyberfarm.block;

import com.cyberfarm.block.entity.WaterPumpBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A pumpjack over a water source (the model, see tools/machinemodels.py; WaterPumpRenderer draws what moves). Has no GUI: right-click tells what it is doing. */
public class WaterPumpBlock extends LitMachineBlock {
    public static final MapCodec<WaterPumpBlock> CODEC = simpleCodec(WaterPumpBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 1, 16), Block.box(1, 1, 1, 15, 8, 15), Block.box(1.5, 8, 1.5, 14.5, 15.5, 14.5));

    public WaterPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WaterPumpBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasPipePorts() {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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

    /** Now and then a splash on the water round the intake and a drip from under the deck. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SPLASH, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() - 0.11, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(16) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() - 0.02, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
        }
    }
}
