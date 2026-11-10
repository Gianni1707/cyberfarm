package com.cyberfarm.registry;

import java.util.List;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.entity.MechEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.cyberfarm.port.DeferredHolder;
import com.cyberfarm.port.DeferredRegister;
import net.minecraft.world.item.ItemStack;

public class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(CyberFarm.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE = ENTITIES.registerEntityType("drone",
            DroneEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.6F, 0.5F).clientTrackingRange(8).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<MechEntity>> MECH = ENTITIES.registerEntityType("mech",
            MechEntity::new, MobCategory.MISC,
            builder -> builder.sized(1.6F, 3.6F).clientTrackingRange(10).fireImmune());

    public static final DeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, CyberFarm.MODID);

    /** A drone's hold, sent whole when it changes. */
    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<List<ItemStack>>> ITEM_LIST = DATA_SERIALIZERS.register("item_list",
            () -> EntityDataSerializer.simple((buf, list) -> buf.writeCollection(list, FriendlyByteBuf::writeItem),
                    buf -> buf.readList(FriendlyByteBuf::readItem)));
}
