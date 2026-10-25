package com.cyberfarm.client;

import com.cyberfarm.item.DroneItem;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Item model condition "cyberfarm:mini_drone": the drone was built on a Mini Drone Frame, so it gets its own sprite. */
public record MiniDroneProperty() implements ConditionalItemModelProperty {
    public static final MapCodec<MiniDroneProperty> MAP_CODEC = MapCodec.unit(new MiniDroneProperty());

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
        return DroneItem.getModules(stack).isMini();
    }

    @Override
    public MapCodec<MiniDroneProperty> type() {
        return MAP_CODEC;
    }
}
