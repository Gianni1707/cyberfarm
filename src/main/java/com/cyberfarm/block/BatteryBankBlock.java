package com.cyberfarm.block;

import com.cyberfarm.block.entity.BatteryBankBlockEntity;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Battery Bank: an FE store whose blocks merge into one bank, like the fluid tanks. */
public class BatteryBankBlock extends MachineBlock {
    public BatteryBankBlock(Properties properties) {
        super(properties);
    }

    /** A socket shows wherever a conduit plugs in. */
    @Override
    protected boolean hasPipePorts() {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return machineTicker(level, type, ModBlockEntities.BATTERY_BANK.get());
    }
}
