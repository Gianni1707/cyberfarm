package com.cyberfarm.item;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** A block item with one coloured line under its name (26.x gives it as a lore component). */
public class LoreBlockItem extends BlockItem {
    private final Component lore;

    public LoreBlockItem(Block block, Properties properties, String key, ChatFormatting color) {
        super(block, properties);
        this.lore = Component.translatable(key).withStyle(color);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(lore);
    }
}
