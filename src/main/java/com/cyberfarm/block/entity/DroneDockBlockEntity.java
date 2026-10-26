package com.cyberfarm.block.entity;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.cyberfarm.Config;
import com.cyberfarm.CyberFarm;
import com.cyberfarm.block.DroneDockBlock;
import com.cyberfarm.energy.EnergyUtil;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.menu.DroneDockMenu;
import com.cyberfarm.menu.ItemFilterMenu;
import com.cyberfarm.multiblock.BuildPlan;
import com.cyberfarm.multiblock.Structure;
import com.cyberfarm.registry.ModBlockEntities;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Home base of a drone: stores the work area, charges the drone and receives its harvest.
 * Without a surveyed area the drone works a 9x9 square centred on the dock.
 */
public class DroneDockBlockEntity extends MachineBlockEntity {
    public static final int CARGO_SLOTS = 9;
    public static final int CAPACITY = 100_000;
    public static final int MAX_INPUT = 1_000;
    public static final int CHARGE_RATE = 500;
    public static final int DEFAULT_RADIUS = 4;
    /** Ticks without news from the drone before the dock shows it as offline. */
    private static final int STATUS_TIMEOUT = 100;
    /** How many kinds of block the current layer misses the dock screen lists; slot 26 counts the others. */
    public static final int BUILD_LIST = 10;
    private static final int BUILD_LIST_DATA = 27;
    /** Then how many drones are on the dock and how many fit. */
    private static final int DRONES_DATA = BUILD_LIST_DATA + 3 * BUILD_LIST;
    /** Data slots after the energy ones; see {@link #getExtraData}. */
    public static final int EXTRA_DATA = DRONES_DATA + 2;

    private @Nullable BoundingBox workArea;
    /** Next layer the Quarry Module digs; unset means the top of the work area. */
    private @Nullable Integer quarryLayer;
    private @Nullable BlockPos transportSource;
    private @Nullable BlockPos transportDestination;
    private @Nullable BlockPos taxiDestination;
    private @Nullable UUID droneId;
    /** What the Quarry keeps (the rest is thrown away) and what the Transport carries. */
    private final ItemFilter filter = new ItemFilter(this::setChanged);
    /** Set by the Return button: the drone comes home and takes no new jobs until resumed. */
    private boolean paused;
    // What the drone last reported, shown in the GUI
    private int droneStatus;
    private int droneEnergy;
    private int droneCapacity;
    /** {@code DroneTask} ordinal + 1 of the job in progress, 0 for none. */
    private int droneTask;
    private int droneModules;
    private BlockPos dronePos = BlockPos.ZERO;
    private long lastStatusTick;
    /** The structure its Builder drones build, linked with the Multiblock Projector. */
    private @Nullable BuildPlan buildPlan;

    /**
     * Anchor Addon: the chunks a dock keeps loaded for its drones. NeoForge keeps the tickets over a
     * restart; those of a dock that is gone are dropped as the world loads.
     */
    public static final TicketController ANCHOR_TICKETS = new TicketController(CyberFarm.id("drone_anchor"), (level, tickets) -> {
        for (BlockPos owner : List.copyOf(tickets.getBlockTickets().keySet())) {
            if (!(level.getBlockState(owner).getBlock() instanceof DroneDockBlock)) {
                tickets.removeAllTickets(owner);
            }
        }
    });
    private static final int ANCHOR_INTERVAL = 10;
    /** The chunks this dock keeps loaded now, as ChunkPos longs (saved, so it can let go of them after a restart). */
    private final Set<Long> anchored = new HashSet<>();
    private int anchorCooldown;
    /** The blocks the current layer misses, for the dock screen; worked out once per tick for all the data slots that show them. */
    private List<Map.Entry<Item, Integer>> shownMissing = List.of();
    private long shownMissingTick = -1;
    /**
     * What pipes, hoppers and Transport drones see: with a build linked only the blocks it still
     * needs go in, so a Transport route from any storage brings exactly the missing materials.
     */
    private final ResourceHandler<ItemResource> externalItems = new DelegatingResourceHandler<>(() -> items) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return super.isValid(index, resource) && (resource.isEmpty() || wanted(resource.getItem()) > 0);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            int wanted = Math.min(amount, wanted(resource.getItem()));
            return wanted > 0 ? super.insert(index, resource, wanted, transaction) : 0;
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            int wanted = Math.min(amount, wanted(resource.getItem()));
            return wanted > 0 ? super.insert(resource, wanted, transaction) : 0;
        }
    };

    public DroneDockBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.DRONE_DOCK.get(), pos, state);
    }

    protected DroneDockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, CARGO_SLOTS, CAPACITY, MAX_INPUT, 0);
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (droneStatus != DroneEntity.STATUS_NONE && level.getGameTime() - lastStatusTick > STATUS_TIMEOUT) {
            droneStatus = DroneEntity.STATUS_NONE;
        }
        if (--anchorCooldown <= 0) {
            anchorCooldown = ANCHOR_INTERVAL;
            anchor(level);
        }
    }

    /**
     * Keeps loaded what the dock's anchored drones need, and lets go of the rest: nothing at all when
     * the dock is paused, the feature is off in the config or the battery cannot pay.
     */
    private void anchor(ServerLevel level) {
        Set<Long> needed = Config.ANCHOR_ENABLED.get() && !paused ? anchorChunks(level) : Set.of();
        Set<Long> wanted = needed.isEmpty() || energy.consume(needed.size() * Config.ANCHOR_ENERGY_PER_CHUNK.get() * ANCHOR_INTERVAL)
                ? needed : Set.of();
        boolean changed = anchored.removeIf(chunk -> !wanted.contains(chunk) && force(level, chunk, false));
        for (long chunk : wanted) {
            if (anchored.add(chunk)) {
                force(level, chunk, true);
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    private boolean force(ServerLevel level, long chunk, boolean add) {
        ANCHOR_TICKETS.forceChunk(level, worldPosition, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), add, false);
        return true;
    }

    /** The chunks to keep loaded, most needed first, up to the config's maximum. */
    private Set<Long> anchorChunks(ServerLevel level) {
        Set<Long> chunks = new LinkedHashSet<>();
        boolean anchoredDrone = false;
        for (UUID id : droneIds()) {
            if (level.getEntity(id) instanceof DroneEntity drone && drone.isAnchored()) {
                anchoredDrone = true;
                // The drone's chunk and the ones around it, so it never flies into an unloaded one
                ChunkPos at = drone.chunkPosition();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        chunks.add(ChunkPos.pack(at.x() + dx, at.z() + dz));
                    }
                }
            }
        }
        if (!anchoredDrone) {
            return Set.of();
        }
        Set<Long> ordered = new LinkedHashSet<>();
        ordered.add(ChunkPos.pack(worldPosition));
        ordered.addAll(chunks);
        for (BlockPos end : new BlockPos[] {transportSource, transportDestination}) {
            if (end != null) {
                ordered.add(ChunkPos.pack(end));
            }
        }
        BoundingBox area = getWorkArea();
        for (int x = area.minX() >> 4; x <= area.maxX() >> 4; x++) {
            for (int z = area.minZ() >> 4; z <= area.maxZ() >> 4; z++) {
                ordered.add(ChunkPos.pack(x, z));
            }
        }
        return ordered.stream().limit(Config.ANCHOR_MAX_CHUNKS.get()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** How many chunks the dock keeps loaded now. */
    public int anchoredChunks() {
        return anchored.size();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            anchored.removeIf(chunk -> force(server, chunk, false));
        }
    }

    public BoundingBox getWorkArea() {
        if (workArea != null) {
            return workArea;
        }
        BlockPos pos = getBlockPos();
        return new BoundingBox(
                pos.getX() - DEFAULT_RADIUS, pos.getY() - 2, pos.getZ() - DEFAULT_RADIUS,
                pos.getX() + DEFAULT_RADIUS, pos.getY() + 3, pos.getZ() + DEFAULT_RADIUS);
    }

    /** Sets the work area; {@code null} goes back to the default square around the dock. */
    public void setWorkArea(@Nullable BoundingBox area) {
        workArea = area;
        quarryLayer = null;
        setChanged();
    }

    public boolean hasCustomArea() {
        return workArea != null;
    }

    public int getQuarryLayer() {
        return quarryLayer != null ? quarryLayer : getWorkArea().maxY();
    }

    public void setQuarryLayer(int y) {
        quarryLayer = y;
        setChanged();
    }

    public @Nullable BlockPos getTransportSource() {
        return transportSource;
    }

    public @Nullable BlockPos getTransportDestination() {
        return transportDestination;
    }

    public @Nullable BlockPos getTaxiDestination() {
        return taxiDestination;
    }

    /** Where a Taxi drone carries players; null clears it. */
    public void setTaxiDestination(@Nullable BlockPos destination) {
        taxiDestination = destination;
        setChanged();
    }

    /** Sets the Transport Module route; pass nulls to clear it. */
    public void setTransportRoute(@Nullable BlockPos source, @Nullable BlockPos destination) {
        transportSource = source;
        transportDestination = destination;
        setChanged();
    }

    public ItemFilter filter() {
        return filter;
    }

    public void openFilter(Player player) {
        player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new ItemFilterMenu(id, inventory, filter,
                ContainerLevelAccess.create(level, worldPosition), getBlockState().getBlock()),
                Component.translatable("gui.cyberfarm.dock.filter_title")));
    }

    public @Nullable BuildPlan getBuildPlan() {
        return buildPlan;
    }

    /** Links the structure the Builder drones build; null unlinks it. */
    public void setBuildPlan(@Nullable BuildPlan plan) {
        buildPlan = plan;
        setChanged();
    }

    public ResourceHandler<ItemResource> getExternalItems() {
        return externalItems;
    }

    /** How many more of {@code item} the dock takes in from outside: what the build still misses, minus what is stored. */
    private int wanted(Item item) {
        if (buildPlan == null || level == null) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, buildPlan.missing(level).getOrDefault(item, 0) - stored(item));
    }

    private int stored(Item item) {
        int stored = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.getResource(i).is(item)) {
                stored += items.getAmountAsInt(i);
            }
        }
        return stored;
    }

    private List<Map.Entry<Item, Integer>> shownMissing() {
        if (buildPlan == null || level == null) {
            return List.of();
        }
        if (level.getGameTime() != shownMissingTick) {
            shownMissingTick = level.getGameTime();
            shownMissing = List.copyOf(buildPlan.missingInLayer(level).entrySet());
        }
        return shownMissing;
    }

    public boolean hasLiveDrone(ServerLevel level) {
        return droneId != null && level.getEntity(droneId) instanceof DroneEntity drone && drone.isAlive();
    }

    public void setDrone(@Nullable UUID id) {
        droneId = id;
        setChanged();
    }

    public void releaseDrone(UUID id) {
        if (id.equals(droneId)) {
            setDrone(null);
            droneStatus = DroneEntity.STATUS_NONE;
        }
    }

    /** The drones that live on this dock. */
    protected List<UUID> droneIds() {
        return droneId != null ? List.of(droneId) : List.of();
    }

    /** How many drones live on this dock, and how many fit. */
    protected int droneCount() {
        return droneId != null ? 1 : 0;
    }

    protected int maxDrones() {
        return 1;
    }

    /** What {@code drone} reports every tick, for the screen. */
    public void updateDroneStatus(UUID drone, long gameTime, int status, int energy, int capacity, int task, int modules, BlockPos pos) {
        droneStatus = status;
        droneEnergy = energy;
        droneCapacity = capacity;
        droneTask = task;
        droneModules = modules;
        dronePos = pos;
        lastStatusTick = gameTime;
    }

    public boolean isPaused() {
        return paused;
    }

    public void togglePaused() {
        paused = !paused;
        setChanged();
    }

    /** Takes up to {@code max} FE out of the dock to recharge the drone. */
    public int charge(int max) {
        return energy.drain(Math.min(max, CHARGE_RATE));
    }

    /** Stores a harvested stack in the dock. Returns whatever did not fit. */
    public ItemStack deposit(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = ResourceHandlerUtil.insertStacking(items, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    /** Takes up to {@code max} items of the first stack matching {@code filter}, e.g. seeds for a Planter. */
    public ItemStack withdraw(Predicate<ItemStack> filter, int max) {
        int slot = findSlot(filter);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = getStack(slot);
        int taken = Math.min(max, stack.getCount());
        setStack(slot, taken == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - taken));
        return stack.copyWithCount(taken);
    }

    public boolean contains(Predicate<ItemStack> filter) {
        return findSlot(filter) >= 0;
    }

    /** Index of the first stored stack matching {@code filter}, or -1. */
    private int findSlot(Predicate<ItemStack> filter) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected int getExtraData(int id) {
        if (id >= DRONES_DATA) {
            return id == DRONES_DATA ? droneCount() : maxDrones();
        }
        if (id >= BUILD_LIST_DATA) {
            // Three slots per listed block: its item id + 1 in two halves (0: none), then how many are missing,
            // negative when the dock has none of it (shortcut: blocks a drone is carrying count as none)
            int index = (id - BUILD_LIST_DATA) / 3;
            List<Map.Entry<Item, Integer>> missing = shownMissing();
            if (index >= Math.min(missing.size(), BUILD_LIST)) {
                return 0;
            }
            Map.Entry<Item, Integer> entry = missing.get(index);
            int item = BuiltInRegistries.ITEM.getId(entry.getKey()) + 1;
            return switch ((id - BUILD_LIST_DATA) % 3) {
                case 0 -> EnergyUtil.low(item);
                case 1 -> EnergyUtil.high(item);
                default -> Math.min(Short.MAX_VALUE, entry.getValue()) * (stored(entry.getKey()) > 0 ? 1 : -1);
            };
        }
        BoundingBox area = getWorkArea();
        return switch (id) {
            case 0 -> area.getXSpan();
            case 1 -> area.getZSpan();
            case 2 -> droneStatus;
            case 3 -> droneCapacity > 0 ? (int) ((long) droneEnergy * 100 / droneCapacity) : 0;
            case 4 -> transportSource != null && transportDestination != null ? 1 : 0;
            case 5 -> quarryLayer != null ? 1 : 0;
            case 6 -> getQuarryLayer();
            case 7 -> droneTask;
            case 8 -> droneModules;
            case 9 -> EnergyUtil.low(dronePos.getX());
            case 10 -> EnergyUtil.high(dronePos.getX());
            case 11 -> EnergyUtil.low(dronePos.getY());
            case 12 -> EnergyUtil.high(dronePos.getY());
            case 13 -> EnergyUtil.low(dronePos.getZ());
            case 14 -> EnergyUtil.high(dronePos.getZ());
            case 15 -> EnergyUtil.low(droneEnergy);
            case 16 -> EnergyUtil.high(droneEnergy);
            case 17 -> EnergyUtil.low(droneCapacity);
            case 18 -> EnergyUtil.high(droneCapacity);
            case 19 -> paused ? 1 : 0;
            case 20 -> taxiDestination != null ? 1 : 0;
            case 21 -> buildPlan != null ? 1 : 0;
            case 22 -> buildPlan != null && level != null ? Math.min(Short.MAX_VALUE, buildPlan.remaining(level)) : 0;
            case 23 -> buildPlan != null && level != null ? Math.min(Short.MAX_VALUE, buildPlan.obstacles(level)) : 0;
            case 24 -> buildPlan != null && level != null ? buildPlan.layerNumber(level) : 0;
            case 25 -> buildPlan != null ? buildPlan.layerCount() : 0;
            case 26 -> Math.max(0, shownMissing().size() - BUILD_LIST);
            default -> 0;
        };
    }

    @Override
    protected int getExtraDataCount() {
        return EXTRA_DATA;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("work_area", BoundingBox.CODEC, workArea);
        output.storeNullable("quarry_layer", Codec.INT, quarryLayer);
        output.storeNullable("transport_source", BlockPos.CODEC, transportSource);
        output.storeNullable("transport_destination", BlockPos.CODEC, transportDestination);
        output.storeNullable("taxi_destination", BlockPos.CODEC, taxiDestination);
        output.storeNullable("drone", UUIDUtil.CODEC, droneId);
        output.putBoolean("paused", paused);
        output.store("anchored", Codec.LONG.listOf(), List.copyOf(anchored));
        if (buildPlan != null) {
            output.store("build", Identifier.CODEC, buildPlan.structure().id());
            output.store("build_origin", BlockPos.CODEC, buildPlan.origin());
            output.store("build_facing", Direction.CODEC, buildPlan.facing());
        }
        filter.save(output.child("filter"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        workArea = input.read("work_area", BoundingBox.CODEC).orElse(null);
        quarryLayer = input.read("quarry_layer", Codec.INT).orElse(null);
        transportSource = input.read("transport_source", BlockPos.CODEC).orElse(null);
        transportDestination = input.read("transport_destination", BlockPos.CODEC).orElse(null);
        taxiDestination = input.read("taxi_destination", BlockPos.CODEC).orElse(null);
        droneId = input.read("drone", UUIDUtil.CODEC).orElse(null);
        paused = input.getBooleanOr("paused", false);
        anchored.clear();
        anchored.addAll(input.read("anchored", Codec.LONG.listOf()).orElse(List.of()));
        Structure build = input.read("build", Identifier.CODEC).flatMap(Structure::byId).orElse(null);
        BlockPos buildOrigin = input.read("build_origin", BlockPos.CODEC).orElse(null);
        buildPlan = build != null && buildOrigin != null
                ? new BuildPlan(build, buildOrigin, input.read("build_facing", Direction.CODEC).orElse(Direction.NORTH)) : null;
        filter.load(input.childOrEmpty("filter"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroneDockMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition), getBlockState().getBlock());
    }
}
