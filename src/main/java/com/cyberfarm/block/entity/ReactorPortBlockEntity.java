package com.cyberfarm.block.entity;

import java.util.Objects;

import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * A Reactor Port lets pipes, cables and hoppers reach the controller of its tower: Fuel Rods and
 * water go in, Depleted Fuel Rods and steam come out. The controller tells each port where it is.
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

    private @Nullable ReactorControllerBlockEntity controller() {
        return controller != null && level != null && level.getBlockEntity(controller) instanceof ReactorControllerBlockEntity entity && entity.isFormed()
                ? entity : null;
    }

    public @Nullable ResourceHandler<ItemResource> getItemHandler() {
        ReactorControllerBlockEntity entity = controller();
        return entity != null ? entity.getAutomationItems() : null;
    }

    public @Nullable ResourceHandler<FluidResource> getFluidHandler() {
        ReactorControllerBlockEntity entity = controller();
        return entity != null ? entity.getAutomationFluids() : null;
    }
}
