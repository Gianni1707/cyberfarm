package com.cyberfarm.block.entity;

import java.util.Objects;

import com.cyberfarm.multiblock.PortOwner;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
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

    private @Nullable PortOwner owner() {
        return controller != null && level != null && level.getBlockEntity(controller) instanceof PortOwner owner && owner.isFormed()
                ? owner : null;
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
