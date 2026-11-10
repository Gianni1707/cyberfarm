package com.cyberfarm.menu;

import java.util.List;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.ReceiverDishBlockEntity;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jspecify.annotations.Nullable;

/** The Receiver Dish's GUI: the networks it may link to, one click to link, and Disconnect. No slots, no inventory. */
public class ReceiverDishMenu extends MachineMenu {
    public static final int BUTTON_DISCONNECT = 100;

    private final @Nullable ReceiverDishBlockEntity dish;
    private final List<ReceiverDishBlockEntity.Choice> choices;
    /** Which row is linked, or -1: set by the server when the player links or disconnects. */
    private final DataSlot linked = DataSlot.standalone();

    public ReceiverDishMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, ReceiverDishBlockEntity.readChoices(buf),
                new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 2), ContainerLevelAccess.NULL);
    }

    public ReceiverDishMenu(int containerId, Inventory inventory, @Nullable ReceiverDishBlockEntity dish, List<ReceiverDishBlockEntity.Choice> choices,
            ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.RECEIVER_DISH.get(), containerId, data, access, ModBlocks.RECEIVER_DISH.get());
        this.dish = dish;
        this.choices = choices;
        int at = -1;
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).linked()) {
                at = i;
            }
        }
        linked.set(at);
        addDataSlot(linked);
    }

    public List<ReceiverDishBlockEntity.Choice> choices() {
        return choices;
    }

    public int linked() {
        return linked.get();
    }

    /** FE that arrived over the last tick. */
    public int received() {
        return com.cyberfarm.energy.EnergyUtil.combine(extra(0), extra(1));
    }

    /** Seats taken on a row's network, counting this dish in or out as the player linked since the list was made. */
    public int seats(int row) {
        ReceiverDishBlockEntity.Choice choice = choices.get(row);
        return choice.used() - (choice.linked() ? 1 : 0) + (row == linked() ? 1 : 0);
    }

    /** Button {@code i} links to row {@code i}; {@link #BUTTON_DISCONNECT} leaves the network. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (dish == null || !(player instanceof ServerPlayer server)) {
            return false;
        }
        if (id == BUTTON_DISCONNECT) {
            dish.unlink();
            linked.set(-1);
            return true;
        }
        if (id >= 0 && id < choices.size() && id != linked() && dish.link(server, choices.get(id).id())) {
            linked.set(id);
            return true;
        }
        return false;
    }
}
