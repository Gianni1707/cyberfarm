package com.cyberfarm.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        int left = stack.getMaxDamage() - stack.getDamageValue();
        builder.accept(Component.translatable("tooltip.cyberfarm.fuel_rod.left", left / 60, left % 60).withStyle(ChatFormatting.GREEN));
    }
}
