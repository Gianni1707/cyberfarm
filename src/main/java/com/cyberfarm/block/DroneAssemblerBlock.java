package com.cyberfarm.block;

import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
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

/** An open chamber: the drone being assembled shows inside it (see {@code AssemblerRenderer}). */
public class DroneAssemblerBlock extends LitMachineBlock {
    public static final MapCodec<DroneAssemblerBlock> CODEC = simpleCodec(DroneAssemblerBlock::new);

    // The base, then the spine and the print head's arm (whichever way it faces)
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(0.25, 4, 0.25, 15.75, 16, 15.75));

    public DroneAssemblerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<DroneAssemblerBlock> codec() {
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
        return new DroneAssemblerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.DRONE_ASSEMBLER.get());
    }
}
