package com.cyberfarm.block.entity;

import com.cyberfarm.block.CombustionGeneratorBlock;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.menu.CombustionGeneratorMenu;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Burns any furnace fuel into FE and pushes it into adjacent blocks. */
public class CombustionGeneratorBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 50_000;
    public static final int GENERATION_PER_TICK = 40;
    public static final int OUTPUT_PER_TICK = 400;

    private int burnTime;
    private int burnDuration;

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, state, 1, CAPACITY, 0, OUTPUT_PER_TICK);
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (burnTime > 0) {
            burnTime--;
            energy.generate(GENERATION_PER_TICK);
            setChanged();
        }
        if (burnTime <= 0 && !energy.isFull()) {
            consumeFuel(level);
        }

        EnergyUtil.pushToNeighbors(level, pos, energy, OUTPUT_PER_TICK);

        boolean burning = burnTime > 0;
        if (state.getValue(CombustionGeneratorBlock.LIT) != burning) {
            level.setBlock(pos, state.setValue(CombustionGeneratorBlock.LIT, burning), Block.UPDATE_ALL);
        }
    }

    private void consumeFuel(ServerLevel level) {
        ItemStack fuel = getStack(0);
        if (fuel.isEmpty()) {
            return;
        }
        // Half the furnace burn time: one coal = 800 ticks * 40 FE = 32,000 FE
        int duration = fuel.getBurnTime(null, level.fuelValues()) / 2;
        if (duration <= 0) {
            return;
        }
        burnTime = burnDuration = duration;
        if (fuel.is(Items.LAVA_BUCKET)) {
            setStack(0, new ItemStack(Items.BUCKET));
        } else {
            fuel.shrink(1);
            setStack(0, fuel);
        }
        setChanged();
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return level == null || resource.toStack().getBurnTime(null, level.fuelValues()) > 0;
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> burnTime;
            case 1 -> burnDuration;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 2;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burn_time", burnTime);
        output.putInt("burn_duration", burnDuration);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnTime = input.getIntOr("burn_time", 0);
        burnDuration = input.getIntOr("burn_duration", 0);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CombustionGeneratorMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
