package com.cyberfarm.block.entity;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import com.cyberfarm.ClientConfig;
import com.cyberfarm.menu.MufflerMenu;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Muffler: silences the kinds of sound chosen in its GUI within the range set in the client config.
 * Sounds only exist on the client, so the choices are synced to it and the client keeps track of
 * the mufflers it has loaded.
 */
public class MufflerBlockEntity extends MachineBlockEntity {
    /** What a Muffler can silence; each one is a toggle in its GUI. */
    public enum Category {
        MACHINES, DRONES, MONSTERS, ANIMALS, OTHER_MACHINES;

        public int bit() {
            return 1 << ordinal();
        }

        public String translationKey() {
            return "gui.cyberfarm.muffler." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final int ALL = (1 << Category.values().length) - 1;
    private static final Set<MufflerBlockEntity> CLIENT_MUFFLERS = Collections.newSetFromMap(new WeakHashMap<>());

    private int muted = ALL;

    public MufflerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MUFFLER.get(), pos, state, 0, 0, 0, 0);
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {}

    public boolean isMuting(Category category) {
        return (muted & category.bit()) != 0;
    }

    public void toggle(Category category) {
        muted ^= category.bit();
        setChanged();
        if (level != null) {
            // Tell the clients, which are the ones that actually silence sounds
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected int getExtraData(int id) {
        return id == 0 ? muted : 0;
    }

    @Override
    protected int getExtraDataCount() {
        return 1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("muted", muted);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        muted = input.getIntOr("muted", ALL);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MufflerMenu(containerId, inventory, this, data, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            CLIENT_MUFFLERS.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_MUFFLERS.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CLIENT_MUFFLERS.remove(this);
    }

    /** Whether a loaded muffler in {@code level} covers the point and silences {@code category}. Client side only. */
    public static boolean isMuffled(Level level, double x, double y, double z, Category category) {
        int range = ClientConfig.MUFFLER_RANGE.get();
        for (MufflerBlockEntity muffler : CLIENT_MUFFLERS) {
            if (muffler.level == level && !muffler.isRemoved() && muffler.isMuting(category)
                    && muffler.worldPosition.distToCenterSqr(x, y, z) <= range * range) {
                return true;
            }
        }
        return false;
    }
}
