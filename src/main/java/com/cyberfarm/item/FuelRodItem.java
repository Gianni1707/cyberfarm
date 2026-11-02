package com.cyberfarm.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.Level;

import java.util.List;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Uranium Fuel Rod for the Fission Reactor. Its durability is the fuel left, one point per
 * second of burning; a spent rod turns into a Depleted Fuel Rod.
 */
public class FuelRodItem extends Item {
    public static final int BURN_SECONDS = 2_400;

    public FuelRodItem(Properties properties) {
        super(properties.durability(BURN_SECONDS));
    }

    public static boolean isSpent(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Consumer<Component> builder = tooltip::add;
        int left = stack.getMaxDamage() - stack.getDamageValue();
        builder.accept(Component.translatable("tooltip.cyberfarm.fuel_rod.left", left / 60, left % 60).withStyle(ChatFormatting.GREEN));
    }
}
