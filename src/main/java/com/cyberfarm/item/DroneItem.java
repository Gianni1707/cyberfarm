package com.cyberfarm.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.cyberfarm.block.entity.DroneDockBlockEntity;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/** An assembled drone. Right-click a Drone Dock to deploy it. */
public class DroneItem extends Item {
    private static final int BAR_COLOR = 0x2FE0FF;

    public DroneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static DroneModules getModules(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DRONE_MODULES.get(), DroneModules.EMPTY);
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
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
        if (dock.hasLiveDrone(level)) {
            message(player, Component.translatable("message.cyberfarm.drone.dock_occupied"));
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        DroneEntity drone = ModEntities.DRONE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (drone == null) {
            return InteractionResult.FAIL;
        }
        drone.setup(getModules(stack), getEnergy(stack), pos);
        drone.snapTo(pos.getX() + 0.5, pos.getY() + DroneEntity.DOCK_HEIGHT, pos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(drone);
        dock.setDrone(drone.getUUID());
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.8F);
        message(player, Component.translatable("message.cyberfarm.drone.deployed"));
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, Component text) {
        if (player != null) {
            player.sendOverlayMessage(text);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
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
