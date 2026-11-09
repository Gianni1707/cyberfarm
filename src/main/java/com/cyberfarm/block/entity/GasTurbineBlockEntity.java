package com.cyberfarm.block.entity;

import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.menu.GasTurbineMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;
import net.minecraft.sounds.SoundSource;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Gas Turbine: burns Biogas or Methane (more than twice the energy) from its tank into FE and pushes
 * it into adjacent machines and conduits, once assembled with its Cyber Frames (see {@link com.cyberfarm.block.SmallMultiblockBlock}).
 * Steam goes to the multiblock {@link TurbineControllerBlockEntity}.
 */
public class GasTurbineBlockEntity extends SmallMultiblockBlockEntity {
    public static final int TANK_CAPACITY = 16_000;

    public static final int CAPACITY = 50_000;
    public static final int OUTPUT_PER_TICK = 400;
    public static final int BURN_PER_TICK = 20;
    public static final int FE_PER_MB_BIOGAS = 5;
    public static final int FE_PER_MB_METHANE = 12;
    private static final int OPEN_TICKS = 62;
    /** The gas in the sight glasses changes slowly: the clients hear of it this often. */
    private static final int SYNC_INTERVAL = 20;

    private final GasTank tank = new GasTank(TANK_CAPACITY, GasTank::isFuelGas, this::setChanged);
    private int generating;
    private int syncedAmount = -1;

    public GasTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_TURBINE.get(), pos, state, 0, CAPACITY, 0, OUTPUT_PER_TICK);
    }

    private static int energyPerMb(FluidResource fluid) {
        if (fluid.is(ModFluids.METHANE_TYPE.get())) {
            return FE_PER_MB_METHANE;
        }
        return fluid.is(ModFluids.BIOGAS_TYPE.get()) ? FE_PER_MB_BIOGAS : 0;
    }

    public GasTank getTank() {
        return tank;
    }

    @Override
    public int openTicks() {
        return OPEN_TICKS;
    }

    /** Its roar comes round this often while it burns. */
    private static final int ROAR_TICKS = 60;

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = advanceStage(level, pos, state);
        int perTick = open ? BURN_PER_TICK * energyPerMb(tank.getResource(0)) : 0;
        // Only burn when the energy has somewhere to go, so no fuel is wasted
        if (perTick > 0 && tank.amount() >= BURN_PER_TICK && energy.getCapacity() - energy.getEnergy() >= perTick) {
            tank.drain(BURN_PER_TICK);
            energy.generate(perTick);
            generating = perTick;
            if (level.getGameTime() % ROAR_TICKS == 0) {
                level.playSound(null, pos, ModSounds.GAS_TURBINE_WORK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        } else {
            generating = 0;
        }
        EnergyUtil.pushToNeighbors(level, pos, energy, OUTPUT_PER_TICK);
        if (level.getGameTime() % SYNC_INTERVAL == 0 && tank.amount() != syncedAmount) {
            syncedAmount = tank.amount();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
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
