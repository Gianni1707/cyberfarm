package com.cyberfarm.item;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.cyberfarm.CyberFarm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/**
 * A piece of the Hazmat Suit: each one keeps out a quarter of the radiation, the full suit all of
 * it (see {@code Radiation.protection}). Little armor, and it keeps the cold of coolant out too.
 */
public class HazmatSuitItem extends ArmorItem {
    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            // Base durability 15 as on 26.x, scaled per piece like vanilla
            return 15 * switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
        }

        @Override
        public int getDefenseForType(Type type) {
            return switch (type) {
                case HELMET, BOOTS -> 1;
                case CHESTPLATE -> 3;
                case LEGGINGS -> 2;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 10;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.LEATHER);
        }

        @Override
        public String getName() {
            return CyberFarm.id("hazmat").toString();
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    public HazmatSuitItem(Properties properties, Type type) {
        super(MATERIAL, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.cyberfarm.hazmat").withStyle(ChatFormatting.YELLOW));
    }
}
