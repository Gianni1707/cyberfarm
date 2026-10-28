package com.cyberfarm.block;

import com.cyberfarm.block.entity.LaserTurretBlockEntity;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A pedestal under a turning head (drawn by its renderer). Has no GUI: right-click tells what it is doing. */
public class LaserTurretBlock extends MachineBlock {
    public static final MapCodec<LaserTurretBlock> CODEC = simpleCodec(LaserTurretBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 6, 15), Block.box(3, 6, 3, 13, 14, 13));

    public LaserTurretBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<LaserTurretBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LaserTurretBlockEntity turret) {
            player.sendOverlayMessage(turret.status());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaserTurretBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.LASER_TURRET.get());
    }

    /** A wisp of smoke off the barrel now and then while it fights. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) == 0) {
            Vec3 at = Vec3.atBottomCenterOf(pos).add(0.0, LaserTurretBlockEntity.MUZZLE_HEIGHT + 0.2, 0.0);
            level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0.0, 0.02, 0.0);
        }
    }
}
