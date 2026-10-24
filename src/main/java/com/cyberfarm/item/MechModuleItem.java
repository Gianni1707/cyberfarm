package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.entity.MechModule;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A mech module: put it in its slot of a Mech Hangar's console with the mech parked on the pad. */
public class MechModuleItem extends Item {
    private final MechModule module;

    public MechModuleItem(Properties properties, MechModule module) {
        super(properties);
        this.module = module;
    }

    public MechModule module() {
        return module;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.cyberfarm.mech_module." + module.id()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.cyberfarm.mech_module.install").withStyle(ChatFormatting.DARK_GRAY));
    }
}
