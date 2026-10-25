package com.cyberfarm.block;

import com.cyberfarm.block.entity.SwarmDockBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Builder Swarm Dock: the Drone Dock's pad, with room for four Mini Drones. */
public class SwarmDockBlock extends DroneDockBlock {
    public static final MapCodec<SwarmDockBlock> CODEC = simpleCodec(SwarmDockBlock::new);

    public SwarmDockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SwarmDockBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwarmDockBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.SWARM_DOCK.get());
    }
}
