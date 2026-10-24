package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.entity.DroneEntity;
import com.cyberfarm.entity.MechEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(CyberFarm.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE = ENTITIES.registerEntityType("drone",
            DroneEntity::new, MobCategory.MISC,
            builder -> builder.sized(0.6F, 0.5F).clientTrackingRange(8).noLootTable().fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<MechEntity>> MECH = ENTITIES.registerEntityType("mech",
            MechEntity::new, MobCategory.MISC,
            builder -> builder.sized(1.6F, 3.6F).eyeHeight(3.2F).clientTrackingRange(10).noLootTable().fireImmune());
}
