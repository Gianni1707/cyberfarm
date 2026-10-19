package com.cyberfarm.block.entity;

import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.menu.GasTurbineMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Gas Turbine: burns Biogas or Methane from its tank into FE and pushes it into adjacent machines
 * and conduits. Methane holds more than twice the energy of biogas.
 */
public class GasTurbineBlockEntity extends MachineBlockEntity {
    public static final int TANK_CAPACITY = 8_000;
    public static final int CAPACITY = 50_000;
    public static final int OUTPUT_PER_TICK = 400;
    public static final int BURN_PER_TICK = 10;
    public static final int FE_PER_MB_BIOGAS = 3;
    public static final int FE_PER_MB_METHANE = 8;

    private final GasTank tank = new GasTank(TANK_CAPACITY, GasTank::isFuelGas, this::setChanged);
    private int generating;

    public GasTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_TURBINE.get(), pos, state, 0, CAPACITY, 0, OUTPUT_PER_TICK);
    }

    public static int energyPerMb(Fluid fluid) {
        if (fluid == ModFluids.METHANE.get()) {
            return FE_PER_MB_METHANE;
        }
        return fluid == ModFluids.BIOGAS.get() ? FE_PER_MB_BIOGAS : 0;
    }

    public GasTank getTank() {
        return tank;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        int perTick = BURN_PER_TICK * energyPerMb(tank.fluid());
        // Only burn when the energy has somewhere to go, so no gas is wasted
        if (perTick > 0 && tank.amount() >= BURN_PER_TICK && energy.getCapacity() - energy.getEnergy() >= perTick) {
            tank.drain(BURN_PER_TICK);
            energy.generate(perTick);
            generating = perTick;
        } else {
            generating = 0;
        }
        EnergyUtil.pushToNeighbors(level, pos, energy, OUTPUT_PER_TICK);
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> tank.amount();
            case 1 -> tank.fluidCode();
            case 2 -> generating;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 3;
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
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new GasTurbineMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
