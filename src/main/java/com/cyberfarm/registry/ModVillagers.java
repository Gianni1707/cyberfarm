package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.village.Blueprints;
import com.google.common.collect.ImmutableSet;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Drone Mechanist: a villager who takes a Supply Terminal as his job site, posts supply
 * contracts on it and sells CyberFarm parts. His trades are data (trade_set/drone_mechanist).
 * Also the blueprints of the tech village each player has learned.
 */
public class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, CyberFarm.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, CyberFarm.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CyberFarm.MODID);

    /** The village blueprints a player has learned from discs: kept through death, and known to that player's client. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Blueprints>> BLUEPRINTS = ATTACHMENT_TYPES.register("blueprints",
            () -> AttachmentType.builder(() -> Blueprints.NONE).serialize(Blueprints.CODEC).copyOnDeath()
                    .sync((holder, to) -> holder == to, Blueprints.STREAM_CODEC).build());

    public static final DeferredHolder<PoiType, PoiType> SUPPLY_TERMINAL = POI_TYPES.register("supply_terminal",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.SUPPLY_TERMINAL.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> DRONE_MECHANIST = PROFESSIONS.register("drone_mechanist",
            () -> new VillagerProfession(Component.translatable("entity.cyberfarm.villager.drone_mechanist"),
                    poi -> poi.is(SUPPLY_TERMINAL.getKey()), poi -> poi.is(SUPPLY_TERMINAL.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_TOOLSMITH,
                    Int2ObjectMap.ofEntries(Int2ObjectMap.entry(1, tradeSet(1)), Int2ObjectMap.entry(2, tradeSet(2)),
                            Int2ObjectMap.entry(3, tradeSet(3)), Int2ObjectMap.entry(4, tradeSet(4)), Int2ObjectMap.entry(5, tradeSet(5)))));

    private static ResourceKey<TradeSet> tradeSet(int level) {
        return ResourceKey.create(Registries.TRADE_SET, CyberFarm.id("drone_mechanist/level_" + level));
    }
}
