package com.cyberfarm.menu;

import java.util.List;

import com.cyberfarm.block.entity.MachineBlockEntity;
import com.cyberfarm.block.entity.TransmitterDishBlockEntity;
import com.cyberfarm.energy.DishNetworks;
import com.cyberfarm.item.RangeModuleItem;
import com.cyberfarm.registry.ModBlocks;
import com.cyberfarm.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/** The Transmitter Dish's GUI: its network's name, colour and visibility, the range module and the receivers it serves. */
public class TransmitterDishMenu extends MachineMenu {
    public static final int BUTTON_PUBLIC = DishNetworks.COLOURS.length;
    public static final int MODULE_X = 97, MODULE_Y = 17;

    private final List<String> receivers;
    private String name;

    public TransmitterDishMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new ItemStacksResourceHandler(1) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return resource.getItem() instanceof RangeModuleItem;
            }
        }, new SimpleContainerData(MachineBlockEntity.ENERGY_DATA_SLOTS + 6), ContainerLevelAccess.NULL, buf.readUtf(), buf.readList(b -> b.readUtf()));
    }

    public TransmitterDishMenu(int containerId, Inventory inventory, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access,
            String name, List<String> receivers) {
        super(ModMenus.TRANSMITTER_DISH.get(), containerId, data, access, ModBlocks.TRANSMITTER_DISH.get());
        this.name = name;
        this.receivers = receivers;
        addMachineSlot(items, TransmitterDishBlockEntity.MODULE, MODULE_X, MODULE_Y);
        addPlayerInventory(inventory, 142);
    }

    public String name() {
        return name;
    }

    /** Where each receiver is, as a distance or a dimension, when the GUI opened. */
    public List<String> receivers() {
        return receivers;
    }

    public int colour() {
        return extra(0);
    }

    public boolean isPublic() {
        return extra(1) != 0;
    }

    public int tier() {
        return extra(2);
    }

    /** The receiver served now, or -1. */
    public int served() {
        return extra(3) - 1;
    }

    public int receiverCount() {
        return extra(4);
    }

    public boolean transmitting() {
        return extra(5) != 0;
    }

    /** Buttons 0-7 pick a colour, {@link #BUTTON_PUBLIC} switches between private and public. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server) || id < 0 || id > BUTTON_PUBLIC) {
            return false;
        }
        return access.evaluate((level, pos) -> {
            if (!(level.getBlockEntity(pos) instanceof TransmitterDishBlockEntity dish)) {
                return false;
            }
            DishNetworks.Network network = dish.network(server);
            if (id == BUTTON_PUBLIC) {
                network.isPublic = !network.isPublic;
            } else {
                network.colour = id;
            }
            DishNetworks.get(server.level().getServer()).changed();
            return true;
        }).orElse(false);
    }

    /** Renames the network: from {@link com.cyberfarm.energy.DishRenamePayload}, on the server. */
    public void rename(ServerPlayer player, String newName) {
        String trimmed = newName.strip();
        if (trimmed.isEmpty() || trimmed.length() > DishNetworks.MAX_NAME) {
            return;
        }
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof TransmitterDishBlockEntity dish) {
                dish.network(player).name = trimmed;
                DishNetworks.get(player.level().getServer()).changed();
                name = trimmed;
            }
        });
    }
}
