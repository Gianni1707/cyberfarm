package com.cyberfarm.trinket;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A program cartridge for the Neural Chip, installed at the Trinket Table. */
public class ProgramItem extends Item {
    private final ChipProgram program;

    public ProgramItem(Properties properties, ChipProgram program) {
        super(properties.stacksTo(16));
        this.program = program;
    }

    public ChipProgram program() {
        return program;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cyberfarm.program." + program.getName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cyberfarm.program.install").withStyle(ChatFormatting.DARK_AQUA));
    }
}
