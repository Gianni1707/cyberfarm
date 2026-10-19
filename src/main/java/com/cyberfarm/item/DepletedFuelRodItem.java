package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.radiation.Radiation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Nuclear waste. Carried, stored in a container or left on the ground it contaminates the area
 * around it; a Waste Reprocessor turns it into harmless Vitrified Waste.
 */
public class DepletedFuelRodItem extends Item {
    public DepletedFuelRodItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    /** Lying on the ground: contaminates its chunk like the waste found in inventories (every 5 s). */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 100 == 0) {
            Radiation.add(level, entity.blockPosition(), Radiation.PER_ROD * stack.getCount());
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.cyberfarm.depleted_fuel_rod").withStyle(ChatFormatting.RED));
        builder.accept(Component.translatable("tooltip.cyberfarm.depleted_fuel_rod.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
