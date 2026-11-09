package com.cyberfarm.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A Transmitter Dish's range module: how far it reaches and how much FE per tick it sends, tier 0 (no module) to 4
 * (Infinity: no limit on either, and into other dimensions).
 */
public class RangeModuleItem extends Item {
    public static final int INFINITY = 4;
    private static final int[] RANGE = {32, 128, 512, 2_048};
    private static final int[] POWER = {2_000, 8_000, 32_000, 128_000};

    private final int tier;

    public RangeModuleItem(Properties properties, int tier) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    /** The tier a dish gets from what is in its module slot. */
    public static int tierOf(ItemStack stack) {
        return stack.getItem() instanceof RangeModuleItem module ? module.tier : 0;
    }

    /** Range in blocks, or -1 for no limit. */
    public static int range(int tier) {
        return tier >= INFINITY ? -1 : RANGE[tier];
    }

    /** FE per tick shared among the receivers, or -1 for no limit. */
    public static int power(int tier) {
        return tier >= INFINITY ? -1 : POWER[tier];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        if (tier >= INFINITY) {
            builder.accept(Component.translatable("tooltip.cyberfarm.range_module.infinity").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            builder.accept(Component.translatable("tooltip.cyberfarm.range_module", String.format("%,d", range(tier)), String.format("%,d", power(tier)))
                    .withStyle(ChatFormatting.AQUA));
        }
        builder.accept(Component.translatable("tooltip.cyberfarm.range_module.slot").withStyle(ChatFormatting.GRAY));
    }
}
