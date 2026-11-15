package com.cyberfarm.trinket;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.cyberfarm.program." + program.getName()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.cyberfarm.program.install").withStyle(ChatFormatting.DARK_AQUA));
    }
}
