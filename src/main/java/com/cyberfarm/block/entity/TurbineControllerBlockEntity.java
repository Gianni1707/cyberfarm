package com.cyberfarm.block.entity;

import com.cyberfarm.block.TurbineControllerBlock;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.menu.TurbineMenu;
import com.cyberfarm.multiblock.Multiblock;
import com.cyberfarm.multiblock.TurbineSize;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Steam Turbine controller: a multiblock hall in three sizes (see {@link TurbineSize}). Steam piped
 * into its ports spins the rotor up to the speed the steam can sustain; the rotor turns the
 * generator, and the FE leaves through the ports and the controller into cables and machines.
 */
public class TurbineControllerBlockEntity extends MultiblockControllerBlockEntity<TurbineSize> {
    public static final int TANK_CAPACITY = 32_000;
    /** Degrees the rotor turns per tick at full speed. */
    public static final float MAX_SPIN = 36.0F;

    private final GasTank steam = new GasTank(TANK_CAPACITY, ModFluids::isSteam, this::setChanged);
    private final TankGroup fluids = TankGroup.of(new GasTank[] {steam});
    /** Rotor speed from 0 to 1; synced to clients for the animation. */
    private float speed;
    private float syncedSpeed;
    private int generating;
    private float rotation;
    private float previousRotation;

    public TurbineControllerBlockEntity(BlockPos pos, BlockState state) {
        // Sized for the largest turbine; smaller ones simply never fill it as fast
        super(ModBlockEntities.TURBINE_CONTROLLER.get(), pos, state, 0, TurbineSize.LARGE.energyCapacity(), 0, TurbineSize.LARGE.maxOutput() * 2,
                TurbineSize.values());
    }

    // ---------------------------------------------------------------- structure

    @Override
    protected boolean isShellBlock(BlockState state) {
        return state.is(ModBlocks.TURBINE_CASING.get()) || state.is(ModBlocks.TURBINE_PORT.get());
    }

    @Override
    protected boolean isPort(BlockState state) {
        return state.is(ModBlocks.TURBINE_PORT.get());
    }

    @Override
    protected float formSoundPitch() {
        return 1.4F;
    }

    /** Right-clicking a casing or port of a formed turbine opens its controller (a large hall's length away at most). */
    public static InteractionResult openFromShell(Level level, BlockPos clicked, Player player) {
        int reach = TurbineSize.LARGE.length();
        int half = TurbineSize.LARGE.half();
        return Multiblock.openController(level, clicked, player, clicked.offset(-reach, -half, -reach), clicked.offset(reach, half, reach),
                state -> state.getBlock() instanceof TurbineControllerBlock ? sizeOf(state, TurbineSize.values()) : null);
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        state = checkStructure(level, pos, state);
        TurbineSize size = size();
        float target = 0.0F;
        if (size != null && energy.getCapacity() - energy.getEnergy() >= size.maxOutput()) {
            // Spin towards the speed the stored steam can keep up; stop when the energy has nowhere to go
            target = Math.min(1.0F, steam.amount() / (float) size.maxSteam());
        }
        float step = size != null ? 1.0F / size.spinUpTicks() : 0.02F;
        speed = Math.clamp(speed + Math.clamp(target - speed, -step, step), 0.0F, 1.0F);
        generating = 0;
        if (size != null) {
            int used = Math.min(steam.amount(), Math.round(size.maxSteam() * speed));
            if (used > 0) {
                steam.drain(used);
                generating = energy.generate(used * size.fePerMb());
            }
            EnergyUtil.pushToNeighbors(level, pos, energy, size.maxOutput());
            for (BlockPos port : ports) {
                EnergyUtil.pushToNeighbors(level, port, energy, size.maxOutput());
            }
            if (speed > 0.05F && level.getGameTime() % Math.max(10, (int) (40 - speed * 30)) == 0) {
                level.playSound(null, pos, ModSounds.STEAM_TURBINE_WORK.get(), SoundSource.BLOCKS, 0.6F + speed * 0.6F, 0.6F + speed * 0.8F);
            }
        }
        boolean running = speed > 0.02F;
        if (running != state.getValue(TurbineControllerBlock.LIT)) {
            level.setBlock(pos, state.setValue(TurbineControllerBlock.LIT, running), Block.UPDATE_CLIENTS);
        }
        if (Math.abs(speed - syncedSpeed) > 0.03F || (speed == 0.0F && syncedSpeed != 0.0F)) {
            syncedSpeed = speed;
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Client: turns the rotor by its synced speed; the renderer blends between the last two angles. */
    public void clientTick() {
        previousRotation = rotation;
        rotation += speed * MAX_SPIN;
        if (rotation >= 360.0F) {
            rotation -= 360.0F;
            previousRotation -= 360.0F;
        }
    }

    public float rotation(float partialTick) {
        return previousRotation + (rotation - previousRotation) * partialTick;
    }

    public float speed() {
        return speed;
    }

    // ---------------------------------------------------------------- ports, GUI and saving

    @Override
    public ResourceHandler<FluidResource> portFluids() {
        return fluids;
    }

    @Override
    public EnergyHandler portEnergy() {
        return energy;
    }

    public GasTank getSteamTank() {
        return steam;
    }

    @Override
    protected int getExtraData(int id) {
        TurbineSize size = size();
        return switch (id) {
            case 0 -> size != null ? size.ordinal() + 1 : 0;
            case 1 -> steam.amount();
            case 2 -> steam.fluidCode();
            case 3 -> Math.round(speed * 1000);
            case 4 -> generating;
            case 5 -> size != null ? size.maxSteam() : 0;
            case 6 -> size != null ? size.fePerMb() : 0;
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return 7;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        steam.serialize(output.child("steam"));
        output.putFloat("speed", speed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        steam.deserialize(input.childOrEmpty("steam"));
        speed = input.getFloatOr("speed", 0.0F);
    }

    /** Clients only need the rotor speed, to animate it. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("speed", speed);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TurbineMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
