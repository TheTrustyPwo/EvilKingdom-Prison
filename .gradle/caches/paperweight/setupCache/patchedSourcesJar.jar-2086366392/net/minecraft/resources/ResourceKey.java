package net.minecraft.resources;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Registry;

public class ResourceKey<T> {

    private static final Map<String, ResourceKey<?>> VALUES = Collections.synchronizedMap(Maps.newHashMap()); // CraftBukkit - SPIGOT-6973: remove costly intern
    private final ResourceLocation registryName;
    private final ResourceLocation location;

    public static <T> Codec<ResourceKey<T>> codec(ResourceKey<? extends Registry<T>> registry) {
        return ResourceLocation.CODEC.xmap((minecraftkey) -> {
            return ResourceKey.create(registry, minecraftkey);
        }, ResourceKey::location);
    }

    public static <T> ResourceKey<T> create(ResourceKey<? extends Registry<T>> registry, ResourceLocation value) {
        return ResourceKey.create(registry.location, value);
    }

    public static <T> ResourceKey<Registry<T>> createRegistryKey(ResourceLocation registry) {
        return ResourceKey.create(Registry.ROOT_REGISTRY_NAME, registry);
    }

    private static <T> ResourceKey<T> create(ResourceLocation registry, ResourceLocation value) {
        String s = (registry + ":" + value); // CraftBukkit - SPIGOT-6973: remove costly intern

        return (ResourceKey) ResourceKey.VALUES.computeIfAbsent(s, (s1) -> {
            return new ResourceKey<>(registry, value);
        });
    }

    private ResourceKey(ResourceLocation registry, ResourceLocation value) {
        this.registryName = registry;
        this.location = value;
    }

    public String toString() {
        return "ResourceKey[" + this.registryName + " / " + this.location + "]";
    }

    public boolean isFor(ResourceKey<? extends Registry<?>> registry) {
        return this.registryName.equals(registry.location());
    }

    public <E> Optional<ResourceKey<E>> cast(ResourceKey<? extends Registry<E>> registryRef) {
        return this.isFor(registryRef) ? (Optional) Optional.of(this) : Optional.empty(); // CraftBukkit - decompile error
    }

    public ResourceLocation location() {
        return this.location;
    }

    public ResourceLocation registry() {
        return this.registryName;
    }

    public static <T> Function<ResourceLocation, ResourceKey<T>> elementKey(ResourceKey<? extends Registry<T>> registry) {
        return (minecraftkey) -> {
            return ResourceKey.create(registry, minecraftkey);
        };
    }
}
