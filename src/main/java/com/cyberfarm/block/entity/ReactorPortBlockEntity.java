package com.cyberfarm.block.entity;

import com.cyberfarm.port.Caps;

import net.minecraftforge.items.IItemHandler;

import net.minecraftforge.fluids.capability.IFluidHandler;

import com.cyberfarm.port.PortBlockEntity;

import java.util.Objects;

import com.cyberfarm.multiblock.PortOwner;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.energy.IEnergyStorage;
import com.cyberfarm.port.FluidResource;
import com.cyberfarm.port.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * A multiblock port (Reactor Port or Turbine Port) lets pipes, cables and hoppers reach the controller
 * of its structure: whatever the controller offers ({@link PortOwner}) is exposed on every side. The
 * controller tells each port where it is.
 */
public class ReactorPortBlockEntity extends PortBlockEntity {
    private @Nullable BlockPos controller;

    public ReactorPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_PORT.get(), pos, state);
    }

    public void setController(@Nullable BlockPos controller) {
        if (!Objects.equals(this.controller, controller)) {
            this.controller = controller;
            if (level != null) {
                Caps.invalidate(level, worldPosition);
            }
        }
    }

    private @Nullable PortOwner owner() {
        return controller != null && level != null && level.getBlockEntity(controller) instanceof PortOwner owner && owner.isFormed()
                ? owner : null;
    }

    public @Nullable IItemHandler getItemHandler() {
        PortOwner owner = owner();
        return owner != null ? owner.portItems() : null;
    }

    public @Nullable IFluidHandler getFluidHandler() {
        PortOwner owner = owner();
        return owner != null ? owner.portFluids() : null;
    }

    public @Nullable IEnergyStorage getEnergyHandler() {
        PortOwner owner = owner();
        return owner != null ? owner.portEnergy() : null;
    }
}
