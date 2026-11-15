package com.cyberfarm.trinket;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A trinket: worn in its own slot, no armor and no durability. Everything it does runs on the FE of the Energy Sphere
 * in the Utility Belt (see {@link UtilityBeltItem#energy}); without one, or with an empty one, it rests.
 */
public class TrinketItem extends Item {
    private final TrinketType type;

    public TrinketItem(Properties properties, TrinketType type) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public TrinketType type() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cyberfarm.trinket.slot", Component.translatable(type.translationKey()))
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
