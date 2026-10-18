package com.cyberfarm.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The base of a drone. Its tier sets the battery size, the flight speed, how many function
 * modules the drone can carry, how many addons fit in it and its base power use in flight.
 */
public class DroneFrameItem extends Item {
    private final int tier;
    private final int energyCapacity;
    private final int speedPercent;
    private final int moduleSlots;
    private final int addonLimit;
    private final int flightCost;

    public DroneFrameItem(Properties properties, int tier, int energyCapacity, int speedPercent, int moduleSlots, int addonLimit, int flightCost) {
        super(properties.stacksTo(16));
        this.tier = tier;
        this.energyCapacity = energyCapacity;
        this.speedPercent = speedPercent;
        this.moduleSlots = moduleSlots;
        this.addonLimit = addonLimit;
        this.flightCost = flightCost;
    }

    public int getTier() {
        return tier;
    }

    public int getEnergyCapacity() {
        return energyCapacity;
    }

    public int getSpeedPercent() {
        return speedPercent;
    }

    public int getModuleSlots() {
        return moduleSlots;
    }

    public int getAddonLimit() {
        return addonLimit;
    }

    /** Base FE per tick while flying; modules add to it and addons raise it by a percentage. */
    public int getFlightCost() {
        return flightCost;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.cyberfarm.frame.tier", tier).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.cyberfarm.capacity", String.format("%,d", energyCapacity)).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.cyberfarm.speed", speedPercent).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.cyberfarm.frame.slots", moduleSlots).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.cyberfarm.frame.addons", addonLimit).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.cyberfarm.cost.flight_base", flightCost).withStyle(ChatFormatting.GOLD));
    }
}
