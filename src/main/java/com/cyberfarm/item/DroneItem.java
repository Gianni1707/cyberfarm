package com.cyberfarm.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.Level;

import java.util.List;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
import com.cyberfarm.block.entity.SwarmDockBlockEntity;
import com.cyberfarm.drone.DroneModules;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/** An assembled drone. Right-click a Drone Dock to deploy it. */
public class DroneItem extends Item {
    private static final int BAR_COLOR = 0x00B894;

    public DroneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static DroneModules getModules(ItemStack stack) {
        return ModDataComponents.DRONE_MODULES.getOrDefault(stack, DroneModules.EMPTY);
    }

    public static int getEnergy(ItemStack stack) {
        return ModDataComponents.ENERGY.getOrDefault(stack, 0);
    }

    /** "Mini Drone" for one built on a Mini Drone Frame. */
    @Override
    public Component getName(ItemStack stack) {
        return getModules(stack).isMini() ? Component.translatable("item.cyberfarm.mini_drone") : super.getName(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof DroneDockBlockEntity dock)) {
            message(player, Component.translatable("message.cyberfarm.drone.needs_dock"));
            return InteractionResult.FAIL;
        }
        // Mini Drones live only on a swarm dock, and a swarm dock takes only Mini Drones
        boolean mini = getModules(context.getItemInHand()).isMini();
        if (mini != dock instanceof SwarmDockBlockEntity) {
            message(player, Component.translatable(mini ? "message.cyberfarm.drone.mini_needs_swarm" : "message.cyberfarm.drone.swarm_only_mini"));
            return InteractionResult.FAIL;
        }
        if (dock.hasLiveDrone(level)) {
            message(player, Component.translatable("message.cyberfarm.drone.dock_occupied"));
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        DroneEntity drone = ModEntities.DRONE.get().create(level);
        if (drone == null) {
            return InteractionResult.FAIL;
        }
        drone.setup(getModules(stack), getEnergy(stack), pos);
        drone.moveTo(pos.getX() + 0.5, pos.getY() + DroneEntity.DOCK_HEIGHT, pos.getZ() + 0.5,
                DroneEntity.parkedYaw(level.getBlockState(pos)), 0.0F);
        drone.setYBodyRot(drone.getYRot());
        drone.setYHeadRot(drone.getYRot());
        level.addFreshEntity(drone);
        drone.beginStartup();
        dock.setDrone(drone.getUUID());
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.8F);
        message(player, Component.translatable("message.cyberfarm.drone.deployed"));
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, Component text) {
        if (player != null) {
            player.displayClientMessage(text, true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Consumer<Component> builder = tooltip::add;
        DroneModules modules = getModules(stack);
        if (modules.parts().isEmpty()) {
            builder.accept(Component.translatable("tooltip.cyberfarm.drone.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        builder.accept(Component.translatable("tooltip.cyberfarm.energy",
                String.format("%,d", getEnergy(stack)), String.format("%,d", modules.energyCapacity())).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.cyberfarm.drone.upkeep", modules.flightCost()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.cyberfarm.drone.modules").withStyle(ChatFormatting.GRAY));
        // Addons are stored once per item: show "Speed Addon x3" instead of three lines
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Item part : modules.parts()) {
            counts.merge(part, 1, Integer::sum);
        }
        counts.forEach((part, count) -> {
            Component name = Component.translatable(part.getDescriptionId());
            builder.accept(Component.literal(" - ")
                    .append(count > 1 ? Component.translatable("tooltip.cyberfarm.drone.part_count", name, count) : name)
                    .withStyle(ChatFormatting.DARK_GRAY));
        });
        builder.accept(Component.translatable("tooltip.cyberfarm.drone.deploy_hint").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getModules(stack).energyCapacity() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = getModules(stack).energyCapacity();
        return capacity <= 0 ? 0 : Math.round(13.0F * Math.min(getEnergy(stack), capacity) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }
}
