package com.cyberfarm.block.entity;

import com.cyberfarm.port.PortBlockEntity;

import com.cyberfarm.block.DockTankBlock;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import com.cyberfarm.port.ValueInput;
import com.cyberfarm.port.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Dock Tank: placed against a Drone Dock, it holds what the dock's Sponge drones suck up, one liquid
 * at a time. Pipes drain it, and may fill it too, as any tank.
 */
public class DockTankBlockEntity extends PortBlockEntity {
    public static final int CAPACITY = 64_000;

    private final GasTank tank = new GasTank(CAPACITY, resource -> true, this::changed);
    /** Client side: the level its renderer shows, sliding to the real one. */
    public float shownFill;

    public DockTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOCK_TANK.get(), pos, state);
    }

    public GasTank tank() {
        return tank;
    }

    /** What pipes see: only behind it and on top, where it has its sockets. */
    public @Nullable GasTank tank(@Nullable Direction side) {
        return side == null || side == Direction.UP || side == getBlockState().getValue(DockTankBlock.FACING).getOpposite() ? tank : null;
    }

    /** Returns how much fit: none of another liquid. */
    public int fill(Fluid fluid, int amount) {
        return tank.fill(fluid, amount);
    }

    /** Saves and tells the clients, whose renderer draws the level. */
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
    }
}
