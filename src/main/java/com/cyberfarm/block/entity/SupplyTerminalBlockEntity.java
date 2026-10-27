package com.cyberfarm.block.entity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import com.cyberfarm.block.MachineBlock;
import com.cyberfarm.menu.SupplyTerminalMenu;
import com.cyberfarm.multiblock.BuildPlan;
import com.cyberfarm.multiblock.Schematic;
import com.cyberfarm.multiblock.Schematics;
import com.cyberfarm.registry.ModBlockEntities;
import com.cyberfarm.registry.ModItems;
import com.cyberfarm.registry.ModVillagers;
import com.cyberfarm.village.VillagePlan;
import com.cyberfarm.village.VillagePlan.Unlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Supply Terminal: the job site of a Drone Mechanist. While he works it, the village posts supply
 * contracts on its board: three ordinary ones (food and materials, bigger and richer as he levels
 * up) and, in a fourth place, its events: a Market Day every morning until dusk, and a call for
 * arms while a raid is on. Some contracts also pay a bonus item.
 *
 * <p>Whatever goes into the delivery slot, by hand, hopper or Transport drone, counts towards
 * them; it only takes what is still wanted. A filled contract pays into the payout slot, gives the
 * mechanist experience, raises the village's opinion of the terminal's owner (better prices) and
 * makes the village more prosperous.
 *
 * <p>The terminal also holds the tech village the Mechanist proposes: whether a player founded it
 * (that player becomes the owner), its level, its prosperity points and its buildings. Each level
 * hands out blueprint discs ({@link VillagePlan}); a building counts as built once a projection of
 * its blueprint, pinned within {@link #VILLAGE_RADIUS} blocks of the terminal, is complete. Once the
 * points reach the next level's mark and the last level's buildings stand, talking to the Mechanist
 * raises the village a level: it grows (a new villager or an iron golem) and the owner becomes its
 * hero for a while.
 */
public class SupplyTerminalBlockEntity extends MachineBlockEntity {
    public static final int DELIVERY = 0;
    public static final int PAYOUT = 1;
    /** Places on the board: three ordinary contracts, then the event's. */
    public static final int CONTRACTS = 4;
    public static final int EVENT = 3;
    /** GUI data per contract: item, required, delivered, emeralds, bonus item, bonus count, kind, seconds left. */
    public static final int CONTRACT_DATA = 8;
    /** Ticks between refills of the free ordinary places on the board. */
    public static final int NEW_CONTRACT_INTERVAL = 2_400;
    /** Prosperity points (the emeralds of filled contracts) each village level needs, 1 to 10. */
    public static final int[] VILLAGE_LEVELS = {15, 40, 80, 130, 200, 290, 400, 530, 680, 850};
    public static final int MAX_LEVEL = VILLAGE_LEVELS.length;
    /** How far the village reaches from its terminal, every way: its buildings go inside. */
    public static final int VILLAGE_RADIUS = 64;
    private static final int SITE_CHECK = 100;
    private static final int MECHANIST_CHECK = 40;
    private static final double RANGE = 48.0;
    /** How far the Mechanist strays from his terminal before he heads back. */
    private static final double LEASH = 10.0;
    private static final float BONUS_CHANCE = 0.3F;
    private static final int MARKET_SCALE = 3;
    private static final int DAY = 24_000;
    private static final int DUSK = 12_000;

    public enum Kind { NORMAL, MARKET, DEFENSE }

    /** What a village may ask for, from which mechanist level, with its size and pay at level 1. */
    public record Template(Supplier<? extends Item> item, int count, int emeralds, int minLevel) {}

    public static final List<Template> TEMPLATES = List.of(
            new Template(() -> Items.BREAD, 32, 3, 1),
            new Template(() -> Items.CARROT, 64, 2, 1),
            new Template(() -> Items.POTATO, 64, 2, 1),
            new Template(() -> Items.WHEAT, 64, 2, 1),
            new Template(() -> Items.BEETROOT, 48, 2, 1),
            new Template(() -> Items.APPLE, 24, 2, 1),
            new Template(() -> Items.WHITE_WOOL, 32, 3, 1),
            new Template(() -> Items.COOKED_BEEF, 32, 4, 2),
            new Template(() -> Items.LEATHER, 24, 4, 2),
            new Template(() -> Items.OAK_LOG, 64, 3, 2),
            new Template(() -> Items.EGG, 16, 2, 2),
            new Template(() -> Items.PUMPKIN, 24, 3, 2),
            new Template(() -> Items.MELON_SLICE, 64, 3, 2),
            new Template(() -> Items.IRON_INGOT, 32, 6, 3),
            new Template(() -> Items.COOKED_CHICKEN, 32, 4, 3),
            new Template(() -> Items.GLASS, 64, 4, 3),
            new Template(() -> Items.COD, 32, 4, 3),
            new Template(() -> Items.STRING, 32, 3, 3),
            new Template(ModItems.ALUMINUM_INGOT, 32, 8, 4),
            new Template(ModItems.BASIC_CIRCUIT, 16, 8, 4),
            new Template(() -> Items.GOLD_INGOT, 16, 6, 4),
            new Template(ModItems.CARBON_FIBER, 16, 12, 5),
            new Template(ModItems.ADVANCED_CIRCUIT, 8, 14, 5));

    /** What the village needs while a raid is on. */
    public static final List<Template> DEFENSE = List.of(
            new Template(() -> Items.IRON_INGOT, 24, 10, 1),
            new Template(() -> Items.ARROW, 64, 8, 1),
            new Template(() -> Items.SHIELD, 4, 10, 1));

    /** A bonus a contract may pay besides its emeralds, from which mechanist level. */
    public record Bonus(Supplier<? extends Item> item, int count, int minLevel) {}

    public static final List<Bonus> BONUSES = List.of(
            new Bonus(ModItems.BATTERY_CELL, 4, 1),
            new Bonus(ModItems.BASIC_CIRCUIT, 4, 1),
            new Bonus(ModItems.ALUMINUM_INGOT, 8, 1),
            new Bonus(ModItems.ROTOR, 4, 1),
            new Bonus(ModItems.SPEED_ADDON, 1, 2),
            new Bonus(ModItems.STACK_ADDON, 1, 2),
            new Bonus(ModItems.DRONE_CORE, 2, 2),
            new Bonus(ModItems.ADVANCED_CIRCUIT, 2, 3),
            new Bonus(ModItems.FORTUNE_ADDON, 1, 3),
            new Bonus(ModItems.BATTERY_ADDON, 1, 3),
            new Bonus(ModItems.CARBON_FIBER, 4, 4),
            new Bonus(ModItems.ANCHOR_ADDON, 1, 4),
            new Bonus(ModItems.SILK_TOUCH_ADDON, 1, 4),
            new Bonus(ModItems.ELITE_CIRCUIT, 1, 5));

    /** A posted contract: how many of the item the village wants, how many came in, what it pays, and until when. */
    public static final class Contract {
        final Item item;
        final int required;
        int delivered;
        final int emeralds;
        final ItemStack bonus;
        final Kind kind;
        /** Game time it closes at, or -1 for open until filled. */
        final long deadline;

        Contract(Item item, int required, int delivered, int emeralds, ItemStack bonus, Kind kind, long deadline) {
            this.item = item;
            this.required = required;
            this.delivered = delivered;
            this.emeralds = emeralds;
            this.bonus = bonus;
            this.kind = kind;
            this.deadline = deadline;
        }

        int missing() {
            return required - delivered;
        }
    }

    private final @Nullable Contract[] contracts = new Contract[CONTRACTS];
    /** Rewards earned that did not fit in the payout slot yet. */
    private final List<ItemStack> pending = new ArrayList<>();
    private @Nullable UUID owner;
    private int cooldown;
    private int checkCooldown;
    /** The mechanist working this terminal, as last found: his level, 0 for nobody. */
    private int mechanistLevel;
    private int prosperity;
    /** The last day a Market Day was held. */
    private long marketDay = -1;
    private boolean founded;
    private int villageLevel;
    private String ownerName = "";
    /** Players who heard the Mechanist's story. */
    private final Set<UUID> met = new HashSet<>();
    /** The highest village level whose discs each player has had. */
    private final Map<UUID, Integer> discsGiven = new HashMap<>();
    /** Each building's highest finished tier, and where it stands. */
    private final Map<String, Built> built = new HashMap<>();
    /** Where projections of unbuilt blueprints were pinned, by schematic: checked until they are complete. */
    private final Map<Identifier, Placed> sites = new HashMap<>();
    /** Buildings finished since the Mechanist last spoke of them. */
    private final List<Identifier> news = new ArrayList<>();
    private int siteCheck;

    /** A blueprint placed in the world: where its origin is and which way its front faces. */
    public record Placed(BlockPos origin, Direction facing) {
        static final Codec<Placed> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("origin").forGetter(Placed::origin),
                Direction.CODEC.fieldOf("facing").forGetter(Placed::facing)).apply(i, Placed::new));
    }

    /** A building's highest finished tier and where it stands. */
    public record Built(int tier, Placed at) {
        static final Codec<Built> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("tier").forGetter(Built::tier),
                Placed.CODEC.fieldOf("at").forGetter(Built::at)).apply(i, Built::new));
    }

    public SupplyTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPPLY_TERMINAL.get(), pos, state, 2, 0, 0, 0);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public boolean founded() {
        return founded;
    }

    public int villageLevel() {
        return villageLevel;
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public boolean hasMet(UUID player) {
        return met.contains(player);
    }

    public void meet(UUID player) {
        if (met.add(player)) {
            setChanged();
        }
    }

    /**
     * {@code player} takes up the Mechanist's offer: the village is theirs. The Outpost around the
     * terminal counts as its first tier, standing where the terminal says, so its upgrades line up.
     */
    public void found(Player player) {
        founded = true;
        owner = player.getUUID();
        ownerName = player.getGameProfile().name();
        Unlock upgrade = new Unlock(VillagePlan.OUTPOST, 2);
        Schematics.byId(upgrade.schematic()).ifPresent(schematic -> {
            Direction facing = getBlockState().getValue(MachineBlock.FACING);
            built.put(VillagePlan.OUTPOST, new Built(1, new Placed(schematic.originFor(VillagePlan.OUTPOST_TERMINAL, worldPosition, facing), facing)));
        });
        setChanged();
    }

    /** The founded village whose terminal is nearest {@code pos}, with {@code pos} inside its bounds. */
    public static Optional<SupplyTerminalBlockEntity> villageAt(ServerLevel level, BlockPos pos) {
        return level.getPoiManager().getInSquare(poi -> poi.is(ModVillagers.SUPPLY_TERMINAL.getKey()), pos, VILLAGE_RADIUS, PoiManager.Occupancy.ANY)
                .map(poi -> level.getBlockEntity(poi.getPos()) instanceof SupplyTerminalBlockEntity terminal ? terminal : null)
                .filter(terminal -> terminal != null && terminal.founded && terminal.contains(pos))
                .min(Comparator.comparingDouble(terminal -> terminal.worldPosition.distSqr(pos)));
    }

    /** Whether {@code pos} is inside the village's bounds: a square {@link #VILLAGE_RADIUS} blocks from the terminal each way. */
    public boolean contains(BlockPos pos) {
        return Math.abs(pos.getX() - worldPosition.getX()) <= VILLAGE_RADIUS && Math.abs(pos.getZ() - worldPosition.getZ()) <= VILLAGE_RADIUS;
    }

    /** Whether the village's levels have unlocked this blueprint. */
    public boolean isUnlocked(Unlock unlock) {
        for (int i = 0; i < villageLevel; i++) {
            if (VillagePlan.LEVELS.get(i).contains(unlock)) {
                return true;
            }
        }
        return false;
    }

    public int builtTier(String building) {
        Built at = built.get(building);
        return at != null ? at.tier() : 0;
    }

    /** Where an upgrade goes: over the tier before it, as built. Empty for a first tier, or before the tier below stands. */
    public Optional<Placed> upgradeSite(Unlock unlock) {
        Built below = built.get(unlock.building());
        return unlock.tier() > 1 && below != null && below.tier() >= unlock.tier() - 1 ? Optional.of(below.at()) : Optional.empty();
    }

    /** A projection of the blueprint was pinned at {@code at}: watch it until it is complete. */
    public void addSite(Unlock unlock, Placed at) {
        if (isUnlocked(unlock) && builtTier(unlock.building()) < unlock.tier() && contains(at.origin())) {
            sites.put(unlock.schematic(), at);
            siteCheck = 0;
            setChanged();
        }
    }

    /** The discs of the levels {@code player} has not had discs for yet; they count as given. */
    public List<Unlock> takeDiscs(UUID player) {
        int from = discsGiven.getOrDefault(player, 0);
        List<Unlock> discs = new ArrayList<>();
        for (int i = from; i < villageLevel; i++) {
            discs.addAll(VillagePlan.LEVELS.get(i));
        }
        if (!discs.isEmpty()) {
            discsGiven.put(player, villageLevel);
            setChanged();
        }
        return discs;
    }

    /** Buildings finished since the last talk, oldest first; forgotten once read. */
    public List<Identifier> takeNews() {
        List<Identifier> taken = List.copyOf(news);
        if (!taken.isEmpty()) {
            news.clear();
            setChanged();
        }
        return taken;
    }

    /** Checks the pinned projections: a complete one makes its building count as built. */
    private void checkSites(ServerLevel level) {
        sites.entrySet().removeIf(entry -> {
            Optional<Unlock> unlock = VillagePlan.unlock(entry.getKey());
            Optional<Schematic> schematic = Schematics.byId(entry.getKey());
            if (unlock.isEmpty() || schematic.isEmpty() || builtTier(unlock.get().building()) >= unlock.get().tier()) {
                return true;
            }
            Placed at = entry.getValue();
            if (!level.isLoaded(at.origin()) || new BuildPlan(schematic.get(), at.origin(), at.facing()).remaining(level) > 0) {
                return false;
            }
            built.put(unlock.get().building(), new Built(unlock.get().tier(), at));
            news.add(entry.getKey());
            level.playSound(null, at.origin(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.2F);
            for (ServerPlayer player : level.players()) {
                if (contains(player.blockPosition())) {
                    player.sendOverlayMessage(Component.translatable("message.cyberfarm.village.built", unlock.get().displayName()));
                }
            }
            return true;
        });
        setChanged();
    }

    /** True when the points reach the next level's mark and nothing is left to build for it. */
    public boolean canLevelUp() {
        return founded && villageLevel < MAX_LEVEL && prosperity >= VILLAGE_LEVELS[villageLevel] && missingBuilding() == null;
    }

    public boolean needsProsperity() {
        return villageLevel < MAX_LEVEL && prosperity < VILLAGE_LEVELS[villageLevel];
    }

    /** A building the last level unlocked that is not built yet, or null: the next level waits for it. */
    public @Nullable Component missingBuilding() {
        if (villageLevel == 0) {
            return null;
        }
        for (Unlock unlock : VillagePlan.LEVELS.get(villageLevel - 1)) {
            if (builtTier(unlock.building()) < unlock.tier()) {
                return unlock.displayName();
            }
        }
        return null;
    }

    /** The Mechanist raises the village a level: it grows and celebrates. */
    public void raiseLevel(ServerLevel level) {
        villageLevel++;
        milestone(level, villageLevel);
        setChanged();
    }

    public boolean marketDay() {
        Contract event = contracts[EVENT];
        return event != null && event.kind == Kind.MARKET;
    }

    /** How many more of {@code item} the open contracts want. */
    private int wanted(Item item) {
        int wanted = 0;
        for (Contract contract : contracts) {
            if (contract != null && contract.item == item) {
                wanted += contract.missing();
            }
        }
        return wanted;
    }

    @Override
    protected boolean isItemValid(int index, ItemResource resource) {
        return index == DELIVERY && wanted(resource.getItem()) > 0;
    }

    @Override
    protected int getSlotLimit(int index, ItemResource resource) {
        return index == DELIVERY ? wanted(resource.getItem()) : super.getSlotLimit(index, resource);
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--checkCooldown <= 0) {
            checkCooldown = MECHANIST_CHECK;
            Villager mechanist = mechanist(level);
            mechanistLevel = mechanist != null ? mechanist.getVillagerData().level() : 0;
            if (mechanist != null) {
                events(level);
                // He keeps close to his terminal: wandered off, he walks back
                if (!mechanist.isSleeping() && mechanist.distanceToSqr(pos.getCenter()) > LEASH * LEASH) {
                    mechanist.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, 0.6F, 3));
                }
            }
        }
        // Every couple of minutes the mechanist fills whatever ordinary places on the board are free
        if (mechanistLevel > 0 && --cooldown <= 0) {
            cooldown = NEW_CONTRACT_INTERVAL;
            for (int i = 0; i < EVENT; i++) {
                if (contracts[i] == null) {
                    contracts[i] = newContract(level, pick(level, TEMPLATES), Kind.NORMAL, -1);
                }
            }
            setChanged();
        }
        if (!sites.isEmpty() && --siteCheck <= 0) {
            siteCheck = SITE_CHECK;
            checkSites(level);
        }
        expire(level);
        deliver(level);
        pay();
    }

    /** Posts the day's Market Day each morning and a call for arms while a raid is on; ends a call when its raid does. */
    private void events(ServerLevel level) {
        Contract event = contracts[EVENT];
        boolean raid = level.getRaidAt(worldPosition) != null;
        if (event != null && event.kind == Kind.DEFENSE && !raid) {
            contracts[EVENT] = null;
            setChanged();
            return;
        }
        if (event != null && event.kind == Kind.DEFENSE) {
            return;
        }
        if (raid) {
            // The raid comes first: a market still open makes way
            contracts[EVENT] = newContract(level, DEFENSE.get(level.getRandom().nextInt(DEFENSE.size())), Kind.DEFENSE, -1);
            level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 0.8F);
            setChanged();
            return;
        }
        long time = level.getOverworldClockTime();
        long day = time / DAY;
        if (event == null && day != marketDay && time % DAY < DUSK / 2) {
            marketDay = day;
            long deadline = level.getGameTime() + DUSK - time % DAY;
            contracts[EVENT] = newContract(level, pick(level, TEMPLATES), Kind.MARKET, deadline);
            level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 1.2F);
            setChanged();
        }
    }

    /** Contracts whose time ran out close, and what came in for them is gone. */
    private void expire(ServerLevel level) {
        for (int i = 0; i < CONTRACTS; i++) {
            Contract contract = contracts[i];
            if (contract != null && contract.deadline >= 0 && level.getGameTime() >= contract.deadline) {
                contracts[i] = null;
                level.playSound(null, worldPosition, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                setChanged();
            }
        }
    }

    private void deliver(ServerLevel level) {
        ItemStack delivered = getStack(DELIVERY);
        if (delivered.isEmpty()) {
            return;
        }
        int left = delivered.getCount();
        for (int i = 0; i < CONTRACTS && left > 0; i++) {
            Contract contract = contracts[i];
            if (contract == null || contract.item != delivered.getItem()) {
                continue;
            }
            int taken = Math.min(left, contract.missing());
            contract.delivered += taken;
            left -= taken;
            if (contract.missing() <= 0) {
                complete(level, contract);
                contracts[i] = null;
            }
        }
        setStack(DELIVERY, delivered.copyWithCount(left));
        setChanged();
    }

    /** Moves what the terminal owes into the payout slot, as far as it fits. */
    private void pay() {
        while (!pending.isEmpty()) {
            ItemStack owed = pending.getFirst();
            ItemStack payout = getStack(PAYOUT);
            if (!payout.isEmpty() && !ItemStack.isSameItemSameComponents(payout, owed)) {
                return;
            }
            int room = owed.getMaxStackSize() - payout.getCount();
            int moved = Math.min(room, owed.getCount());
            if (moved <= 0) {
                return;
            }
            setStack(PAYOUT, owed.copyWithCount(payout.getCount() + moved));
            owed.shrink(moved);
            if (owed.isEmpty()) {
                pending.removeFirst();
            }
            setChanged();
        }
    }

    /** A filled contract: its pay, the mechanist's experience, the village's goodwill and its prosperity. */
    private void complete(ServerLevel level, Contract contract) {
        pending.add(new ItemStack(Items.EMERALD, contract.emeralds));
        if (!contract.bonus.isEmpty()) {
            pending.add(contract.bonus.copy());
        }
        Villager mechanist = mechanist(level);
        if (mechanist != null) {
            int villagerLevel = mechanist.getVillagerData().level();
            mechanist.setVillagerXp(mechanist.getVillagerXp() + 5 + 5 * villagerLevel);
            levelUp(mechanist, level);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mechanist.getX(), mechanist.getY() + 2.0, mechanist.getZ(), 8, 0.4, 0.3, 0.4, 0.0);
        }
        if (owner != null) {
            for (Villager villager : level.getEntitiesOfClass(Villager.class, new AABB(worldPosition).inflate(RANGE))) {
                villager.getGossips().add(owner, villager == mechanist ? GossipType.MAJOR_POSITIVE : GossipType.MINOR_POSITIVE, villager == mechanist ? 10 : 5);
            }
        }
        if (contract.kind == Kind.DEFENSE) {
            // The village arms itself: two iron golems join the fight
            spawnNear(level, EntityType.IRON_GOLEM);
            spawnNear(level, EntityType.IRON_GOLEM);
        }
        level.playSound(null, worldPosition, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6F, 1.2F);
        prosperity += contract.emeralds;
    }

    /**
     * The village reaches a new level: it grows (a new villager at odd levels, an iron golem at even
     * ones), celebrates, and makes the owner its hero for twenty minutes.
     */
    private void milestone(ServerLevel level, int reached) {
        if (reached % 2 == 1) {
            Villager villager = spawnNear(level, EntityType.VILLAGER);
            if (villager != null) {
                villager.setAge(-24_000);
            }
        }
        if (reached % 2 == 0) {
            spawnNear(level, EntityType.IRON_GOLEM);
        }
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                40, 0.6, 0.6, 0.6, 0.3);
        level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.5F, 1.0F);
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (owner != null && level.getPlayerByUUID(owner) instanceof ServerPlayer player && player.distanceToSqr(worldPosition.getCenter()) < RANGE * RANGE * 4) {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 20 * 60 * 20, Math.min(reached - 1, 4)));
            player.sendOverlayMessage(Component.translatable("message.cyberfarm.terminal.prosperity", reached));
        }
    }

    /** Spawns a villager or a golem on free ground close to the terminal. */
    private <T extends net.minecraft.world.entity.Mob> @Nullable T spawnNear(ServerLevel level, EntityType<T> type) {
        BlockPos spot = worldPosition.above();
        search:
        for (int r = 1; r <= 4; r++) {
            for (BlockPos at : BlockPos.betweenClosed(worldPosition.offset(-r, -1, -r), worldPosition.offset(r, 1, r))) {
                if (level.isEmptyBlock(at) && level.isEmptyBlock(at.above()) && level.isEmptyBlock(at.above(2))
                        && !level.getBlockState(at.below()).getCollisionShape(level, at.below()).isEmpty()) {
                    spot = at.immutable();
                    break search;
                }
            }
        }
        return type.spawn(level, spot, EntitySpawnReason.EVENT);
    }

    /**
     * Raises the mechanist a level once his experience is enough, as a trade would.
     * shortcut: calls Villager's private increaseMerchantCareer by reflection (NeoForge runs on
     * Mojang's names); if it is ever renamed, he only gets the level and his new trades wait for
     * his next level-up from trading.
     */
    private static void levelUp(Villager villager, ServerLevel level) {
        int current = villager.getVillagerData().level();
        if (!VillagerData.canLevelUp(current) || villager.getVillagerXp() < VillagerData.getMaxXpPerLevel(current)) {
            return;
        }
        try {
            Method career = Villager.class.getDeclaredMethod("increaseMerchantCareer", ServerLevel.class);
            career.setAccessible(true);
            career.invoke(villager, level);
        } catch (ReflectiveOperationException | RuntimeException e) {
            villager.setVillagerData(villager.getVillagerData().withLevel(current + 1));
        }
    }

    /** A template the mechanist's level allows, for something not already on the board. */
    private Template pick(ServerLevel level, List<Template> from) {
        List<Template> open = new ArrayList<>();
        for (Template template : from) {
            if (template.minLevel() <= mechanistLevel && wanted(template.item().get()) == 0) {
                open.add(template);
            }
        }
        return open.isEmpty() ? from.getFirst() : open.get(level.getRandom().nextInt(open.size()));
    }

    /**
     * A contract for {@code template}: sized and paid by the mechanist's level, give or take a
     * quarter, three times over for a Market Day, half again in a thriving village; maybe with a bonus.
     */
    private Contract newContract(ServerLevel level, Template template, Kind kind, long deadline) {
        var random = level.getRandom();
        float scale = (1.0F + 0.5F * (mechanistLevel - 1)) * (0.75F + 0.5F * random.nextFloat()) * (kind == Kind.MARKET ? MARKET_SCALE : 1);
        int count = Math.max(8, Math.round(template.count() * scale / 8.0F) * 8);
        float pay = template.emeralds() * (float) count / template.count() * (villageLevel >= MAX_LEVEL ? 1.5F : 1.0F);
        ItemStack bonus = ItemStack.EMPTY;
        if (kind != Kind.NORMAL || random.nextFloat() < BONUS_CHANCE) {
            List<Bonus> open = BONUSES.stream().filter(b -> b.minLevel() <= mechanistLevel).toList();
            Bonus chosen = open.get(random.nextInt(open.size()));
            bonus = new ItemStack(chosen.item().get(), chosen.count());
        }
        return new Contract(template.item().get(), count, 0, Math.max(1, Math.round(pay)), bonus, kind, deadline);
    }

    /** The Drone Mechanist whose job site this terminal is, or null. */
    private @Nullable Villager mechanist(ServerLevel level) {
        for (Villager villager : level.getEntitiesOfClass(Villager.class, new AABB(worldPosition).inflate(RANGE),
                v -> v.getVillagerData().profession().is(ModVillagers.DRONE_MECHANIST.getKey()))) {
            boolean here = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                    .map(site -> site.pos().equals(worldPosition) && site.dimension() == level.dimension()).orElse(false);
            if (here) {
                return villager;
            }
        }
        return null;
    }

    @Override
    protected int getExtraData(int id) {
        if (id < CONTRACTS * CONTRACT_DATA) {
            Contract contract = contracts[id / CONTRACT_DATA];
            int field = id % CONTRACT_DATA;
            if (contract == null) {
                return field == 0 || field == 4 ? -1 : 0;
            }
            return switch (field) {
                case 0 -> BuiltInRegistries.ITEM.getId(contract.item);
                case 1 -> contract.required;
                case 2 -> contract.delivered;
                case 3 -> contract.emeralds;
                case 4 -> contract.bonus.isEmpty() ? -1 : BuiltInRegistries.ITEM.getId(contract.bonus.getItem());
                case 5 -> contract.bonus.getCount();
                case 6 -> contract.kind.ordinal();
                default -> contract.deadline < 0 || level == null ? -1 : (int) Math.max(0, (contract.deadline - level.getGameTime()) / 20);
            };
        }
        return switch (id - CONTRACTS * CONTRACT_DATA) {
            case 0 -> mechanistLevel;
            case 1 -> Math.max(0, cooldown) / 20;
            case 2 -> prosperity;
            case 3 -> founded ? villageLevel : -1;
            case 4 -> worldPosition.getX();
            case 5 -> worldPosition.getY();
            default -> worldPosition.getZ();
        };
    }

    @Override
    protected int getExtraDataCount() {
        return CONTRACTS * CONTRACT_DATA + 7;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < CONTRACTS; i++) {
            Contract contract = contracts[i];
            if (contract != null) {
                ValueOutput saved = output.child("contract_" + i);
                saved.store("item", BuiltInRegistries.ITEM.byNameCodec(), contract.item);
                saved.putInt("required", contract.required);
                saved.putInt("delivered", contract.delivered);
                saved.putInt("emeralds", contract.emeralds);
                saved.store("bonus", ItemStack.OPTIONAL_CODEC, contract.bonus);
                saved.putInt("kind", contract.kind.ordinal());
                saved.putLong("deadline", contract.deadline);
            }
        }
        output.store("pending", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(pending));
        output.storeNullable("owner", UUIDUtil.CODEC, owner);
        output.putInt("cooldown", cooldown);
        output.putInt("prosperity", prosperity);
        output.putLong("market_day", marketDay);
        output.putBoolean("founded", founded);
        output.putInt("village_level", villageLevel);
        output.putString("owner_name", ownerName);
        output.store("met", UUIDUtil.CODEC.listOf(), List.copyOf(met));
        output.store("discs_given", Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT), Map.copyOf(discsGiven));
        output.store("built", Codec.unboundedMap(Codec.STRING, Built.CODEC), Map.copyOf(built));
        output.store("sites", Codec.unboundedMap(Identifier.CODEC, Placed.CODEC), Map.copyOf(sites));
        output.store("news", Identifier.CODEC.listOf(), List.copyOf(news));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < CONTRACTS; i++) {
            int slot = i;
            contracts[i] = null;
            input.child("contract_" + i).ifPresent(saved -> saved.read("item", BuiltInRegistries.ITEM.byNameCodec()).ifPresent(item ->
                    contracts[slot] = new Contract(item, saved.getIntOr("required", 1), saved.getIntOr("delivered", 0), saved.getIntOr("emeralds", 1),
                            saved.read("bonus", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY),
                            Kind.values()[Math.clamp(saved.getIntOr("kind", 0), 0, Kind.values().length - 1)], saved.getLongOr("deadline", -1L))));
        }
        pending.clear();
        input.read("pending", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> list.stream().filter(stack -> !stack.isEmpty()).forEach(pending::add));
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        cooldown = input.getIntOr("cooldown", 0);
        prosperity = input.getIntOr("prosperity", 0);
        marketDay = input.getLongOr("market_day", -1L);
        founded = input.getBooleanOr("founded", false);
        villageLevel = Math.clamp(input.getIntOr("village_level", 0), 0, MAX_LEVEL);
        ownerName = input.getStringOr("owner_name", "");
        met.clear();
        input.read("met", UUIDUtil.CODEC.listOf()).ifPresent(met::addAll);
        discsGiven.clear();
        input.read("discs_given", Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)).ifPresent(discsGiven::putAll);
        built.clear();
        input.read("built", Codec.unboundedMap(Codec.STRING, Built.CODEC)).ifPresent(built::putAll);
        sites.clear();
        input.read("sites", Codec.unboundedMap(Identifier.CODEC, Placed.CODEC)).ifPresent(sites::putAll);
        news.clear();
        input.read("news", Identifier.CODEC.listOf()).ifPresent(news::addAll);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (owner == null) {
            setOwner(player.getUUID());
        }
        return new SupplyTerminalMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }
}
