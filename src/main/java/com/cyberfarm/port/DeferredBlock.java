package com.cyberfarm.port;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

public class DeferredBlock<B extends Block> extends DeferredHolder<Block, B> implements ItemLike {
    public DeferredBlock(RegistryObject<B> object) {
        super(object);
    }

    @Override
    public Item asItem() {
        return get().asItem();
    }

    public ItemStack toStack() {
        return new ItemStack(this);
    }
}
