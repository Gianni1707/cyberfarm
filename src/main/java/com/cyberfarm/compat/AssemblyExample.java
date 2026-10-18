package com.cyberfarm.compat;

import java.util.ArrayList;
import java.util.List;

import com.cyberfarm.block.entity.DroneAssemblerBlockEntity;
import com.cyberfarm.drone.DroneModules;
import com.cyberfarm.item.DroneFrameItem;
import com.cyberfarm.registry.ModDataComponents;
import com.cyberfarm.registry.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * One Drone Assembler "recipe" for recipe viewers (JEI, REI): a frame tier with its unlocked
 * module slots and the addon slots, each cycling through every module or addon that fits.
 * Plain data, so it can be shared by every recipe viewer plugin.
 */
public record AssemblyExample(DroneFrameItem frame, List<List<ItemStack>> moduleSlots, List<List<ItemStack>> addonSlots, ItemStack output) {
    public static final int ENERGY_COST = DroneAssemblerBlockEntity.ASSEMBLY_TIME * DroneAssemblerBlockEntity.ENERGY_PER_TICK;

    public static List<AssemblyExample> all() {
        List<ItemStack> modules = stacks(ModItems.HARVESTER_MODULE.get(), ModItems.QUARRY_MODULE.get(), ModItems.TRANSPORT_MODULE.get());
        List<ItemStack> addons = stacks(ModItems.SPEED_ADDON.get(), ModItems.STACK_ADDON.get(), ModItems.BATTERY_ADDON.get(),
                ModItems.FORTUNE_ADDON.get(), ModItems.SILK_TOUCH_ADDON.get());

        List<AssemblyExample> examples = new ArrayList<>();
        for (DroneFrameItem frame : List.of(ModItems.BASIC_FRAME.get(), ModItems.ADVANCED_FRAME.get(), ModItems.ELITE_FRAME.get())) {
            List<List<ItemStack>> moduleSlots = new ArrayList<>();
            for (int i = 0; i < DroneAssemblerBlockEntity.MODULE_SLOTS; i++) {
                // Locked slots stay empty; unlocked ones start on a different module each
                moduleSlots.add(i < frame.getModuleSlots() ? rotated(modules, i) : List.of());
            }
            List<List<ItemStack>> addonSlots = new ArrayList<>();
            for (int i = 0; i < DroneAssemblerBlockEntity.ADDON_SLOTS; i++) {
                addonSlots.add(rotated(addons, i));
            }

            List<Item> parts = new ArrayList<>();
            parts.add(frame);
            parts.add(ModItems.HARVESTER_MODULE.get());
            ItemStack drone = new ItemStack(ModItems.DRONE.get());
            drone.set(ModDataComponents.DRONE_MODULES.get(), new DroneModules(parts));
            drone.set(ModDataComponents.ENERGY.get(), 0);

            examples.add(new AssemblyExample(frame, moduleSlots, addonSlots, drone));
        }
        return examples;
    }

    public ItemStack frameStack() {
        return new ItemStack(frame);
    }

    private static List<ItemStack> stacks(ItemLike... items) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemLike item : items) {
            list.add(new ItemStack(item));
        }
        return list;
    }

    private static List<ItemStack> rotated(List<ItemStack> list, int offset) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(list.get((i + offset) % list.size()));
        }
        return result;
    }
}
