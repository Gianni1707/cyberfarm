package com.cyberfarm.registry;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.entity.MechEntity;

import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(CyberFarm.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE = ENTITIES.registerEntityType("drone",
            DroneEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.6F, 0.5F).clientTrackingRange(8).noLootTable().fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<MechEntity>> MECH = ENTITIES.registerEntityType("mech",
            MechEntity::new, MobCategory.MISC,
            builder -> builder.sized(1.6F, 3.6F).eyeHeight(3.2F).clientTrackingRange(10).noLootTable().fireImmune());

    public static final DeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, CyberFarm.MODID);

    /** A drone's hold, sent whole when it changes. */
    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<List<ItemStack>>> ITEM_LIST = DATA_SERIALIZERS.register("item_list",
            () -> EntityDataSerializer.forValueType(ItemStack.OPTIONAL_LIST_STREAM_CODEC));
}
