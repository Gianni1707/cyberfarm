package com.cyberfarm.block.entity;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.energy.MachineEnergyHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Shared base for CyberFarm machines: an FE buffer, an item inventory and synced GUI data.
 * The first {@link #ENERGY_DATA_SLOTS} data slots always carry energy and capacity.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_DATA_SLOTS = 4;
    /** The status is measured over this many ticks, and sent again at least this often regardless. */
    private static final int STATUS_INTERVAL = 20;
    private static final int STATUS_RESEND = 100;

    protected final MachineEnergyHandler energy;
    protected final ItemStacksResourceHandler items;

    /** Client side: what the server last said this machine is doing. */
    public MachineStatus status = MachineStatus.IDLE;
    /** Client side: the game time the status arrived, to carry progress on smoothly until the next one. */
    public long statusTime;
    /** Client side, for its moving parts: how fast they go (0 still, 1 full speed, eased), how far they have turned, and when. */
    public float motionSpeed;
    public float motionPhase;
    public float motionTime;
    private MachineStatus sentStatus = MachineStatus.IDLE;

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

    /** For machines that work without making or using FE, such as a digester or a reactor. */
    protected boolean isWorking() {
        return false;
    }

    /** How far the current batch is, 0 to 1, or -1 for a machine without batches. */
    protected float progress() {
        return -1.0F;
    }

    /** Measures what the machine did over the last second and tells the players near it, if it changed. */
    public final void tickStatus(ServerLevel level, BlockPos pos) {
        long time = level.getGameTime();
        if (time % STATUS_INTERVAL != 0) {
            return;
        }
        float made = energy.takeGenerated() / (float) STATUS_INTERVAL;
        float used = energy.takeUsed() / (float) STATUS_INTERVAL;
        MachineStatus now = new MachineStatus(made > 0 || used > 0 || isWorking(), made, used, energy.getEnergy(), energy.getCapacity(), progress());
        // Sent again now and then too, for players who loaded the chunk since
        if (!now.equals(sentStatus) || time % STATUS_RESEND == 0) {
            sentStatus = now;
            PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(pos), new MachineStatusPayload(pos, now));
        }
        BlockState state = getBlockState();
        if (state.getBlock() instanceof LitMachineBlock && state.getValue(LitMachineBlock.LIT) != now.working()) {
            level.setBlock(pos, state.setValue(LitMachineBlock.LIT, now.working()), Block.UPDATE_CLIENTS);
        }
    }

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

    /** Whether one more {@code item} fits in the slot. */
    protected boolean hasRoomFor(int slot, Item item) {
        ItemStack stack = getStack(slot);
        return stack.isEmpty() || (stack.is(item) && stack.getCount() < stack.getMaxStackSize());
    }

    /** Adds one {@code item} to the slot; check {@link #hasRoomFor} first. */
    protected void addOne(int slot, Item item) {
        ItemStack stack = getStack(slot);
        setStack(slot, stack.isEmpty() ? new ItemStack(item) : stack.copyWithCount(stack.getCount() + 1));
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
