package com.cyberfarm.port;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;

/**
 * 26.x asks the renderer for a block entity's render box; Forge 1.20.1 asks the block entity. The
 * client fills this when it registers its renderers, and {@link PortBlockEntity} looks it up.
 */
public final class RenderBoxes {
    static final Map<BlockEntityType<?>, Function<BlockEntity, AABB>> BOXES = new IdentityHashMap<>();

    private RenderBoxes() {}

    public static void register(BlockEntityType<?> type, Function<BlockEntity, AABB> box) {
        BOXES.put(type, box);
    }
}
