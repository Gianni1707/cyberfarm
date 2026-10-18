package com.cyberfarm.block.entity;

import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.energy.MachineEnergyHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;

/**
 * Shared base for CyberFarm machines: an FE buffer, an item inventory and synced GUI data.
 * The first {@link #ENERGY_DATA_SLOTS} data slots always carry energy and capacity.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_DATA_SLOTS = 4;

    protected final MachineEnergyHandler energy;
    protected final ItemStacksResourceHandler items;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> EnergyUtil.low(energy.getEnergy());
                case 1 -> EnergyUtil.high(energy.getEnergy());
                case 2 -> EnergyUtil.low(energy.getCapacity());
                case 3 -> EnergyUtil.high(energy.getCapacity());
                default -> getExtraData(id - ENERGY_DATA_SLOTS);
            };
        }

        @Override
        public void set(int id, int value) {}

        @Override
        public int getCount() {
            return ENERGY_DATA_SLOTS + getExtraDataCount();
        }
    };

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, int capacity, int maxInsert, int maxExtract) {
        super(type, pos, state);
        this.energy = new MachineEnergyHandler(capacity, maxInsert, maxExtract, this::setChanged);
        this.items = new ItemStacksResourceHandler(slots) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return isItemValid(index, resource);
            }

            @Override
            protected int getCapacity(int index, ItemResource resource) {
                return Math.min(super.getCapacity(index, resource), getSlotLimit(index, resource));
            }

            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
            }
        };
    }

    public abstract void serverTick(ServerLevel level, BlockPos pos, BlockState state);

    /** Whether automation and players may put this item into the slot. */
    protected boolean isItemValid(int index, ItemResource resource) {
        return true;
    }

    protected int getSlotLimit(int index, ItemResource resource) {
        return Integer.MAX_VALUE;
    }

    protected int getExtraData(int id) {
        return 0;
    }

    protected int getExtraDataCount() {
        return 0;
    }

    public MachineEnergyHandler getEnergy() {
        return energy;
    }

    public ItemStacksResourceHandler getItems() {
        return items;
    }

    public ItemStack getStack(int index) {
        return ItemUtil.getStack(items, index);
    }

    public void setStack(int index, ItemStack stack) {
        items.set(index, ItemResource.of(stack), stack.getCount());
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        items.serialize(output.child("items"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input.childOrEmpty("energy"));
        // Deserializing straight into the handler would adopt the saved slot count, which breaks
        // the menu when a machine gained slots in an update. Load into a copy, keep what fits.
        ItemStacksResourceHandler saved = new ItemStacksResourceHandler(items.size());
        saved.deserialize(input.childOrEmpty("items"));
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = i < saved.size() ? ItemUtil.getStack(saved, i) : ItemStack.EMPTY;
            items.set(i, ItemResource.of(stack), stack.getCount());
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            for (int i = 0; i < items.size(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), getStack(i));
            }
        }
    }
}
