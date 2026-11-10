package com.cyberfarm.block;

import com.cyberfarm.client.renderer.FrameDroneRenderer;
import com.cyberfarm.multiblock.Blueprint;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import com.cyberfarm.block.entity.TurbineControllerBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Steam Turbine controller: FORMED when its hall is complete (SIZE says which), LIT while the rotor turns. */
public class TurbineControllerBlock extends MultiblockControllerBlock {

    public TurbineControllerBlock(Properties properties) {
        super(properties);
    }


    /** Until it is complete, a click sends out the drones that show what it lacks, instead of opening the GUI. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(FORMED)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level.isClientSide()) {
            FrameDroneRenderer.clickedStructure(level, pos, state, Blueprint.TURBINE_SMALL, Blueprint.TURBINE_MEDIUM, Blueprint.TURBINE_LARGE);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            // The rotor's angle is animated on the client from the synced speed
            return createTickerHelper(type, ModBlockEntities.TURBINE_CONTROLLER.get(), (lvl, pos, st, be) -> be.clientTick());
        }
        return machineTicker(level, type, ModBlockEntities.TURBINE_CONTROLLER.get());
    }
}
