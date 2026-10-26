package com.cyberfarm.block.entity;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Item filter of the Drone Dock, with the usual options of other mods: up to 9 sample items, as a
 * whitelist or a blacklist, matched by item (optionally with components), by mod, or by a tag picked
 * from the sample's tags. The default is an empty blacklist: everything passes.
 *
 * <p>It also holds the Transport Module's limit: how many of each item the route's destination
 * should hold, 0 for no limit.
 */
public class ItemFilter {
    public static final int SLOTS = 9;
    /** GUI data: the 4 energy slots every machine menu starts with, then the 4 filter settings and the limit. */
    public static final int DATA_SLOTS = MachineBlockEntity.ENERGY_DATA_SLOTS + 5;
    /** The limits the arrows step through; 0 is no limit. */
    private static final int[] LIMITS = {0, 1, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096};
    public static final int MAX_LIMIT = LIMITS[LIMITS.length - 1];

    private final SimpleContainer items;
    private final Runnable onChanged;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id - MachineBlockEntity.ENERGY_DATA_SLOTS) {
                case 0 -> whitelist ? 1 : 0;
                case 1 -> matchComponents ? 1 : 0;
                case 2 -> matchMod ? 1 : 0;
                case 3 -> tagIndex();
                case 4 -> limit;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {}

        @Override
        public int getCount() {
            return DATA_SLOTS;
        }
    };
    private boolean whitelist;
    private boolean matchComponents;
    private boolean matchMod;
    private @Nullable TagKey<Item> tag;
    private int limit;

    public ItemFilter(Runnable onChanged) {
        this.onChanged = onChanged;
        items = new SimpleContainer(SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                // The tag is picked from the first sample: drop it when that sample no longer has it
                if (tag != null && !tagsOf(firstSample()).contains(tag)) {
                    tag = null;
                }
                onChanged.run();
            }
        };
    }

    public SimpleContainer items() {
        return items;
    }

    public ContainerData data() {
        return data;
    }

    public boolean test(ItemStack stack) {
        boolean match = tag != null && stack.is(tag);
        for (int i = 0; i < SLOTS && !match; i++) {
            ItemStack sample = items.getItem(i);
            if (sample.isEmpty()) {
                continue;
            }
            match = matchMod
                    ? BuiltInRegistries.ITEM.getKey(sample.getItem()).getNamespace().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())
                    : matchComponents ? ItemStack.isSameItemSameComponents(sample, stack) : sample.is(stack.getItem());
        }
        return whitelist == match;
    }

    public boolean whitelist() {
        return whitelist;
    }

    public boolean matchComponents() {
        return matchComponents;
    }

    public boolean matchMod() {
        return matchMod;
    }

    public void toggleWhitelist() {
        whitelist = !whitelist;
        onChanged.run();
    }

    public void toggleComponents() {
        matchComponents = !matchComponents;
        onChanged.run();
    }

    public void toggleMod() {
        matchMod = !matchMod;
        onChanged.run();
    }

    /** How many of each item the Transport Module's destination should hold; 0 for no limit. */
    public int limit() {
        return limit;
    }

    /** Steps the limit up or down: through the round values, or by one when {@code fine}. */
    public void stepLimit(int step, boolean fine) {
        if (fine) {
            limit = Math.clamp(limit + step, 0, MAX_LIMIT);
        } else if (step > 0) {
            limit = Arrays.stream(LIMITS).filter(value -> value > limit).findFirst().orElse(MAX_LIMIT);
        } else {
            int below = 0;
            for (int value : LIMITS) {
                if (value < limit) {
                    below = value;
                }
            }
            limit = below;
        }
        onChanged.run();
    }

    /** Tags of the first sample item, sorted so client and server agree on the order. */
    public static List<TagKey<Item>> tagsOf(ItemStack sample) {
        return sample.typeHolder().tags().sorted(Comparator.comparing((TagKey<Item> key) -> key.location().toString())).toList();
    }

    public ItemStack firstSample() {
        for (int i = 0; i < SLOTS; i++) {
            if (!items.getItem(i).isEmpty()) {
                return items.getItem(i);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Index of the chosen tag among the first sample's tags, or -1 for none. */
    public int tagIndex() {
        return tag == null ? -1 : tagsOf(firstSample()).indexOf(tag);
    }

    /** Steps through "no tag" and the first sample's tags, in a loop. */
    public void cycleTag(int step) {
        List<TagKey<Item>> tags = tagsOf(firstSample());
        int index = Math.floorMod(tagIndex() + 1 + step, tags.size() + 1) - 1;
        tag = index < 0 ? null : tags.get(index);
        onChanged.run();
    }

    public void save(ValueOutput output) {
        for (int i = 0; i < SLOTS; i++) {
            output.store("filter_" + i, ItemStack.OPTIONAL_CODEC, items.getItem(i));
        }
        output.putBoolean("whitelist", whitelist);
        output.putBoolean("match_components", matchComponents);
        output.putBoolean("match_mod", matchMod);
        output.storeNullable("tag", Identifier.CODEC, tag == null ? null : tag.location());
        output.putInt("limit", limit);
    }

    public void load(ValueInput input) {
        for (int i = 0; i < SLOTS; i++) {
            items.setItem(i, input.read("filter_" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
        whitelist = input.getBooleanOr("whitelist", false);
        matchComponents = input.getBooleanOr("match_components", false);
        matchMod = input.getBooleanOr("match_mod", false);
        tag = input.read("tag", Identifier.CODEC).map(id -> TagKey.create(Registries.ITEM, id)).orElse(null);
        limit = input.getIntOr("limit", 0);
    }
}
