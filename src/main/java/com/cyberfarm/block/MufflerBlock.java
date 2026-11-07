package com.cyberfarm.block;

import com.cyberfarm.block.entity.MufflerBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Right-click opens the GUI where the player picks what the Muffler silences; a weight on a coil bobs as it does (MufflerRenderer). */
public class MufflerBlock extends MachineBlock {
    public static final MapCodec<MufflerBlock> CODEC = simpleCodec(MufflerBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15.5, 15);

    public MufflerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<MufflerBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MufflerBlockEntity(pos, state);
    }
}
