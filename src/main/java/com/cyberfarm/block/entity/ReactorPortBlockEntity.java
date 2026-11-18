package com.cyberfarm.block.entity;

import java.util.Objects;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.multiblock.PortOwner;
import com.cyberfarm.multiblock.TowerSize;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * A multiblock port (Reactor Port or Turbine Port) lets pipes, cables and hoppers reach the controller
 * of its structure: whatever the controller offers ({@link PortOwner}) is exposed on every side. The
 * controller tells each port where it is.
 */
public class ReactorPortBlockEntity extends BlockEntity {
    private @Nullable BlockPos controller;

    public ReactorPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_PORT.get(), pos, state);
    }

    public void setController(@Nullable BlockPos controller) {
        if (!Objects.equals(this.controller, controller)) {
            this.controller = controller;
            if (level != null) {
                level.invalidateCapabilities(worldPosition);
            }
        }
    }

    public @Nullable BlockPos controller() {
        return controller;
    }

    public boolean isHeldBy(BlockPos controller) {
        return controller.equals(this.controller);
    }

    /** Free, already this controller's, or held by one that is gone or no longer formed. */
    public boolean isFreeFor(BlockPos controller) {
        // An owner in an unloaded chunk keeps it, and looking would load that chunk
        return this.controller == null || isHeldBy(controller) || level != null && level.isLoaded(this.controller) && owner() == null;
    }

    private @Nullable PortOwner owner() {
        return controller != null && level != null && level.getBlockEntity(controller) instanceof PortOwner owner && owner.isFormed()
                ? owner : null;
    }

    /**
     * Whether pipes, cables and hoppers reach it through {@code side}. A reactor's port opens only away from the tower, where its
     * hatch is (a floor port only from below), so a pipe running past it above or below does not plug into the tower's side.
     * A turbine's port opens on every side.
     */
    public boolean opensTo(@Nullable Direction side) {
        if (side == null || controller == null || level == null
                || !(level.getBlockEntity(controller) instanceof ReactorControllerBlockEntity reactor) || !reactor.isFormed()) {
            return true;
        }
        BlockState state = reactor.getBlockState();
        TowerSize size = MultiblockControllerBlockEntity.sizeOf(state, TowerSize.values());
        if (size == null) {
            return true;
        }
        BlockPos floor = size.floorCentre(controller, state.getValue(MachineBlock.FACING));
        Direction out = size.outward(worldPosition.getX() - floor.getX(), worldPosition.getY() - floor.getY(), worldPosition.getZ() - floor.getZ());
        return side == (out != null ? out : Direction.DOWN);
    }

    public @Nullable ResourceHandler<ItemResource> getItemHandler() {
        PortOwner owner = owner();
        return owner != null ? owner.portItems() : null;
    }

    public @Nullable ResourceHandler<FluidResource> getFluidHandler() {
        PortOwner owner = owner();
        return owner != null ? owner.portFluids() : null;
    }

    public @Nullable EnergyHandler getEnergyHandler() {
        PortOwner owner = owner();
        return owner != null ? owner.portEnergy() : null;
    }
}
