package com.cyberfarm.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.Level;

import java.util.List;

import java.util.function.Consumer;

import com.cyberfarm.drone.AddonType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** An addon chip: improves a drone (speed, carried stacks, fortune, silk touch); Speed, Efficiency and Battery also go into machines. */
public class DroneAddonItem extends Item {
    private final AddonType addonType;

    public DroneAddonItem(Properties properties, AddonType addonType) {
        super(properties.stacksTo(16));
        this.addonType = addonType;
    }

    public AddonType getAddonType() {
        return addonType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Consumer<Component> builder = tooltip::add;
        builder.accept(Component.translatable("tooltip.cyberfarm.addon").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (addonType.getFlightCostPercent() != 0) {
            builder.accept(Component.translatable("tooltip.cyberfarm.cost.flight_percent", String.format("%+d", addonType.getFlightCostPercent())).withStyle(ChatFormatting.GOLD));
        }
        if (addonType.getJobCostPercent() != 0) {
            builder.accept(Component.translatable("tooltip.cyberfarm.cost.job_percent", String.format("%+d", addonType.getJobCostPercent())).withStyle(ChatFormatting.GOLD));
        }
        if (addonType == AddonType.SPEED || addonType == AddonType.EFFICIENCY || addonType == AddonType.BATTERY) {
            builder.accept(Component.translatable(getDescriptionId() + ".machine").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (addonType.getMaxPerDrone() != Integer.MAX_VALUE) {
            builder.accept(Component.translatable("tooltip.cyberfarm.addon.max", addonType.getMaxPerDrone()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
