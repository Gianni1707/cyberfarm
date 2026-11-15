package com.cyberfarm.trinket;

import java.util.function.Consumer;

import com.cyberfarm.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Gravity Boots: keep the player hovering at a set height over the ground (or a liquid), following the terrain like a
 * hovercraft. The height is picked with a key; Sneak lands. The server switches the player's gravity off while they
 * hover (so no kick for floating and no fall damage) and GravityBootsClient holds the height.
 */
public class GravityBootsItem extends TrinketItem {
    /** Hover heights in blocks, by the index the key cycles through; 0 = off. */
    public static final double[] HEIGHTS = {0.0, 0.5, 1.0, 2.0};
    public static final int COST_PER_TICK = 12;

    public GravityBootsItem(Properties properties) {
        super(properties, TrinketType.FEET);
    }

    public static int heightIndex(ItemStack stack) {
        return Math.min(HEIGHTS.length - 1, stack.getOrDefault(ModDataComponents.HOVER_HEIGHT.get(), 1));
    }

    public static double height(ItemStack stack) {
        return HEIGHTS[heightIndex(stack)];
    }

    public static ItemStack nextHeight(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.set(ModDataComponents.HOVER_HEIGHT.get(), (heightIndex(stack) + 1) % HEIGHTS.length);
        return copy;
    }

    public static Component heightName(ItemStack stack) {
        int index = heightIndex(stack);
        return index == 0 ? Component.translatable("tooltip.cyberfarm.gravity_boots.off")
                : Component.translatable("tooltip.cyberfarm.gravity_boots.height", HEIGHTS[index]);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(heightName(stack).copy().withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.cyberfarm.trinket.cost", COST_PER_TICK).withStyle(ChatFormatting.GRAY));
    }
}
