package com.cyberfarm.trinket;

import org.jspecify.annotations.Nullable;

import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

/** The FE of an Energy Sphere or Pocket Drone for other mods' chargers (26.x ItemAccessEnergyHandler): charge only, kept in the stack. */
final class ItemEnergy implements IEnergyStorage, ICapabilityProvider {
    private final ItemStack stack;
    private final int capacity;
    private final int maxInput;
    private final LazyOptional<IEnergyStorage> self = LazyOptional.of(() -> this);

    ItemEnergy(ItemStack stack, int capacity, int maxInput) {
        this.stack = stack;
        this.capacity = capacity;
        this.maxInput = maxInput;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return ForgeCapabilities.ENERGY.orEmpty(cap, self);
    }

    @Override
    public int receiveEnergy(int max, boolean simulate) {
        int energy = getEnergyStored();
        int added = Math.max(0, Math.min(Math.min(max, maxInput), capacity - energy));
        if (added > 0 && !simulate) {
            ModDataComponents.ENERGY.set(stack, energy + added);
        }
        return added;
    }

    @Override
    public int extractEnergy(int max, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return ModDataComponents.ENERGY.getOrDefault(stack, 0);
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
