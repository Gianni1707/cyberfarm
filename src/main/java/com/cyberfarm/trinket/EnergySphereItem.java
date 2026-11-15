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
 * Energy Sphere: an orb of plasma that holds the FE of every trinket. It sits in the Utility Belt and charges on the
 * Human Dock, in a Battery Bank or in any other mod's charger.
 */
public class EnergySphereItem extends Item {
    /** The tiers: FE held and FE per tick a charger may push in. */
    public static final int CAPACITY = 500_000, MAX_INPUT = 4_000;
    public static final int ADVANCED_CAPACITY = 2_000_000, ADVANCED_INPUT = 16_000;
    public static final int ELITE_CAPACITY = 8_000_000, ELITE_INPUT = 64_000;
    static final int BAR_COLOR = 0x46E6D2;

    private final int capacity;
    private final int maxInput;
    /** 1, 2 or 3: the plasma's colour on the belt. */
    private final int tier;

    public EnergySphereItem(Properties properties, int tier, int capacity, int maxInput) {
        super(properties.stacksTo(1));
        this.tier = tier;
        this.capacity = capacity;
        this.maxInput = maxInput;
    }

    public static int energy(ItemStack stack) {
        return stack.getItem() instanceof EnergySphereItem ? stack.getOrDefault(ModDataComponents.ENERGY.get(), 0) : 0;
    }

    /** What the sphere holds at most; 0 for anything else. */
    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof EnergySphereItem sphere ? sphere.capacity : 0;
    }

    public static int tier(ItemStack stack) {
        return stack.getItem() instanceof EnergySphereItem sphere ? sphere.tier : 0;
    }

    public int capacity() {
        return capacity;
    }

    public int maxInput() {
        return maxInput;
    }

    public static ItemStack withEnergy(ItemStack stack, int energy) {
        ItemStack copy = stack.copy();
        copy.set(ModDataComponents.ENERGY.get(), Math.clamp(energy, 0, capacity(stack)));
        return copy;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.cyberfarm.energy", String.format("%,d", energy(stack)), String.format("%,d", capacity))
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.cyberfarm.energy_sphere").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * energy(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }
}
