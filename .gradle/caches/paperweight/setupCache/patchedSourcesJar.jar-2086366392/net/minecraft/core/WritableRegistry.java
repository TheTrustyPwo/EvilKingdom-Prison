package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceKey;

public abstract class WritableRegistry<T> extends Registry<T> {
    public WritableRegistry(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle) {
        super(key, lifecycle);
    }

    public abstract Holder<T> registerMapping(int rawId, ResourceKey<T> key, T value, Lifecycle lifecycle);

    public abstract Holder<T> register(ResourceKey<T> key, T entry, Lifecycle lifecycle);

    public abstract Holder<T> registerOrOverride(OptionalInt rawId, ResourceKey<T> key, T newEntry, Lifecycle lifecycle);

    public abstract boolean isEmpty();
}
