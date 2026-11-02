package com.cyberfarm.port;

import java.util.Set;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Constructors whose 26.x shape differs on 1.20.1. */
public final class PortTypes {
    private PortTypes() {}

    public static <T extends BlockEntity> BlockEntityType<T> blockEntity(BlockEntityType.BlockEntitySupplier<? extends T> factory, Set<Block> blocks) {
        return new BlockEntityType<>(factory, blocks, null);
    }
}
