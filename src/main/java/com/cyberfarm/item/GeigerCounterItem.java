package com.cyberfarm.item;

import java.util.function.Consumer;

import com.cyberfarm.radiation.Radiation;
import com.cyberfarm.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Held in either hand it clicks faster where radiation is higher and shows the level above the hotbar. */
public class GeigerCounterItem extends Item {
    public GeigerCounterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player) || (player.getMainHandItem() != stack && player.getOffhandItem() != stack)) {
            return;
        }
        float radiation = Radiation.level(level, player.blockPosition());
        // A faint background tick everywhere, a crackle in contaminated land
        float chance = Math.min(0.85F, 0.01F + radiation / 150.0F);
        if (level.getRandom().nextFloat() < chance) {
            level.playSound(null, player.blockPosition(), ModSounds.GEIGER_CLICK.get(), SoundSource.PLAYERS,
                    0.4F, 0.9F + level.getRandom().nextFloat() * 0.3F);
        }
        if (level.getGameTime() % 20 == 0) {
            ChatFormatting color = radiation >= Radiation.SICKER ? ChatFormatting.RED
                    : radiation >= Radiation.SICK ? ChatFormatting.GOLD : ChatFormatting.GREEN;
            String band = radiation >= Radiation.SICKER ? "danger" : radiation >= Radiation.SICK ? "unsafe" : "safe";
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.geiger", Math.round(radiation),
                    Component.translatable("message.cyberfarm.geiger." + band)).withStyle(color));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.cyberfarm.geiger").withStyle(ChatFormatting.GRAY));
    }
}
