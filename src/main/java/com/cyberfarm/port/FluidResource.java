package com.cyberfarm.port;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/** The 26.x FluidResource: a fluid with its NBT and no amount. */
public final class FluidResource {
    public static final FluidResource EMPTY = new FluidResource(Fluids.EMPTY, null);

    private final Fluid fluid;
    private final @Nullable CompoundTag tag;

    private FluidResource(Fluid fluid, @Nullable CompoundTag tag) {
        this.fluid = fluid;
        this.tag = tag;
    }

    public static FluidResource of(FluidStack stack) {
        return stack.isEmpty() ? EMPTY : new FluidResource(stack.getFluid(), stack.getTag() == null ? null : stack.getTag().copy());
    }

    public static FluidResource of(Fluid fluid) {
        return fluid == Fluids.EMPTY ? EMPTY : new FluidResource(fluid, null);
    }

    public boolean isEmpty() {
        return fluid == Fluids.EMPTY;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public boolean is(Fluid other) {
        return fluid == other;
    }

    public boolean is(TagKey<Fluid> fluidTag) {
        return fluid.is(fluidTag);
    }

    public FluidStack toStack(int amount) {
        return isEmpty() || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount, tag == null ? null : tag.copy());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FluidResource other && other.fluid == fluid && Objects.equals(other.tag, tag);
    }

    @Override
    public int hashCode() {
        return 31 * fluid.hashCode() + Objects.hashCode(tag);
    }
}
