package com.cyberfarm.block.entity;

import com.cyberfarm.block.HiddenFrameBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.SmallMultiblockBlock;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.menu.SteamEngineMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Steam Engine: an induction boiler (this block) burns Biogas or Methane into steam for three cylinders and a dynamo, which unroll
 * into the row of four Cyber Frames behind it (see {@link SmallMultiblockBlock}). Gas goes in and FE comes out at the boiler. It takes
 * a few seconds to spin up and to slow down.
 */
public class SteamEngineBlockEntity extends SmallMultiblockBlockEntity {
    public static final int CYLINDERS = 3;
    /** Fuel each cylinder burns per tick. */
    public static final int FUEL_PER_CYLINDER = 15;
    public static final int BIOGAS_FE_PER_MB = 9;
    public static final int METHANE_FE_PER_MB = 15;
    public static final int TANK_CAPACITY = 16_000;
    private static final int CAPACITY = 200_000;
    private static final int OUTPUT_PER_SIDE = 8_192;
    private static final int OPEN_TICKS = 66;
    /** Speed gained and lost per tick: about 5 seconds to full speed, 2.5 to a stop. */
    private static final float SPIN_UP = 0.01F;
    private static final float SPIN_DOWN = 0.02F;
    /** Flywheel turn per tick at full speed, in radians. */
    public static final float TURN_PER_TICK = 0.35F;
    private static final int SYNC_INTERVAL = 20;

    private final GasTank tank = new GasTank(TANK_CAPACITY, GasTank::isFuelGas, this::setChanged);
    private boolean running;
    private float speed;
    private int output;
    // Client side: the flywheel's angle, turned by the client tick from the synced running state
    private float angle;
    private float previousAngle;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state, 0, CAPACITY, 0, OUTPUT_PER_SIDE);
    }

    public GasTank getTank() {
        return tank;
    }

    @Override
    public int openTicks() {
        return OPEN_TICKS;
    }

    /** Open, the row's cells show their static models, as no single model may reach that far; otherwise the renderer draws them. */
    @Override
    protected void stageChanged(ServerLevel level, BlockPos pos, BlockState state, int stage) {
        Direction back = state.getValue(MachineBlock.FACING).getOpposite();
        for (int i = 1; i <= CYLINDERS + 1; i++) {
            BlockPos at = pos.relative(back, i);
            BlockState frame = level.getBlockState(at);
            int piece = stage != SmallMultiblockBlock.OPEN ? HiddenFrameBlock.NOTHING
                    : i <= CYLINDERS ? HiddenFrameBlock.STEAM_CYLINDER : HiddenFrameBlock.STEAM_DYNAMO;
            if (frame.is(ModBlocks.HIDDEN_FRAME.get()) && frame.getValue(HiddenFrameBlock.PIECE) != piece) {
                level.setBlock(at, frame.setValue(HiddenFrameBlock.PIECE, piece), Block.UPDATE_CLIENTS);
            }
        }
    }

    public static int fePerMb(Fluid fluid) {
        return fluid == ModFluids.METHANE.get() ? METHANE_FE_PER_MB : fluid == ModFluids.BIOGAS.get() ? BIOGAS_FE_PER_MB : 0;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = advanceStage(level, pos, state);
        int burn = CYLINDERS * FUEL_PER_CYLINDER;
        boolean wasRunning = running;
        // A full buffer stops it: it never burns fuel for power nobody takes
        running = open && tank.amount() >= burn && !energy.isFull();
        output = 0;
        if (running) {
            int perMb = fePerMb(tank.fluid());
            tank.drain(burn);
            speed = Math.min(1.0F, speed + SPIN_UP);
            output = (int) (speed * burn * perMb);
            energy.generate(output);
            if (level.getGameTime() % 10 == 0) {
                level.playSound(null, pos, ModSounds.STEAM_TURBINE_WORK.get(), SoundSource.BLOCKS, 0.4F + 0.4F * speed, 0.6F + 0.6F * speed);
            }
        } else {
            speed = Math.max(0.0F, speed - SPIN_DOWN);
        }
        EnergyUtil.pushToNeighbors(level, pos, energy, OUTPUT_PER_SIDE);
        if (running != wasRunning || level.getGameTime() % SYNC_INTERVAL == 0) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Client tick: eases the speed like the server does, turns the flywheel and puffs steam from the boiler. */
    public void clientTick() {
        speed = running ? Math.min(1.0F, speed + SPIN_UP) : Math.max(0.0F, speed - SPIN_DOWN);
        previousAngle = angle;
        angle += speed * TURN_PER_TICK;
        if (level != null && speed > 0.1F && level.getRandom().nextFloat() < speed * 0.4F) {
            level.addParticle(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.15, worldPosition.getZ() + 0.5,
                    0.0, 0.04 + 0.04 * speed, 0.0);
        }
    }

    public float speed() {
        return speed;
    }

    public float angle(float partialTick) {
        return Mth.lerp(partialTick, previousAngle, angle);
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> getBlockState().getValue(SmallMultiblockBlock.STAGE) == SmallMultiblockBlock.OPEN ? CYLINDERS : 0;
            case 1 -> Math.round(speed * 100);
            case 2 -> tank.amount();
            case 3 -> tank.fluidCode();
            case 4 -> EnergyUtil.low(output);
            case 5 -> EnergyUtil.high(output);
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 6;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        tank.serialize(out.child("tank"));
        out.putBoolean("running", running);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        tank.deserialize(in.childOrEmpty("tank"));
        running = in.getBooleanOr("running", false);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SteamEngineMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
