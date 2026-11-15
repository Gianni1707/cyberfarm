package com.cyberfarm.trinket;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.CyberFarm;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Where a player's trinkets are worn. Without Curios: slots of our own, kept in a player attachment and shown on the
 * Trinkets screen (TrinketsMenu, a tab of the inventory). With Curios: its slots cyber_neck, cyber_belt and cyber_feet
 * (see {@link CuriosTrinkets}). Everything else asks {@link #get} and {@link #set} and does not care which.
 */
public final class Trinkets {
    public static final String CURIOS = "curios";

    /** The stacks, by {@link TrinketType} ordinal. */
    public record Worn(List<ItemStack> stacks) {
        public static final Worn EMPTY = of(List.of());
        static final MapCodec<Worn> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("stacks", List.of()).forGetter(Worn::stacks)).apply(i, Worn::of));
        static final StreamCodec<RegistryFriendlyByteBuf, Worn> STREAM_CODEC =
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).map(Worn::of, Worn::stacks);

        /** Always {@link TrinketType#COUNT} stacks, whatever was saved. */
        static Worn of(List<ItemStack> saved) {
            List<ItemStack> stacks = new ArrayList<>(TrinketType.COUNT);
            for (int i = 0; i < TrinketType.COUNT; i++) {
                stacks.add(i < saved.size() ? saved.get(i) : ItemStack.EMPTY);
            }
            return new Worn(List.copyOf(stacks));
        }

        Worn with(TrinketType type, ItemStack stack) {
            List<ItemStack> stacks = new ArrayList<>(this.stacks);
            stacks.set(type.ordinal(), stack);
            return new Worn(List.copyOf(stacks));
        }
    }

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CyberFarm.MODID);
    /** Saved with the player, kept through death with keepInventory (dropped otherwise), sent to everyone who sees them. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Worn>> WORN = ATTACHMENTS.register("trinkets",
            () -> AttachmentType.builder(() -> Worn.EMPTY).serialize(Worn.CODEC).copyOnDeath().sync(Worn.STREAM_CODEC).build());
    /** The Pocket Drone's errand, seen by everyone near so they see it fly. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DroneFetch>> FETCH = ATTACHMENTS.register("drone_fetch",
            () -> AttachmentType.builder(() -> DroneFetch.NONE).serialize(DroneFetch.CODEC).sync(DroneFetch.STREAM_CODEC).build());

    private Trinkets() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Trinkets::dropOnDeath);
    }

    /** Whether the trinkets live in our own slots: not when Curios brings slots of its own. */
    public static boolean ownSlots() {
        return !ModList.get().isLoaded(CURIOS);
    }

    public static ItemStack get(Player player, TrinketType type) {
        return ownSlots() ? player.getData(WORN).stacks().get(type.ordinal()) : CuriosTrinkets.get(player, type);
    }

    /** Puts {@code stack} in the slot; also how a trinket saves a change to itself (its energy, its setting). */
    public static void set(Player player, TrinketType type, ItemStack stack) {
        if (ownSlots()) {
            player.setData(WORN, player.getData(WORN).with(type, stack));
        } else {
            CuriosTrinkets.set(player, type, stack);
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
                || level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }
        for (ItemStack stack : player.getData(WORN).stacks()) {
            if (!stack.isEmpty()) {
                event.getDrops().add(new ItemEntity(level, player.getX(), player.getEyeY() - 0.3, player.getZ(), stack));
            }
        }
        player.setData(WORN, Worn.EMPTY);
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
        public Identifier getNoItemIcon() {
            return type.emptyIcon();
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
            return player.getData(WORN).stacks().stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return player.getData(WORN).stacks().get(slot);
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
            player.setData(WORN, player.getData(WORN).with(TrinketType.values()[slot], stack));
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player user) {
            return user == player;
        }

        @Override
        public void clearContent() {
            player.setData(WORN, Worn.EMPTY);
        }
    }
}
