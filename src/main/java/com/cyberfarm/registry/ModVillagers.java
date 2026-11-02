package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.google.common.collect.ImmutableSet;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import com.cyberfarm.port.DeferredHolder;
import com.cyberfarm.port.DeferredRegister;

/**
 * The Drone Mechanist: a villager who takes a Supply Terminal as his job site, posts supply
 * contracts on it and sells CyberFarm parts. His trades are in MechanistTrades.
 */
public class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, CyberFarm.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, CyberFarm.MODID);

    public static final DeferredHolder<PoiType, PoiType> SUPPLY_TERMINAL = POI_TYPES.register("supply_terminal",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.SUPPLY_TERMINAL.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> DRONE_MECHANIST = PROFESSIONS.register("drone_mechanist",
            () -> new VillagerProfession("drone_mechanist",
                    poi -> poi.is(SUPPLY_TERMINAL.getKey()), poi -> poi.is(SUPPLY_TERMINAL.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_TOOLSMITH));
}
