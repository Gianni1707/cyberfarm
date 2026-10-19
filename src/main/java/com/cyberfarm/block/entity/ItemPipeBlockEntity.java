package com.cyberfarm.block.entity;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.cyberfarm.block.ItemPipeBlock;
import com.cyberfarm.energy.ConduitNetwork;
import com.cyberfarm.menu.ItemPipeMenu;
import com.cyberfarm.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Moves items across an Item Pipe network: pulls from the inventories its arms are set to extract
 * from, and takes whatever other blocks push in. Items go straight to the nearest inventory that
 * takes them, never back where they came from. Its {@link ItemFilter} decides which items may enter
 * the network through this pipe.
 */
public class ItemPipeBlockEntity extends BlockEntity implements MenuProvider {
    /** Ticks between two pulls; the pipe's rate is items per second. */
    private static final int INTERVAL = 10;

    private List<ConduitNetwork.Endpoint> endpoints = List.of();
    private int cachedVersion = -1;
    private final Map<Direction, ResourceHandler<ItemResource>> handlers = new EnumMap<>(Direction.class);
    private final ItemFilter filter = new ItemFilter(this::setChanged);
    /** Filter settings for the GUI, after the 4 energy slots every machine menu starts with. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id - MachineBlockEntity.ENERGY_DATA_SLOTS) {
                case 0 -> filter.whitelist() ? 1 : 0;
                case 1 -> filter.matchComponents() ? 1 : 0;
                case 2 -> filter.matchMod() ? 1 : 0;
                case 3 -> filter.tagIndex();
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {}

        @Override
        public int getCount() {
            return MachineBlockEntity.ENERGY_DATA_SLOTS + 4;
        }
    };

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_PIPE.get(), pos, state);
    }

    /** Items moved per pull, or per insertion from outside: the tier's rate spread over a second. */
    private int batch() {
        return getBlockState().getBlock() instanceof ItemPipeBlock pipe ? Math.max(1, pipe.rate() * INTERVAL / 20) : 0;
    }

    private List<ConduitNetwork.Endpoint> endpoints() {
        if (cachedVersion != ConduitNetwork.version()) {
            endpoints = ConduitNetwork.findEndpoints(level, worldPosition);
            cachedVersion = ConduitNetwork.version();
        }
        return endpoints;
    }

    /** The inventories of the network, nearest to {@code source} first, without {@code source} itself. */
    private List<ConduitNetwork.Endpoint> targets(BlockPos source) {
        return endpoints().stream().filter(target -> !target.pos().equals(source))
                .sorted(Comparator.comparingDouble(target -> target.pos().distSqr(source))).toList();
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if ((level.getGameTime() + pos.asLong()) % INTERVAL != 0) {
            return;
        }
        for (Direction side : Direction.values()) {
            if (!state.getValue(ItemPipeBlock.EXTRACT.get(side))) {
                continue;
            }
            BlockPos sourcePos = pos.relative(side);
            ResourceHandler<ItemResource> source = level.getCapability(Capabilities.Item.BLOCK, sourcePos, side.getOpposite());
            if (source == null) {
                continue;
            }
            int left = batch();
            for (ConduitNetwork.Endpoint target : targets(sourcePos)) {
                ResourceHandler<ItemResource> receiver = level.getCapability(Capabilities.Item.BLOCK, target.pos(), target.side());
                if (receiver != null && !(level.getBlockEntity(target.pos()) instanceof ItemPipeBlockEntity)) {
                    left -= ResourceHandlerUtil.move(source, receiver, resource -> filter.test(resource.toStack()), left, null);
                }
                if (left <= 0) {
                    break;
                }
            }
        }
    }

    public ItemFilter filter() {
        return filter;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ItemPipeMenu(containerId, inventory, this, data, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        filter.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filter.load(input);
    }

    /** What a block on {@code side} pushes into: never sends items back to that block. */
    public @Nullable ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return side == null ? null : handlers.computeIfAbsent(side, this::inputFrom);
    }

    private ResourceHandler<ItemResource> inputFrom(Direction side) {
        return new ResourceHandler<>() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public ItemResource getResource(int index) {
                return ItemResource.EMPTY;
            }

            @Override
            public long getAmountAsLong(int index) {
                return 0;
            }

            @Override
            public long getCapacityAsLong(int index, ItemResource resource) {
                return batch();
            }

            @Override
            public boolean isValid(int index, ItemResource resource) {
                return true;
            }

            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (level == null || resource.isEmpty() || !filter.test(resource.toStack())) {
                    return 0;
                }
                int left = Math.min(amount, batch());
                int moved = 0;
                for (ConduitNetwork.Endpoint target : targets(worldPosition.relative(side))) {
                    if (moved >= left) {
                        break;
                    }
                    ResourceHandler<ItemResource> receiver = level.getCapability(Capabilities.Item.BLOCK, target.pos(), target.side());
                    // Other pipes are part of this network: skip them, or items would loop
                    if (receiver != null && !(level.getBlockEntity(target.pos()) instanceof ItemPipeBlockEntity)) {
                        moved += receiver.insert(resource, left - moved, transaction);
                    }
                }
                return moved;
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return 0;
            }
        };
    }
}
