package com.cyberfarm.block.entity;

import com.cyberfarm.port.PortBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds nothing: on Forge 1.20.1 only block entities expose capabilities, so the ports of a
 * multiblock (inlets, outlets, dynamo, pylon) carry this one, and their providers registered by
 * block in CyberFarm answer through it.
 */
public class CapabilityPortBlockEntity extends PortBlockEntity {
    public CapabilityPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAPABILITY_PORT.get(), pos, state);
    }
}
