package com.cyberfarm.port;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Base of every CyberFarm block entity on 1.20.1: keeps the 26.x save hooks (ValueInput/ValueOutput,
 * preRemoveSideEffects, getUpdateTag with registries) and exposes what {@link Caps} registered.
 */
public abstract class PortBlockEntity extends BlockEntity {
    private static final int SIDES = 7;
    private static final Capability<?>[] CAPS = {Caps.ENERGY, Caps.ITEM, Caps.FLUID};

    // One LazyOptional per capability and side, reused while the handler stays the same, so pipes
    // that cache it never allocate and are told when it goes away.
    private final Object[] handlers = new Object[CAPS.length * SIDES];
    @SuppressWarnings("unchecked")
    private final LazyOptional<Object>[] optionals = new LazyOptional[CAPS.length * SIDES];

    protected PortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected final void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveAdditional(ValueOutput.of(tag));
    }

    protected void saveAdditional(ValueOutput output) {}

    @Override
    public final void load(CompoundTag tag) {
        super.load(tag);
        loadAdditional(ValueInput.of(tag));
    }

    protected void loadAdditional(ValueInput input) {}

    /** Called by {@link PortEntityBlock} when the block is replaced by another one, before the block entity goes. */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    @Override
    public CompoundTag getUpdateTag() {
        return level != null ? getUpdateTag(level.registryAccess()) : super.getUpdateTag();
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag();
    }

    public CompoundTag saveWithoutMetadata(HolderLookup.Provider registries) {
        return saveWithoutMetadata();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        int capIndex = indexOf(cap);
        if (capIndex < 0 || remove) {
            return super.getCapability(cap, side);
        }
        int i = capIndex * SIDES + (side == null ? 6 : side.ordinal());
        Object handler = Caps.provide(this, cap, side);
        if (handler != handlers[i]) {
            if (optionals[i] != null) {
                optionals[i].invalidate();
            }
            handlers[i] = handler;
            optionals[i] = handler == null ? null : LazyOptional.of(() -> handler);
        }
        return optionals[i] == null ? super.getCapability(cap, side) : optionals[i].cast();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (int i = 0; i < optionals.length; i++) {
            if (optionals[i] != null) {
                optionals[i].invalidate();
                optionals[i] = null;
                handlers[i] = null;
            }
        }
    }

    private static int indexOf(Capability<?> cap) {
        for (int i = 0; i < CAPS.length; i++) {
            if (CAPS[i] == cap) {
                return i;
            }
        }
        return -1;
    }
}
