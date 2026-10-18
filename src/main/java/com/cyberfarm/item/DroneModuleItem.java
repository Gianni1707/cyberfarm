package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.drone.DroneTask;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A function module: it decides what job the drone does (harvest, quarry, transport...). */
public class DroneModuleItem extends Item {
    private final DroneTask task;

    public DroneModuleItem(Properties properties, DroneTask task) {
        super(properties.stacksTo(16));
        this.task = task;
    }

    public DroneTask getTask() {
        return task;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.cyberfarm.module").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
