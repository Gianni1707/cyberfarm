package com.cyberfarm.block.entity;

import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.menu.GasTurbineMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Turbines: burn a fluid from their tank into FE and push it into adjacent machines and conduits.
 * The Gas Turbine burns Biogas or Methane (more than twice the energy); the Steam Turbine expands
 * the Steam of a Fission Reactor, or of any other mod, much faster.
 */
public class GasTurbineBlockEntity extends MachineBlockEntity {
    public static final int TANK_CAPACITY = 16_000;

    public static final int CAPACITY = 50_000;
    public static final int OUTPUT_PER_TICK = 400;
    public static final int BURN_PER_TICK = 10;
    public static final int FE_PER_MB_BIOGAS = 3;
    public static final int FE_PER_MB_METHANE = 8;

    public static final int STEAM_CAPACITY = 200_000;
    public static final int STEAM_OUTPUT_PER_TICK = 1_600;
    public static final int STEAM_PER_TICK = 200;
    public static final int FE_PER_MB_STEAM = 4;

    private final boolean steam;
    private final int burnPerTick;
    private final int outputPerTick;
    private final GasTank tank;
    private int generating;

    public GasTurbineBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.GAS_TURBINE.get(), pos, state, false);
    }

    public static GasTurbineBlockEntity steam(BlockPos pos, BlockState state) {
        return new GasTurbineBlockEntity(ModBlockEntities.STEAM_TURBINE.get(), pos, state, true);
    }

    private GasTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean steam) {
        super(type, pos, state, 0, steam ? STEAM_CAPACITY : CAPACITY, 0, steam ? STEAM_OUTPUT_PER_TICK : OUTPUT_PER_TICK);
        this.steam = steam;
        this.burnPerTick = steam ? STEAM_PER_TICK : BURN_PER_TICK;
        this.outputPerTick = steam ? STEAM_OUTPUT_PER_TICK : OUTPUT_PER_TICK;
        this.tank = new GasTank(TANK_CAPACITY, steam ? ModFluids::isSteam : GasTank::isFuelGas, this::setChanged);
    }

    private int energyPerMb(FluidResource fluid) {
        if (steam) {
            return ModFluids.isSteam(fluid) ? FE_PER_MB_STEAM : 0;
        }
        if (fluid.is(ModFluids.METHANE_TYPE.get())) {
            return FE_PER_MB_METHANE;
        }
        return fluid.is(ModFluids.BIOGAS_TYPE.get()) ? FE_PER_MB_BIOGAS : 0;
    }

    public GasTank getTank() {
        return tank;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        int perTick = burnPerTick * energyPerMb(tank.getResource(0));
        // Only burn when the energy has somewhere to go, so no fuel is wasted
        if (perTick > 0 && tank.amount() >= burnPerTick && energy.getCapacity() - energy.getEnergy() >= perTick) {
            tank.drain(burnPerTick);
            energy.generate(perTick);
            generating = perTick;
            if (steam && level.getGameTime() % 40 == 0) {
                level.playSound(null, pos, ModSounds.STEAM_TURBINE_WORK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        } else {
            generating = 0;
        }
        EnergyUtil.pushToNeighbors(level, pos, energy, outputPerTick);
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
        ContainerLevelAccess access = ContainerLevelAccess.create(level, worldPosition);
        return steam ? GasTurbineMenu.steam(containerId, inventory, data, access) : new GasTurbineMenu(containerId, inventory, data, access);
    }
}
