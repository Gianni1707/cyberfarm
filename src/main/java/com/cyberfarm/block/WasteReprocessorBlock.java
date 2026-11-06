package com.cyberfarm.block;

import com.cyberfarm.block.entity.WasteReprocessorBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A disc of four pockets before a shielded plate, with the radiation mark on the roof (see tools/machinemodels.py; WasteReprocessorRenderer draws the pockets). */
public class WasteReprocessorBlock extends LitMachineBlock {
    public static final MapCodec<WasteReprocessorBlock> CODEC = simpleCodec(WasteReprocessorBlock::new);
    /** Green outside the palette, on purpose: it says "radioactive". */
    private static final DustParticleOptions GLOW = new DustParticleOptions(0x6CFF6C, 1.0F);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 1, 16), Block.box(1, 1, 1, 15, 16, 15));

    public WasteReprocessorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WasteReprocessorBlock> codec() {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WasteReprocessorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.WASTE_REPROCESSOR.get());
    }

    /** A radioactive glow and a little smoke out of the top pocket, where the rod melts, while it reprocesses. */
    @Override
    protected void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction front = state.getValue(FACING);
        double x = pos.getX() + 0.5 + front.getStepX() * 4.9 / 16.0;
        double y = pos.getY() + 14.2 / 16.0;
        double z = pos.getZ() + 0.5 + front.getStepZ() * 4.9 / 16.0;
        if (random.nextInt(12) == 0) {
            level.addParticle(GLOW, x, y, z, 0.0, 0.02, 0.0);
        }
        if (random.nextInt(24) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.03, 0.0);
        }
    }
}
