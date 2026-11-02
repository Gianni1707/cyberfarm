package com.cyberfarm.port;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegistryObject;

/** The 26.x registry handle, backed by a Forge RegistryObject so the rest of the code keeps its shape. */
public class DeferredHolder<R, T extends R> implements Supplier<T> {
    protected final RegistryObject<T> object;

    public DeferredHolder(RegistryObject<T> object) {
        this.object = object;
    }

    @Override
    public T get() {
        return object.get();
    }

    public T value() {
        return object.get();
    }

    public ResourceLocation getId() {
        return object.getId();
    }

    @SuppressWarnings("unchecked")
    public ResourceKey<R> getKey() {
        return (ResourceKey<R>) object.getKey();
    }
}
