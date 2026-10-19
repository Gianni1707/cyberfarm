package com.cyberfarm.block.entity;

import java.util.List;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.TurbineControllerBlock;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.fluid.TankGroup;
import com.cyberfarm.menu.TurbineMenu;
import com.cyberfarm.multiblock.Multiblock;
import com.cyberfarm.multiblock.PortOwner;
import com.cyberfarm.multiblock.TurbineSize;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModFluids;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.jspecify.annotations.Nullable;

/**
 * Steam Turbine controller: a multiblock hall in three sizes (see {@link TurbineSize}). Steam piped
 * into its ports spins the rotor up to the speed the steam can sustain; the rotor turns the
 * generator, and the FE leaves through the ports and the controller into cables and machines.
 */
public class TurbineControllerBlockEntity extends MachineBlockEntity implements PortOwner {
    public static final int TANK_CAPACITY = 32_000;
    private static final int CHECK_INTERVAL = 20;
    /** Degrees the rotor turns per tick at full speed. */
    public static final float MAX_SPIN = 36.0F;

    private final GasTank steam = new GasTank(TANK_CAPACITY, ModFluids::isSteam, this::setChanged);
    private final TankGroup fluids = TankGroup.of(new GasTank[] {steam});
    private List<BlockPos> ports = List.of();
    private int checkCooldown;
    /** Rotor speed from 0 to 1; synced to clients for the animation. */
    private float speed;
    private float syncedSpeed;
    private int generating;
    private float rotation;
    private float previousRotation;
    private List<BlockPos> renderPorts = List.of();
    private long renderPortsTime = -1;

    public TurbineControllerBlockEntity(BlockPos pos, BlockState state) {
        // Sized for the largest turbine; smaller ones simply never fill it as fast
        super(ModBlockEntities.TURBINE_CONTROLLER.get(), pos, state, 0, TurbineSize.LARGE.energyCapacity(), 0, TurbineSize.LARGE.maxOutput() * 2);
    }

    // ---------------------------------------------------------------- structure

    private static boolean isShellBlock(BlockState state) {
        return state.is(ModBlocks.TURBINE_CASING.get()) || state.is(ModBlocks.TURBINE_PORT.get());
    }

    private static boolean isPort(BlockState state) {
        return state.is(ModBlocks.TURBINE_PORT.get());
    }

    private record Found(TurbineSize size, List<BlockPos> ports) {}

    private static @Nullable Found scan(Level level, BlockPos controller, Direction facing) {
        TurbineSize[] sizes = TurbineSize.values();
        for (int i = sizes.length - 1; i >= 0; i--) {
            List<BlockPos> found = Multiblock.scan(level, controller, sizes[i].cells(controller, facing),
                    TurbineControllerBlockEntity::isShellBlock, TurbineControllerBlockEntity::isPort);
            if (found != null) {
                return new Found(sizes[i], found);
            }
        }
        return null;
    }

    public static @Nullable TurbineSize sizeOf(BlockState state) {
        return state.getValue(TurbineControllerBlock.FORMED) ? TurbineSize.byOrdinal(state.getValue(TurbineControllerBlock.SIZE)) : null;
    }

    public @Nullable TurbineSize size() {
        return sizeOf(getBlockState());
    }

    @Override
    public boolean isFormed() {
        return getBlockState().getValue(TurbineControllerBlock.FORMED);
    }

    /** Right-clicking a casing or port of a formed turbine opens its controller (a large hall's length away at most). */
    public static InteractionResult openFromShell(Level level, BlockPos clicked, Player player) {
        int reach = TurbineSize.LARGE.length();
        int half = TurbineSize.LARGE.half();
        for (BlockPos pos : BlockPos.betweenClosed(clicked.offset(-reach, -half, -reach), clicked.offset(reach, half, reach))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TurbineControllerBlock)) {
                continue;
            }
            TurbineSize size = sizeOf(state);
            BlockPos controller = pos.immutable();
            if (size != null && Multiblock.isShell(size.cells(controller, state.getValue(MachineBlock.FACING)), clicked)) {
                if (!level.isClientSide() && level.getBlockEntity(controller) instanceof TurbineControllerBlockEntity entity) {
                    player.openMenu(entity);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /** Client side: the ports of the formed turbine, for the renderer's hatches. Rescanned every second. */
    public List<BlockPos> portsForRender() {
        if (level != null && (renderPortsTime < 0 || level.getGameTime() - renderPortsTime >= 20)) {
            Found found = scan(level, worldPosition, getBlockState().getValue(MachineBlock.FACING));
            renderPorts = found != null ? found.ports() : List.of();
            renderPortsTime = level.getGameTime();
        }
        return renderPorts;
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(MachineBlock.FACING);
        if (--checkCooldown <= 0) {
            checkCooldown = CHECK_INTERVAL;
            Found found = scan(level, pos, facing);
            TurbineSize previous = sizeOf(state);
            TurbineSize size = found != null ? found.size() : null;
            ports = Multiblock.rebindPorts(level, pos, ports, found != null ? found.ports() : List.of());
            if (previous != null && previous != size) {
                Multiblock.setFormed(level, previous.cells(pos, facing), false);
            }
            if (size != previous) {
                state = state.setValue(TurbineControllerBlock.FORMED, size != null).setValue(TurbineControllerBlock.LIT, false)
                        .setValue(TurbineControllerBlock.SIZE, size != null ? size.ordinal() : state.getValue(TurbineControllerBlock.SIZE));
                level.setBlock(pos, state, Block.UPDATE_ALL);
                level.playSound(null, pos, (size != null ? ModSounds.REACTOR_ACTIVATE : ModSounds.REACTOR_DEACTIVATE).get(), SoundSource.BLOCKS, 1.0F, 1.4F);
            }
            if (size != null) {
                Multiblock.setFormed(level, size.cells(pos, facing), true);
            }
        }

        TurbineSize size = sizeOf(state);
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
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        TurbineSize size = sizeOf(state);
        if (level != null) {
            ports = Multiblock.rebindPorts(level, pos, ports, List.of());
            if (size != null) {
                Multiblock.setFormed(level, size.cells(pos, state.getValue(MachineBlock.FACING)), false);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TurbineMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
