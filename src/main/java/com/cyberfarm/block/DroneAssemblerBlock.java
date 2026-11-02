package com.cyberfarm.block;


import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;

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

    // The base, a pillar on each corner and the cap
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(0, 14, 0, 16, 16, 16),
            Block.box(0, 4, 0, 2, 14, 2), Block.box(14, 4, 0, 16, 14, 2), Block.box(0, 4, 14, 2, 14, 16), Block.box(14, 4, 14, 16, 14, 16));

    public DroneAssemblerBlock(Properties properties) {
        super(properties);
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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
