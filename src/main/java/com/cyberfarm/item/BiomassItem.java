package com.cyberfarm.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Biomass, for the Bio-Digester, or a weak fertilizer: it is always used up but grows the plant one time in three.
 * A press makes it for far fewer crops than a composter wants for bone meal, so at full strength the composter would have no use.
 */
public class BiomassItem extends BoneMealItem {
    private static final float GROW_CHANCE = 1.0F / 3.0F;

    public BiomassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (level instanceof ServerLevel server && state.getBlock() instanceof BonemealableBlock plant
                && plant.isValidBonemealTarget(level, pos, state) && level.getRandom().nextFloat() >= GROW_CHANCE) {
            context.getItemInHand().shrink(1);
            server.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.25, 0.25, 0.25, 0.0);
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
