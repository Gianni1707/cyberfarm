package com.cyberfarm.trinket;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.port.net.ByteBufCodecs;
import com.cyberfarm.port.net.CustomPacketPayload;
import com.cyberfarm.port.net.IPayloadContext;
import com.cyberfarm.port.net.PacketDistributor;
import com.cyberfarm.port.net.StreamCodec;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

/**
 * Where a player's trinkets are worn. Without Curios: slots of our own, kept in a player capability and shown on the
 * Trinkets screen (TrinketsMenu, a tab of the inventory). With Curios: its slots cyber_neck, cyber_belt and cyber_feet
 * (see {@link CuriosTrinkets}). Everything else asks {@link #get} and {@link #set} and does not care which.
 * <p>
 * On 1.20.1 the 26.x synced attachments are a capability plus {@link SyncPayload}: every change on the server goes to the
 * player and to everyone who sees them, and a player who comes into view is sent once.
 */
public final class Trinkets {
    public static final String CURIOS = "curios";
    public static final Capability<Worn> WORN = CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation KEY = CyberFarm.id("trinkets");

    /** A player's trinket stacks by {@link TrinketType} ordinal (own slots only), and the Pocket Drone's errand. */
    public static final class Worn implements ICapabilitySerializable<CompoundTag> {
        private final NonNullList<ItemStack> stacks = NonNullList.withSize(TrinketType.COUNT, ItemStack.EMPTY);
        private DroneFetch fetch = DroneFetch.NONE;
        private final LazyOptional<Worn> self = LazyOptional.of(() -> this);

        public ItemStack get(TrinketType type) {
            return stacks.get(type.ordinal());
        }

        public DroneFetch fetch() {
            return fetch;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return WORN.orEmpty(cap, self);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (ItemStack stack : stacks) {
                list.add(stack.save(new CompoundTag()));
            }
            tag.put("stacks", list);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            ListTag list = tag.getList("stacks", Tag.TAG_COMPOUND);
            for (int i = 0; i < TrinketType.COUNT; i++) {
                stacks.set(i, i < list.size() ? ItemStack.of(list.getCompound(i)) : ItemStack.EMPTY);
            }
        }
    }

    /** Server to client: one player's worn trinkets and their drone's errand. */
    public record SyncPayload(int entity, List<ItemStack> stacks, DroneFetch fetch) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(CyberFarm.id("trinkets"));
        private static final StreamCodec<Object, ItemStack> ITEM = StreamCodec.of(buf -> buf.readItem(), (buf, stack) -> buf.writeItem(stack));
        private static final StreamCodec<Object, List<ItemStack>> ITEMS = ByteBufCodecs.<Object, ItemStack>list().apply(ITEM);
        public static final StreamCodec<io.netty.buffer.ByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SyncPayload::entity,
                ITEMS, SyncPayload::stacks,
                DroneFetch.STREAM_CODEC, SyncPayload::fetch,
                SyncPayload::new);

        @Override
        public Type<SyncPayload> type() {
            return TYPE;
        }

        public static void handle(SyncPayload payload, IPayloadContext context) {
            Entity entity = context.player().level().getEntity(payload.entity());
            Worn worn = entity instanceof Player player ? data(player) : null;
            if (worn != null) {
                for (int i = 0; i < TrinketType.COUNT; i++) {
                    worn.stacks.set(i, i < payload.stacks().size() ? payload.stacks().get(i) : ItemStack.EMPTY);
                }
                worn.fetch = payload.fetch();
            }
        }
    }

    private Trinkets() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(Worn.class));
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, Trinkets::attach);
        MinecraftForge.EVENT_BUS.addListener(Trinkets::dropOnDeath);
        MinecraftForge.EVENT_BUS.addListener(Trinkets::copyOnDeath);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getTarget() instanceof Player seen && event.getEntity() instanceof ServerPlayer viewer) {
                PacketDistributor.sendToPlayer(viewer, payload(seen));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> sync(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> sync(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> sync(event.getEntity()));
    }

    private static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(KEY, new Worn());
        }
    }

    /** Kept through death; {@link #dropOnDeath} empties them first unless the inventory is kept too. */
    private static void copyOnDeath(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        Worn old = data(event.getOriginal());
        Worn now = data(event.getEntity());
        if (old != null && now != null) {
            for (int i = 0; i < TrinketType.COUNT; i++) {
                now.stacks.set(i, old.stacks.get(i));
            }
        }
        event.getOriginal().invalidateCaps();
    }

    private static @Nullable Worn data(Player player) {
        return player.getCapability(WORN).orElse(null);
    }

    /** Whether the trinkets live in our own slots: not when Curios brings slots of its own. */
    public static boolean ownSlots() {
        return !ModList.get().isLoaded(CURIOS);
    }

    public static ItemStack get(Player player, TrinketType type) {
        if (!ownSlots()) {
            return CuriosTrinkets.get(player, type);
        }
        Worn worn = data(player);
        return worn != null ? worn.get(type) : ItemStack.EMPTY;
    }

    /** Puts {@code stack} in the slot; also how a trinket saves a change to itself (its energy, its setting). */
    public static void set(Player player, TrinketType type, ItemStack stack) {
        if (!ownSlots()) {
            CuriosTrinkets.set(player, type, stack);
            return;
        }
        Worn worn = data(player);
        if (worn != null) {
            worn.stacks.set(type.ordinal(), stack);
            sync(player);
        }
    }

    /** The Pocket Drone's errand. */
    public static DroneFetch fetch(Player player) {
        Worn worn = data(player);
        return worn != null ? worn.fetch : DroneFetch.NONE;
    }

    static void setFetch(Player player, DroneFetch fetch) {
        Worn worn = data(player);
        if (worn != null) {
            worn.fetch = fetch;
            sync(player);
        }
    }

    private static SyncPayload payload(Player player) {
        Worn worn = data(player);
        return worn == null ? new SyncPayload(player.getId(), List.of(), DroneFetch.NONE)
                : new SyncPayload(player.getId(), List.copyOf(worn.stacks), worn.fetch);
    }

    /** Server side: the player and everyone who sees them learn what they wear. */
    private static void sync(Player player) {
        if (player instanceof ServerPlayer server) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(server, payload(server));
        }
    }

    /** The trinket worn in that slot if it is of the given item class, or null. */
    public static <T extends TrinketItem> ItemStack worn(Player player, TrinketType type, Class<T> item) {
        ItemStack stack = get(player, type);
        return item.isInstance(stack.getItem()) ? stack : ItemStack.EMPTY;
    }

    public static boolean fits(ItemStack stack, TrinketType type) {
        return stack.getItem() instanceof TrinketItem trinket && trinket.type() == type;
    }

    private static void dropOnDeath(LivingDropsEvent event) {
        if (!ownSlots() || !(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
                || level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        Worn worn = data(player);
        if (worn == null) {
            return;
        }
        for (int i = 0; i < TrinketType.COUNT; i++) {
            ItemStack stack = worn.stacks.get(i);
            if (!stack.isEmpty()) {
                event.getDrops().add(new ItemEntity(level, player.getX(), player.getEyeY() - 0.3, player.getZ(), stack));
                worn.stacks.set(i, ItemStack.EMPTY);
            }
        }
        sync(player);
    }

    // The slots of the Trinkets screen (TrinketsMenu)

    /** One slot per trinket type, at the given screen positions, top to bottom. */
    public static List<Slot> slots(Player player, int x, int y) {
        PlayerTrinkets container = new PlayerTrinkets(player);
        List<Slot> slots = new ArrayList<>();
        for (TrinketType type : TrinketType.values()) {
            slots.add(new Holder(container, type, x, y + type.ordinal() * 18));
        }
        return slots;
    }

    public static final class Holder extends Slot {
        private final TrinketType type;

        private Holder(Container container, TrinketType type, int x, int y) {
            super(container, type.ordinal(), x, y);
            this.type = type;
        }

        public TrinketType type() {
            return type;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return fits(stack, type);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, type.emptyIcon());
        }
    }

    /** A player's trinket slots seen as a container. */
    private record PlayerTrinkets(Player player) implements Container {
        @Override
        public int getContainerSize() {
            return TrinketType.COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (TrinketType type : TrinketType.values()) {
                if (!get(player, type).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return get(player, TrinketType.values()[slot]);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || count <= 0) {
                return ItemStack.EMPTY;
            }
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, 1);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            set(player, TrinketType.values()[slot], stack);
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player user) {
            return user == player;
        }

        @Override
        public void clearContent() {
            for (TrinketType type : TrinketType.values()) {
                set(player, type, ItemStack.EMPTY);
            }
        }
    }
}
