package com.cyberfarm.block.entity;

import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModSounds;
import net.minecraft.sounds.SoundSource;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import com.cyberfarm.port.ValueInput;
import com.cyberfarm.port.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Water Pump: over a water source block (an endless supply) it pumps water with FE and pushes it
 * into adjacent pipes and machines, e.g. a Fission Reactor port.
 */
public class WaterPumpBlockEntity extends MachineBlockEntity {
    public static final int RATE = 500;
    public static final int ENERGY_PER_TICK = 40;
    private static final int CAPACITY = 10_000;
    private static final int MAX_INPUT = 200;
    private static final int CHECK_INTERVAL = 20;
    /** Stays working this long after it last pumped: a machine drawing a little at a time would otherwise flip LIT every second. */
    private static final int RUN_ON = 200;

    private final GasTank tank = new GasTank(4_000, resource -> false, this::setChanged);
    private boolean waterBelow;
    private int checkCooldown;
    private int runOn;
    /** Client side: the last moment the status said it was pumping, for the renderer's wind-down. */
    public float lastPumpTime;

    public WaterPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_PUMP.get(), pos, state, 0, CAPACITY, MAX_INPUT, 0);
    }

    public GasTank getTank() {
        return tank;
    }

    /** A stroke heard this often while it pumps. */
    private static final int STROKE_TICKS = 30;

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--checkCooldown <= 0) {
            checkCooldown = CHECK_INTERVAL;
            FluidState below = level.getFluidState(pos.below());
            waterBelow = below.is(FluidTags.WATER) && below.isSource();
        }
        if (waterBelow && tank.capacity() - tank.amount() >= RATE && energy.consume(ENERGY_PER_TICK)) {
            tank.fill(Fluids.WATER, RATE);
            runOn = RUN_ON;
            if (level.getGameTime() % STROKE_TICKS == 0) {
                level.playSound(null, pos, ModSounds.WATER_PUMP_WORK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        } else if (runOn > 0) {
            runOn--;
        }
        tank.pushToNeighbors(level, pos, RATE * 2);
    }

    @Override
    protected boolean isWorking() {
        return runOn > 0;
    }

    /** Shown when the pump is right-clicked: it has no GUI. */
    public Component status() {
        if (!waterBelow) {
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
