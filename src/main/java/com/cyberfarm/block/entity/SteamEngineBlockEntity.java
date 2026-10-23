package com.cyberfarm.block.entity;

import com.cyberfarm.block.CyberFrameBlock;
import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.SteamEngineBlock;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.menu.SteamEngineMenu;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Steam Engine: an induction boiler (this block) burns Biogas or Methane into steam that drives a
 * row of 1 to 4 Cylinders behind it, ending in the Dynamo that turns their flywheel into FE. Every
 * cylinder burns more fuel and adds more power. It takes a few seconds to spin up and to slow down.
 */
public class SteamEngineBlockEntity extends MachineBlockEntity {
    public static final int MAX_CYLINDERS = 4;
    /** Fuel each cylinder burns per tick. */
    public static final int FUEL_PER_CYLINDER = 15;
    public static final int BIOGAS_FE_PER_MB = 6;
    public static final int METHANE_FE_PER_MB = 10;
    public static final int TANK_CAPACITY = 16_000;
    private static final int CAPACITY = 200_000;
    private static final int OUTPUT_PER_SIDE = 8_192;
    /** Speed gained and lost per tick: about 5 seconds to full speed, 2.5 to a stop. */
    private static final float SPIN_UP = 0.01F;
    private static final float SPIN_DOWN = 0.02F;
    /** Flywheel turn per tick at full speed, in radians. */
    public static final float TURN_PER_TICK = 0.35F;
    private static final int SCAN_INTERVAL = 20;

    private final GasTank tank = new GasTank(TANK_CAPACITY, GasTank::isFuelGas, this::setChanged);
    /** Cylinders found in the row, 0 when the row is not complete (no dynamo at its end). */
    private int cylinders;
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

    private Direction back() {
        return getBlockState().getValue(MachineBlock.FACING).getOpposite();
    }

    /** The Dynamo closing the row, or null while the engine is not complete. */
    public @Nullable BlockPos dynamoPos() {
        return cylinders > 0 ? worldPosition.relative(back(), cylinders + 1) : null;
    }

    /** Counts the cylinders behind the boiler; 0 unless a dynamo closes the row. */
    private int findCylinders(Level level) {
        Direction back = back();
        int count = 0;
        while (count < MAX_CYLINDERS && level.getBlockState(worldPosition.relative(back, count + 1)).is(ModBlocks.STEAM_ENGINE_CYLINDER.get())) {
            count++;
        }
        return count > 0 && level.getBlockState(worldPosition.relative(back, count + 1)).is(ModBlocks.STEAM_ENGINE_DYNAMO.get()) ? count : 0;
    }

    /** Hides (or shows again) the cylinders and dynamo of a row of {@code count} cylinders. */
    private void setPartsFormed(Level level, int count, boolean formed) {
        if (count <= 0) {
            return;
        }
        Direction back = back();
        for (int i = 1; i <= count + 1; i++) {
            BlockPos at = worldPosition.relative(back, i);
            BlockState part = level.getBlockState(at);
            if (part.hasProperty(CyberFrameBlock.FORMED) && !(part.getBlock() instanceof SteamEngineBlock)
                    && part.getValue(CyberFrameBlock.FORMED) != formed) {
                level.setBlock(at, part.setValue(CyberFrameBlock.FORMED, formed), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            setPartsFormed(level, cylinders, false);
        }
    }

    public static int fePerMb(Fluid fluid) {
        return fluid == ModFluids.METHANE.get() ? METHANE_FE_PER_MB : fluid == ModFluids.BIOGAS.get() ? BIOGAS_FE_PER_MB : 0;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % SCAN_INTERVAL == 0) {
            int found = findCylinders(level);
            if (found != cylinders) {
                setPartsFormed(level, cylinders, false);
                cylinders = found;
                state = state.setValue(SteamEngineBlock.FORMED, cylinders > 0);
                level.setBlock(pos, state, Block.UPDATE_ALL);
            }
            setPartsFormed(level, cylinders, true);
        }
        int burn = cylinders * FUEL_PER_CYLINDER;
        boolean wasRunning = running;
        // A full buffer stops it: it never burns fuel for power nobody takes
        running = cylinders > 0 && tank.amount() >= burn && !energy.isFull();
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
        BlockPos dynamo = dynamoPos();
        if (dynamo != null) {
            EnergyUtil.pushToNeighbors(level, dynamo, energy, OUTPUT_PER_SIDE);
        }
        if (running != wasRunning || level.getGameTime() % SCAN_INTERVAL == 0) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Client tick: eases the speed like the server does, turns the flywheel and puffs steam from the boiler. */
    public void clientTick() {
        speed = running ? Math.min(1.0F, speed + SPIN_UP) : Math.max(0.0F, speed - SPIN_DOWN);
        previousAngle = angle;
        angle += speed * TURN_PER_TICK;
        if (level != null && cylinders > 0 && speed > 0.1F && level.getRandom().nextFloat() < speed * 0.4F) {
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

    public int cylinders() {
        return cylinders;
    }

    @Override
    protected int getExtraData(int id) {
        return switch (id) {
            case 0 -> cylinders;
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        tank.serialize(out.child("tank"));
        out.putInt("cylinders", cylinders);
        out.putBoolean("running", running);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        tank.deserialize(in.childOrEmpty("tank"));
        cylinders = in.getIntOr("cylinders", 0);
        running = in.getBooleanOr("running", false);
    }

    /** The engine whose row ends in the dynamo at {@code pos}: looked for along the four sides. */
    public static @Nullable SteamEngineBlockEntity ofDynamo(Level level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int distance = 2; distance <= MAX_CYLINDERS + 1; distance++) {
                BlockPos at = pos.relative(side, distance);
                if (level.isLoaded(at) && level.getBlockEntity(at) instanceof SteamEngineBlockEntity engine && pos.equals(engine.dynamoPos())) {
                    return engine;
                }
            }
        }
        return null;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SteamEngineMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
