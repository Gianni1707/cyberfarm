package com.cyberfarm.port;

import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * The 26.x block capability registry on Forge 1.20.1: providers are registered per block entity
 * type (or per block) once, and {@link PortBlockEntity#getCapability} answers from them. Also the
 * lookups into other blocks, which go through Forge capabilities so they reach any mod.
 */
public final class Caps {
    public static final Capability<IEnergyStorage> ENERGY = ForgeCapabilities.ENERGY;
    public static final Capability<IItemHandler> ITEM = ForgeCapabilities.ITEM_HANDLER;
    public static final Capability<IFluidHandler> FLUID = ForgeCapabilities.FLUID_HANDLER;

    @FunctionalInterface
    public interface EntityProvider<B extends BlockEntity, T> {
        @Nullable T get(B be, @Nullable Direction side);
    }

    @FunctionalInterface
    public interface BlockProvider<T> {
        @Nullable T get(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity be, @Nullable Direction side);
    }

    private static final Map<BlockEntityType<?>, Map<Capability<?>, EntityProvider<?, ?>>> BY_TYPE = new IdentityHashMap<>();
    private static final Map<Block, Map<Capability<?>, BlockProvider<?>>> BY_BLOCK = new IdentityHashMap<>();

    private Caps() {}

    public static <B extends BlockEntity, T> void registerBlockEntity(Capability<T> cap, BlockEntityType<B> type,
            EntityProvider<? super B, ? extends T> provider) {
        BY_TYPE.computeIfAbsent(type, t -> new IdentityHashMap<>()).put(cap, provider);
    }

    public static <T> void registerBlock(Capability<T> cap, BlockProvider<? extends T> provider, Block... blocks) {
        for (Block block : blocks) {
            BY_BLOCK.computeIfAbsent(block, b -> new IdentityHashMap<>()).put(cap, provider);
        }
    }

    /** What CyberFarm itself exposes on {@code be} for {@code cap}, or null. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static @Nullable Object provide(BlockEntity be, Capability<?> cap, @Nullable Direction side) {
        Map<Capability<?>, EntityProvider<?, ?>> byType = BY_TYPE.get(be.getType());
        if (byType != null) {
            EntityProvider provider = byType.get(cap);
            if (provider != null) {
                return provider.get(be, side);
            }
        }
        BlockState state = be.getBlockState();
        Map<Capability<?>, BlockProvider<?>> byBlock = BY_BLOCK.get(state.getBlock());
        if (byBlock != null && be.getLevel() != null) {
            BlockProvider<?> provider = byBlock.get(cap);
            if (provider != null) {
                return provider.get(be.getLevel(), be.getBlockPos(), state, be, side);
            }
        }
        return null;
    }

    /** The {@code cap} handler of the block at {@code pos}, from any mod, or null. */
    public static <T> @Nullable T find(Capability<T> cap, Level level, BlockPos pos, @Nullable Direction side) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.getCapability(cap, side).orElse(null);
    }

    public static @Nullable IEnergyStorage energy(Level level, BlockPos pos, @Nullable Direction side) {
        return find(ENERGY, level, pos, side);
    }

    public static @Nullable IItemHandler items(Level level, BlockPos pos, @Nullable Direction side) {
        return find(ITEM, level, pos, side);
    }

    public static @Nullable IFluidHandler fluids(Level level, BlockPos pos, @Nullable Direction side) {
        return find(FLUID, level, pos, side);
    }
}
