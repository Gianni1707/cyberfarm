package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.drone.AddonType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** An addon chip: improves a drone (speed, carried stacks, fortune, silk touch). */
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.cyberfarm.addon").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (addonType.getFlightCostPercent() != 0) {
            builder.accept(Component.translatable("tooltip.cyberfarm.cost.flight_percent", addonType.getFlightCostPercent()).withStyle(ChatFormatting.GOLD));
        }
        if (addonType.getJobCostPercent() != 0) {
            builder.accept(Component.translatable("tooltip.cyberfarm.cost.job_percent", addonType.getJobCostPercent()).withStyle(ChatFormatting.GOLD));
        }
        if (addonType.getMaxPerDrone() != Integer.MAX_VALUE) {
            builder.accept(Component.translatable("tooltip.cyberfarm.addon.max", addonType.getMaxPerDrone()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
