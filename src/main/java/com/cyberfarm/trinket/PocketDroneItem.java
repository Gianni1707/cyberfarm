package com.cyberfarm.trinket;

import java.util.function.Consumer;

import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Pocket Drone: rides folded on the Utility Belt; switched on with its key it unfolds, flies around the player and
 * pulls the drops near them in. A battery of its own, small since it uses very little, kept topped up from the belt's
 * Energy Sphere whether it flies or not.
 */
public class PocketDroneItem extends Item {
    public static final int CAPACITY = 20_000;
    /** FE per tick while it flies. */
    public static final int COST_PER_TICK = 1;
    /** FE per tick the Energy Sphere gives it until it is full. */
    public static final int CHARGE_PER_TICK = 10;

    public PocketDroneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int energy(ItemStack stack) {
        return stack.getItem() instanceof PocketDroneItem ? stack.getOrDefault(ModDataComponents.ENERGY.get(), 0) : 0;
    }

    public static ItemStack withEnergy(ItemStack stack, int energy) {
        ItemStack copy = stack.copy();
        copy.set(ModDataComponents.ENERGY.get(), Math.clamp(energy, 0, CAPACITY));
        return copy;
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DRONE_ACTIVE.get(), false);
    }

    public static ItemStack withActive(ItemStack stack, boolean active) {
        ItemStack copy = stack.copy();
        copy.set(ModDataComponents.DRONE_ACTIVE.get(), active);
        return copy;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.cyberfarm.energy", String.format("%,d", energy(stack)), String.format("%,d", CAPACITY))
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.cyberfarm.pocket_drone").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * energy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return EnergySphereItem.BAR_COLOR;
    }
}
