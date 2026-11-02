package com.cyberfarm.port;

import net.minecraft.world.InteractionResult;

/**
 * The 26.x split of a block click into useItemOn then useWithoutItem, on the single 1.20.1
 * {@code use}: useItemOn's default answer, {@link #TRY_WITH_EMPTY_HAND}, goes on to useWithoutItem.
 */
public final class BlockUse {
    /** Stands for 26.x TRY_WITH_EMPTY_HAND; CyberFarm itself never returns CONSUME_PARTIAL. */
    public static final InteractionResult TRY_WITH_EMPTY_HAND = InteractionResult.CONSUME_PARTIAL;

    private BlockUse() {}
}
