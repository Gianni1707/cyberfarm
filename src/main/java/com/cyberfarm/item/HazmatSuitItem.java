package com.cyberfarm.item;

import java.util.EnumMap;
import java.util.function.Consumer;

import com.cyberfarm.CyberFarm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * A piece of the Hazmat Suit: each one keeps out a quarter of the radiation, the full suit all of
 * it (see {@code Radiation.protection}). Little armor, and it keeps the cold of coolant out too.
 */
public class HazmatSuitItem extends Item {
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(15, defense(), 10, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            ItemTags.REPAIRS_LEATHER_ARMOR, ResourceKey.create(EquipmentAssets.ROOT_ID, CyberFarm.id("hazmat")));

    private static EnumMap<ArmorType, Integer> defense() {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.HELMET, 1);
        map.put(ArmorType.CHESTPLATE, 3);
        map.put(ArmorType.LEGGINGS, 2);
        map.put(ArmorType.BOOTS, 1);
        map.put(ArmorType.BODY, 3);
        return map;
    }

    public HazmatSuitItem(Properties properties, ArmorType type) {
        super(properties.humanoidArmor(MATERIAL, type));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.cyberfarm.hazmat").withStyle(ChatFormatting.YELLOW));
    }
}
