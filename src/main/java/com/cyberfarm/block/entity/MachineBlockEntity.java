package com.cyberfarm.block.entity;

import com.cyberfarm.port.SlotRulesItemHandler;

import net.minecraftforge.items.IItemHandler;

import com.cyberfarm.port.PortBlockEntity;

import com.cyberfarm.block.LitMachineBlock;
import com.cyberfarm.drone.AddonType;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.energy.MachineEnergyHandler;
import com.cyberfarm.item.DroneAddonItem;

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
import com.cyberfarm.port.ValueInput;
import com.cyberfarm.port.ValueOutput;
import com.cyberfarm.port.ItemResource;
import com.cyberfarm.port.ItemStacksResourceHandler;
import com.cyberfarm.port.net.PacketDistributor;

/**
 * Shared base for CyberFarm machines: an FE buffer, an item inventory and synced GUI data.
 * The first {@link #ENERGY_DATA_SLOTS} data slots always carry energy and capacity.
 */
public abstract class MachineBlockEntity extends PortBlockEntity implements MenuProvider {
    public static final int ENERGY_DATA_SLOTS = 4;
    /** The status is measured over this many ticks, and sent again at least this often regardless. */
    private static final int STATUS_INTERVAL = 20;
    private static final int STATUS_RESEND = 100;

    /** Addon slots of a machine that takes addons (see {@link #takesAddons}). */
    public static final int ADDON_SLOTS = 3;
    /** Per addon in a machine: Speed +50% speed and +80% FE/t, Efficiency -20% FE, Battery +100% buffer. */
    private static final float SPEED_STEP = 0.5F;
    private static final float SPEED_ENERGY_STEP = 0.8F;
    private static final float EFFICIENCY_STEP = 0.2F;
    private static final int MAX_SPEED = 4;
    private static final int MAX_EFFICIENCY = 3;
    private static final int MAX_BATTERY = 4;

    protected final MachineEnergyHandler energy;
    protected final ItemStacksResourceHandler items;
    /** Kept apart from {@link #items}, so pipes and hoppers never reach the addons. */
    protected final ItemStacksResourceHandler addons;
    /** What hoppers and pipes see of {@link #items}: what a slot accepts goes in, only the output slots give anything out. */
    private final IItemHandler automationItems;
    /** Ticks into the current batch, for the machines that work in batches (see {@link #work}). */
    protected int progress;
    private final int baseCapacity;
    private final int baseMaxInsert;
    private final int baseMaxExtract;
    private int speedAddons;
    private int efficiencyAddons;
    private int batteryAddons;

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
        this.baseCapacity = capacity;
        this.baseMaxInsert = maxInsert;
        this.baseMaxExtract = maxExtract;
        this.addons = new ItemStacksResourceHandler(ADDON_SLOTS) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return takesAddons() && addonType(resource) != null;
            }

            @Override
            protected int getCapacity(int index, ItemResource resource) {
                AddonType type = addonType(resource);
                return type == null ? 0 : Math.max(0, Math.min(super.getCapacity(index, resource), maxAddons(type) - addonCount(type, index)));
            }

            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                applyAddons();
                setChanged();
            }
        };
        this.items = new ItemStacksResourceHandler(slots) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return MachineBlockEntity.this.isItemValid(index, resource);
            }

            @Override
            protected int getCapacity(int index, ItemResource resource) {
                return Math.min(super.getCapacity(index, resource), MachineBlockEntity.this.getSlotLimit(index, resource));
            }

            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
            }
        };
        this.automationItems = new SlotRulesItemHandler(items, slot -> true, this::isOutput);
    }

    public abstract void serverTick(ServerLevel level, BlockPos pos, BlockState state);

    public IItemHandler automationItems() {
        return automationItems;
    }

    /** Whether automation may take items out of the slot: any slot, unless the machine keeps its inputs in. */
    protected boolean isOutput(int index) {
        return true;
    }

    /**
     * One tick of a batch: uses FE while the machine can run, and tells when the batch is done. A machine that
     * cannot run loses what it had done; one that only lacks FE waits.
     */
    protected boolean work(boolean canRun, int baseEnergy, int baseTicks) {
        if (canRun && energy.consume(energyPerTick(baseEnergy))) {
            setChanged();
            if (++progress >= workTime(baseTicks)) {
                progress = 0;
                return true;
            }
        } else if (!canRun && progress != 0) {
            progress = 0;
            setChanged();
        }
        return false;
    }

    /** Whether this machine has addon slots: the ones that use FE to work. */
    public boolean takesAddons() {
        return false;
    }

    /** The machine addons: Speed, Efficiency and Battery; null for any other item. */
    private static AddonType addonType(ItemResource resource) {
        if (resource.getItem() instanceof DroneAddonItem addon) {
            AddonType type = addon.getAddonType();
            return type == AddonType.SPEED || type == AddonType.EFFICIENCY || type == AddonType.BATTERY ? type : null;
        }
        return null;
    }

    private static int maxAddons(AddonType type) {
        return switch (type) {
            case SPEED -> MAX_SPEED;
            case EFFICIENCY -> MAX_EFFICIENCY;
            default -> MAX_BATTERY;
        };
    }

    /** How many addons of this type sit in the addon slots, leaving out slot {@code except} (-1 for none). */
    private int addonCount(AddonType type, int except) {
        int count = 0;
        for (int i = 0; i < addons.size(); i++) {
            if (i != except && addonType(addons.getResource(i)) == type) {
                count += addons.getAmountAsInt(i);
            }
        }
        return count;
    }

    /** Recounts the addons and resizes the buffer and transfer limits to them. */
    private void applyAddons() {
        speedAddons = addonCount(AddonType.SPEED, -1);
        efficiencyAddons = addonCount(AddonType.EFFICIENCY, -1);
        batteryAddons = addonCount(AddonType.BATTERY, -1);
        energy.setCapacity(baseCapacity * (1 + batteryAddons));
        energy.setTransferLimits((int) Math.ceil(baseMaxInsert * energyFactor()), baseMaxExtract);
    }

    private float energyFactor() {
        return (1 + SPEED_ENERGY_STEP * speedAddons) * (1 - EFFICIENCY_STEP * efficiencyAddons);
    }

    /** A work time in ticks, shortened by the Speed addons. */
    public int workTime(int baseTicks) {
        return Math.max(1, Math.round(baseTicks / (1 + SPEED_STEP * speedAddons)));
    }

    /** A rate (e.g. mB per tick), raised by the Speed addons. */
    public int workRate(int baseRate) {
        return Math.round(baseRate * (1 + SPEED_STEP * speedAddons));
    }

    /** FE per tick while working: more with Speed addons, less with Efficiency addons. */
    public int energyPerTick(int baseEnergy) {
        return Math.max(1, Math.round(baseEnergy * energyFactor()));
    }

    public ItemStacksResourceHandler getAddons() {
        return addons;
    }

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
            PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), new MachineStatusPayload(pos, now));
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
        return items.getStackInSlot(index);
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

    /** Takes {@code count} items out of the slot, which must hold that many. */
    protected void take(int slot, int count) {
        ItemStack stack = getStack(slot);
        setStack(slot, stack.copyWithCount(stack.getCount() - count));
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
        addons.serialize(output.child("addons"));
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Addons first: a Battery Addon makes room for the energy saved above the base buffer
        addons.deserialize(input.childOrEmpty("addons"));
        applyAddons();
        energy.deserialize(input.childOrEmpty("energy"));
        progress = input.getIntOr("progress", 0);
        // Deserializing straight into the handler would adopt the saved slot count, which breaks
        // the menu when a machine gained slots in an update. Load into a copy, keep what fits.
        ItemStacksResourceHandler saved = new ItemStacksResourceHandler(items.size());
        saved.deserialize(input.childOrEmpty("items"));
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = i < saved.size() ? saved.getStackInSlot(i) : ItemStack.EMPTY;
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
            for (int i = 0; i < addons.size(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), addons.getStackInSlot(i).copy());
            }
        }
    }
}
