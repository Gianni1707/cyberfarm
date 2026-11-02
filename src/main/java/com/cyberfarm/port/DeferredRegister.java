package com.cyberfarm.port;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;

/** The 26.x DeferredRegister API (with its Blocks/Items/Entities flavours) on top of Forge's. */
public class DeferredRegister<T> {
    protected final net.minecraftforge.registries.DeferredRegister<T> forge;
    private final List<DeferredHolder<T, ? extends T>> entries = new ArrayList<>();

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registry, String modId) {
        this.forge = net.minecraftforge.registries.DeferredRegister.create(registry, modId);
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registry, String modId) {
        return new DeferredRegister<>(registry, modId);
    }

    public static Blocks createBlocks(String modId) {
        return new Blocks(modId);
    }

    public static Items createItems(String modId) {
        return new Items(modId);
    }

    public static Entities createEntities(String modId) {
        return new Entities(modId);
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> sup) {
        return track(new DeferredHolder<>(forge.register(name, sup)));
    }

    protected <H extends DeferredHolder<T, ? extends T>> H track(H holder) {
        entries.add(holder);
        return holder;
    }

    /** Everything registered here, in order. */
    public Collection<DeferredHolder<T, ? extends T>> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void register(IEventBus bus) {
        forge.register(bus);
    }

    public static class Blocks extends DeferredRegister<Block> {
        Blocks(String modId) {
            super(Registries.BLOCK, modId);
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
                UnaryOperator<BlockBehaviour.Properties> props) {
            return track(new DeferredBlock<>(forge.register(name, () -> factory.apply(props.apply(BlockBehaviour.Properties.of())))));
        }

        public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> factory) {
            return registerBlock(name, factory, UnaryOperator.identity());
        }

        public DeferredBlock<Block> registerSimpleBlock(String name, UnaryOperator<BlockBehaviour.Properties> props) {
            return registerBlock(name, Block::new, props);
        }
    }

    public static class Items extends DeferredRegister<Item> {
        Items(String modId) {
            super(Registries.ITEM, modId);
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> factory,
                UnaryOperator<Item.Properties> props) {
            return track(new DeferredItem<>(forge.register(name, () -> factory.apply(props.apply(new Item.Properties())))));
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> factory) {
            return registerItem(name, factory, UnaryOperator.identity());
        }

        public DeferredItem<Item> registerSimpleItem(String name, UnaryOperator<Item.Properties> props) {
            return registerItem(name, Item::new, props);
        }

        public DeferredItem<Item> registerSimpleItem(String name) {
            return registerItem(name, Item::new);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(DeferredBlock<?> block, UnaryOperator<Item.Properties> props) {
            return registerItem(block.getId().getPath(), p -> new BlockItem(block.get(), p), props);
        }

        public DeferredItem<BlockItem> registerSimpleBlockItem(DeferredBlock<?> block) {
            return registerSimpleBlockItem(block, UnaryOperator.identity());
        }
    }

    public static class Entities extends DeferredRegister<EntityType<?>> {
        Entities(String modId) {
            super(Registries.ENTITY_TYPE, modId);
        }

        public <E extends Entity> DeferredHolder<EntityType<?>, EntityType<E>> registerEntityType(String name,
                EntityType.EntityFactory<E> factory, MobCategory category, UnaryOperator<EntityType.Builder<E>> builder) {
            return track(new DeferredHolder<>(forge.register(name, () -> builder.apply(EntityType.Builder.of(factory, category)).build(name))));
        }
    }
}
