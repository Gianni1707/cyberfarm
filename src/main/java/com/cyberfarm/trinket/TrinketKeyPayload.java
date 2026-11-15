package com.cyberfarm.trinket;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.menu.TrinketsMenu;
import com.cyberfarm.menu.UtilityBeltMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A trinket key or button the player pressed; what it changes is saved on the worn trinket, server side. */
public record TrinketKeyPayload(Action action) implements CustomPacketPayload {
    public enum Action { HOVER_HEIGHT, NIGHT_VISION, TAXI_CALL, OPEN_TRINKETS, OPEN_BELT, DRONE, PROJECTOR, SURVEY_TOOL }

    public static final Type<TrinketKeyPayload> TYPE = new Type<>(CyberFarm.id("trinket_key"));
    public static final StreamCodec<ByteBuf, TrinketKeyPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(i -> new TrinketKeyPayload(Action.values()[Math.floorMod(i, Action.values().length)]), payload -> payload.action().ordinal());

    @Override
    public Type<TrinketKeyPayload> type() {
        return TYPE;
    }

    public static void handle(TrinketKeyPayload payload, IPayloadContext context) {
        Player player = context.player();
        switch (payload.action()) {
            case HOVER_HEIGHT -> {
                ItemStack boots = Trinkets.worn(player, TrinketType.FEET, GravityBootsItem.class);
                if (!boots.isEmpty()) {
                    ItemStack next = GravityBootsItem.nextHeight(boots);
                    Trinkets.set(player, TrinketType.FEET, next);
                    player.sendOverlayMessage(GravityBootsItem.heightName(next));
                }
            }
            case NIGHT_VISION -> {
                ItemStack chip = NeuralChipItem.worn(player);
                if (NeuralChipItem.has(chip, ChipProgram.NIGHT_VISION)) {
                    ItemStack next = NeuralChipItem.toggle(chip, ChipProgram.NIGHT_VISION);
                    Trinkets.set(player, TrinketType.NECK, next);
                    player.sendOverlayMessage(Component.translatable(NeuralChipItem.isOn(next, ChipProgram.NIGHT_VISION)
                            ? "message.cyberfarm.night_vision.on" : "message.cyberfarm.night_vision.off"));
                }
            }
            case TAXI_CALL -> TaxiCall.call(player);
            case OPEN_TRINKETS -> {
                if (Trinkets.ownSlots()) {
                    player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new TrinketsMenu(id, inventory),
                            Component.translatable("container.cyberfarm.trinkets")));
                }
            }
            case OPEN_BELT -> {
                ItemStack belt = UtilityBeltItem.worn(player);
                if (!belt.isEmpty()) {
                    player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new UtilityBeltMenu(id, inventory), belt.getHoverName()));
                }
            }
            case DRONE -> toggleDrone(player);
            case PROJECTOR -> swapTool(player, UtilityBeltItem.PROJECTOR);
            case SURVEY_TOOL -> swapTool(player, UtilityBeltItem.SURVEY_TOOL);
        }
    }

    private static void toggleDrone(Player player) {
        ItemStack drone = UtilityBeltItem.content(player, UtilityBeltItem.DRONE);
        if (drone.isEmpty()) {
            return;
        }
        boolean active = !PocketDroneItem.isActive(drone);
        if (active && PocketDroneItem.energy(drone) <= 0) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.pocket_drone.empty"));
            return;
        }
        UtilityBeltItem.update(player, UtilityBeltItem.DRONE, stack -> PocketDroneItem.withActive(stack, active));
        player.sendOverlayMessage(Component.translatable(active ? "message.cyberfarm.pocket_drone.on" : "message.cyberfarm.pocket_drone.off"));
    }

    /**
     * A tool from the belt to the main hand, or back: the held item moves to a free inventory slot to make room, and
     * the tool goes back to the belt when it is the one in hand.
     */
    static void swapTool(Player player, int holder) {
        ItemStack held = player.getMainHandItem();
        ItemStack inBelt = UtilityBeltItem.content(player, holder);
        if (inBelt.isEmpty()) {
            if (UtilityBeltItem.fits(holder, held)) {
                UtilityBeltItem.update(player, holder, old -> held.copy());
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            return;
        }
        if (!held.isEmpty() && !roomBesideHand(player.getInventory().getNonEquipmentItems(), player.getInventory().getSelectedSlot(), held)) {
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.utility_belt.full"));
            return;
        }
        // The tool takes the hand first: emptied, the hand's own slot would be where the held item went, and the tool
        // would then overwrite it
        ItemStack moving = held.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, inBelt);
        UtilityBeltItem.update(player, holder, old -> ItemStack.EMPTY);
        if (!moving.isEmpty()) {
            player.getInventory().add(moving);
        }
    }

    /** Whether some slot of the main inventory ({@code slots}) other than the selected one can take all of {@code stack}. */
    static boolean roomBesideHand(List<ItemStack> slots, int selected, ItemStack stack) {
        for (int slot = 0; slot < slots.size(); slot++) {
            ItemStack there = slots.get(slot);
            if (slot != selected && (there.isEmpty()
                    || ItemStack.isSameItemSameComponents(there, stack) && there.getCount() + stack.getCount() <= there.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }
}
