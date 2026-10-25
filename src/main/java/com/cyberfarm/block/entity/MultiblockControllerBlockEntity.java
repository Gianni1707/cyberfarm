package com.cyberfarm.block.entity;

import java.util.List;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.block.MultiblockControllerBlock;
import com.cyberfarm.effect.Effects;
import com.cyberfarm.multiblock.Multiblock;
import com.cyberfarm.multiblock.PortOwner;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Controller of a multiblock in several sizes (Fission Reactor, Steam Turbine). Once a second it
 * looks for the largest complete structure, binds its ports, lights up the shell and keeps
 * FORMED / SIZE in its block state, so clients know the size too.
 */
public abstract class MultiblockControllerBlockEntity<S extends Enum<S> & Multiblock.Shape> extends MachineBlockEntity implements PortOwner {
    private static final int CHECK_INTERVAL = 20;

    private final S[] sizes;
    protected List<BlockPos> ports = List.of();
    private int checkCooldown;
    private List<BlockPos> renderPorts = List.of();
    /** Game time of the last scan, -1 before the first: a MIN_VALUE start would overflow the subtraction. */
    private long renderPortsTime = -1;

    protected MultiblockControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int slots, int capacity, int maxInput, int maxOutput, S[] sizes) {
        super(type, pos, state, slots, capacity, maxInput, maxOutput);
        this.sizes = sizes;
    }

    protected abstract boolean isShellBlock(BlockState state);

    protected abstract boolean isPort(BlockState state);

    /** Pitch of the sounds played when the structure forms or breaks. */
    protected float formSoundPitch() {
        return 1.0F;
    }

    /** The structure formed as {@code previous} just broke. Returns true when the controller must stop ticking. */
    protected boolean onStructureBroken(ServerLevel level, BlockPos pos, Direction facing, S previous) {
        return false;
    }

    /** Called after every structure check. */
    protected void onStructureChecked(ServerLevel level) {}

    /** Size of a formed controller from its block state (also on clients), or null when incomplete. */
    public static <S> @Nullable S sizeOf(BlockState state, S[] sizes) {
        return state.getValue(MultiblockControllerBlock.FORMED)
                ? sizes[Math.clamp(state.getValue(MultiblockControllerBlock.SIZE), 0, sizes.length - 1)] : null;
    }

    public @Nullable S size() {
        return sizeOf(getBlockState(), sizes);
    }

    @Override
    public boolean isFormed() {
        return getBlockState().getValue(MultiblockControllerBlock.FORMED);
    }

    private Multiblock.@Nullable Match<S> scan(Level level, BlockPos pos, Direction facing) {
        return Multiblock.scanLargest(level, pos, facing, sizes, this::isShellBlock, this::isPort);
    }

    /**
     * Re-checks the structure once a second. Returns the controller's state afterwards, or null
     * when it must stop ticking.
     */
    protected @Nullable BlockState checkStructure(ServerLevel level, BlockPos pos, BlockState state) {
        if (--checkCooldown > 0) {
            return state;
        }
        checkCooldown = CHECK_INTERVAL;
        Direction facing = state.getValue(MachineBlock.FACING);
        Multiblock.Match<S> found = scan(level, pos, facing);
        S previous = sizeOf(state, sizes);
        S size = found != null ? found.size() : null;
        // Ports first, so their pipes find the capabilities when the shell update reaches them
        ports = Multiblock.rebindPorts(level, pos, ports, found != null ? found.ports() : List.of());
        if (previous != null && previous != size) {
            Multiblock.setFormed(level, previous.cells(pos, facing), false);
        }
        if (previous != null && size == null && onStructureBroken(level, pos, facing, previous)) {
            return null;
        }
        if (size != previous) {
            state = state.setValue(MultiblockControllerBlock.FORMED, size != null).setValue(MultiblockControllerBlock.LIT, false)
                    .setValue(MultiblockControllerBlock.SIZE, size != null ? size.ordinal() : state.getValue(MultiblockControllerBlock.SIZE));
            level.setBlock(pos, state, Block.UPDATE_ALL);
            level.playSound(null, pos, (size != null ? ModSounds.REACTOR_ACTIVATE : ModSounds.REACTOR_DEACTIVATE).get(),
                    SoundSource.BLOCKS, 1.0F, formSoundPitch());
            if (size != null) {
                Effects.reveal(level, size.cells(pos, facing).stream().map(Multiblock.Cell::pos).toList());
            }
        }
        if (size != null) {
            // Re-applied while formed, for shell blocks placed back or shared with a broken neighbour
            Multiblock.setFormed(level, size.cells(pos, facing), true);
        }
        onStructureChecked(level);
        return state;
    }

    /** Client side: the ports of the formed structure, for the renderer to draw their hatches. Rescanned every second. */
    public List<BlockPos> portsForRender() {
        if (level != null && (renderPortsTime < 0 || level.getGameTime() - renderPortsTime >= 20)) {
            Multiblock.Match<S> found = scan(level, worldPosition, getBlockState().getValue(MachineBlock.FACING));
            renderPorts = found != null ? found.ports() : List.of();
            renderPortsTime = level.getGameTime();
        }
        return renderPorts;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        S size = sizeOf(state, sizes);
        if (level != null) {
            ports = Multiblock.rebindPorts(level, pos, ports, List.of());
            if (size != null) {
                Multiblock.setFormed(level, size.cells(pos, state.getValue(MachineBlock.FACING)), false);
            }
        }
    }
}
