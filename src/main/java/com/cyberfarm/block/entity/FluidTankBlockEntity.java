package com.cyberfarm.block.entity;

import com.cyberfarm.block.FluidTankBlock;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/** Contents of a Liquid Tank or Gas Tank: 32 buckets of one fluid. */
public class FluidTankBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 32_000;
    /** Ticks between two updates of the clients' copy, which the window shows. */
    private static final int SYNC_INTERVAL = 5;

    private final GasTank tank = new GasTank(CAPACITY, this::accepts, this::setChanged);
    private Fluid syncedFluid = Fluids.EMPTY;
    private int syncedAmount;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state, 0, 0, 0, 0);
    }

    /** A Gas Tank holds what is lighter than air, a Liquid Tank the rest. */
    private boolean accepts(FluidResource resource) {
        boolean gasTank = getBlockState().getBlock() instanceof FluidTankBlock block && block.gas();
        return resource.getFluidType().isLighterThanAir() == gasTank;
    }

    public GasTank getTank() {
        return tank;
    }

    /** Passive: pipes and buckets do the moving. The tick only sends changes to the clients. */
    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % SYNC_INTERVAL == 0 && (tank.amount() != syncedAmount || tank.fluid() != syncedFluid)) {
            syncedAmount = tank.amount();
            syncedFluid = tank.fluid();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
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

    public Component status() {
        if (tank.amount() == 0) {
            return Component.translatable("message.cyberfarm.tank.empty", String.format("%,d", CAPACITY));
        }
        return Component.translatable("message.cyberfarm.tank.contents", tank.getResource(0).getFluidType().getDescription(),
                String.format("%,d", tank.amount()), String.format("%,d", CAPACITY));
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
