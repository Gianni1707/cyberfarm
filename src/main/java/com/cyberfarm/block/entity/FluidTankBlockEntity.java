package com.cyberfarm.block.entity;

import com.cyberfarm.block.FluidTankBlock;
import com.cyberfarm.fluid.GasTank;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/** Contents of a Liquid Tank or Gas Tank: 32 buckets of one fluid. */
public class FluidTankBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 32_000;

    private final GasTank tank = new GasTank(CAPACITY, this::accepts, this::setChanged);

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

    /** Passive: pipes and buckets do the moving. */
    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {}

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
