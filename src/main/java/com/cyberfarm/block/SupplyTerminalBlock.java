package com.cyberfarm.block;

import com.cyberfarm.block.entity.SupplyTerminalBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Supply Terminal: the Drone Mechanist's job site, where the village posts its supply contracts. */
public class SupplyTerminalBlock extends MachineBlock {
    public static final MapCodec<SupplyTerminalBlock> CODEC = simpleCodec(SupplyTerminalBlock::new);

    public SupplyTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SupplyTerminalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyTerminalBlockEntity(pos, state);
    }

    /** Whoever places it gets the village's goodwill for the contracts it fills. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        if (by != null && level.getBlockEntity(pos) instanceof SupplyTerminalBlockEntity terminal) {
            terminal.setOwner(by.getUUID());
        }
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.SUPPLY_TERMINAL.get());
    }
}
