package com.cyberfarm.block.entity;

import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Water Pump: next to two or more water source blocks (an endless supply, as for a bucket) it
 * pumps water with FE and pushes it into adjacent pipes and machines, e.g. a Fission Reactor port.
 */
public class WaterPumpBlockEntity extends MachineBlockEntity {
    public static final int RATE = 250;
    public static final int ENERGY_PER_TICK = 20;
    private static final int CAPACITY = 10_000;
    private static final int MAX_INPUT = 200;
    private static final int CHECK_INTERVAL = 20;

    private final GasTank tank = new GasTank(4_000, resource -> false, this::setChanged);
    private int sources;
    private int checkCooldown;

    public WaterPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_PUMP.get(), pos, state, 0, CAPACITY, MAX_INPUT, 0);
    }

    public GasTank getTank() {
        return tank;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--checkCooldown <= 0) {
            checkCooldown = CHECK_INTERVAL;
            sources = 0;
            for (Direction direction : Direction.values()) {
                FluidState fluid = level.getFluidState(pos.relative(direction));
                if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
                    sources++;
                }
            }
        }
        if (sources >= 2 && tank.capacity() - tank.amount() >= RATE && energy.consume(ENERGY_PER_TICK)) {
            tank.fill(Fluids.WATER, RATE);
        }
        tank.pushToNeighbors(level, pos, RATE * 2);
    }

    /** Shown when the pump is right-clicked: it has no GUI. */
    public Component status() {
        if (sources < 2) {
            return Component.translatable("message.cyberfarm.pump.no_water");
        }
        if (energy.getEnergy() < ENERGY_PER_TICK) {
            return Component.translatable("message.cyberfarm.pump.no_power");
        }
        return Component.translatable("message.cyberfarm.pump.working", RATE, ENERGY_PER_TICK);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return null;
    }
}
