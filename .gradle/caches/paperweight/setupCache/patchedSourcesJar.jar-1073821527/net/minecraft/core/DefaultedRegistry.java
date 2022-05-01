package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class DefaultedRegistry<T> extends MappedRegistry<T> {
    private final ResourceLocation defaultKey;
    private Holder<T> defaultValue;

    public DefaultedRegistry(String defaultId, ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, @Nullable Function<T, Holder.Reference<T>> valueToEntryFunction) {
        super(key, lifecycle, valueToEntryFunction);
        this.defaultKey = new ResourceLocation(defaultId);
    }

    @Override
    public Holder<T> registerMapping(int rawId, ResourceKey<T> key, T value, Lifecycle lifecycle) {
        Holder<T> holder = super.registerMapping(rawId, key, value, lifecycle);
        if (this.defaultKey.equals(key.location())) {
            this.defaultValue = holder;
        }

        return holder;
    }

    @Override
    public int getId(@Nullable T value) {
        int i = super.getId(value);
        return i == -1 ? super.getId(this.defaultValue.value()) : i;
    }

    @Nonnull
    @Override
    public ResourceLocation getKey(T value) {
        ResourceLocation resourceLocation = super.getKey(value);
        return resourceLocation == null ? this.defaultKey : resourceLocation;
    }

    @Nonnull
    @Override
    public T get(@Nullable ResourceLocation id) {
        T object = super.get(id);
        return (T)(object == null ? this.defaultValue.value() : object);
    }

    @Override
    public Optional<T> getOptional(@Nullable ResourceLocation id) {
        return Optional.ofNullable(super.get(id));
    }

    @Nonnull
    @Override
    public T byId(int index) {
        T object = super.byId(index);
        return (T)(object == null ? this.defaultValue.value() : object);
    }

    @Override
    public Optional<Holder<T>> getRandom(Random random) {
        return super.getRandom(random).or(() -> {
            return Optional.of(this.defaultValue);
        });
    }

    public ResourceLocation getDefaultKey() {
        return this.defaultKey;
    }
}
