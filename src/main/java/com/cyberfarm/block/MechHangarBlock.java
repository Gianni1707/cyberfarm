package com.cyberfarm.block;

import com.cyberfarm.client.renderer.FrameDroneRenderer;
import com.cyberfarm.multiblock.Blueprint;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import com.cyberfarm.block.entity.MechHangarBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/** The Mech Hangar's console: the controller at its front-left corner, with the hangar's screen. */
public class MechHangarBlock extends MachineBlock {
    public static final MapCodec<MechHangarBlock> CODEC = simpleCodec(MechHangarBlock::new);
    /** The hangar around it is complete. */
    public static final BooleanProperty FORMED = MethaneExtractorBlock.FORMED;

    public MechHangarBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<MechHangarBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    /** Until it is complete, a click sends out the drones that show what it lacks, instead of opening the GUI. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(FORMED)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level.isClientSide()) {
            FrameDroneRenderer.clickedStructure(level, pos, state, Blueprint.MECH_HANGAR);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MechHangarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.MECH_HANGAR.get());
    }
}
