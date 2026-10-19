package com.cyberfarm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * A pool of reactor coolant: wading through it slowly freezes you (frost on the screen, damage once
 * fully frozen; leather armor keeps the cold out, as in powder snow). The slowdown is in
 * {@link com.cyberfarm.fluid.CoolantFluidType}.
 */
public class CoolantBlock extends LiquidBlock {
    /** Vanilla thaws frozen entities by 2 ticks every tick they are out of powder snow. */
    private static final int VANILLA_THAW = 2;

    private final int freezeRate;

    /** {@code freezeRate}: frozen ticks gained each tick spent in the pool. */
    public CoolantBlock(FlowingFluid fluid, Properties properties, int freezeRate) {
        super(fluid, properties);
        this.freezeRate = freezeRate;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        super.entityInside(state, level, pos, entity, effects, intersects);
        // Called for every coolant block the entity touches: act once per tick, from the block at its feet
        if (!pos.equals(entity.blockPosition())) {
            return;
        }
        if (entity.canFreeze()) {
            entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze(), entity.getTicksFrozen() + VANILLA_THAW + freezeRate));
        }
    }
}
