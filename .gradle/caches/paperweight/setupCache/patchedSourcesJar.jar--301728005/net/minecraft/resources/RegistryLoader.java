package net.minecraft.resources;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;

public class RegistryLoader {
    private final RegistryResourceAccess resources;
    private final Map<ResourceKey<? extends Registry<?>>, RegistryLoader.ReadCache<?>> readCache = new IdentityHashMap<>();

    RegistryLoader(RegistryResourceAccess entryLoader) {
        this.resources = entryLoader;
    }

    public <E> DataResult<? extends Registry<E>> overrideRegistryFromResources(WritableRegistry<E> registry, ResourceKey<? extends Registry<E>> registryRef, Codec<E> codec, DynamicOps<JsonElement> ops) {
        Collection<ResourceKey<E>> collection = this.resources.listResources(registryRef);
        DataResult<WritableRegistry<E>> dataResult = DataResult.success(registry, Lifecycle.stable());

        for(ResourceKey<E> resourceKey : collection) {
            dataResult = dataResult.flatMap((reg) -> {
                return this.overrideElementFromResources(reg, registryRef, codec, resourceKey, ops).map((entry) -> {
                    return reg;
                });
            });
        }

        return dataResult.setPartial(registry);
    }

    <E> DataResult<Holder<E>> overrideElementFromResources(WritableRegistry<E> registry, ResourceKey<? extends Registry<E>> registryRef, Codec<E> codec, ResourceKey<E> entryKey, DynamicOps<JsonElement> ops) {
        RegistryLoader.ReadCache<E> readCache = this.readCache(registryRef);
        DataResult<Holder<E>> dataResult = readCache.values.get(entryKey);
        if (dataResult != null) {
            // Paper start - register in registry due to craftbukkit running this 3 times instead of once
            if (registryRef == (ResourceKey) Registry.LEVEL_STEM_REGISTRY && dataResult.result().isPresent()) {
                // OptionalInt.empty() because the LevelStem registry is only loaded from the resource manager, not the InMemory resource access
                registry.registerOrOverride(java.util.OptionalInt.empty(), entryKey, dataResult.result().get().value(), dataResult.lifecycle());
            }
            // Paper end
            return dataResult;
        } else {
            Holder<E> holder = registry.getOrCreateHolder(entryKey);
            readCache.values.put(entryKey, DataResult.success(holder));
            Optional<DataResult<RegistryResourceAccess.ParsedEntry<E>>> optional = this.resources.parseElement(ops, registryRef, entryKey, codec);
            DataResult<Holder<E>> dataResult2;
            if (optional.isEmpty()) {
                if (registry.containsKey(entryKey)) {
                    dataResult2 = DataResult.success(holder, Lifecycle.stable());
                } else {
                    dataResult2 = DataResult.error("Missing referenced custom/removed registry entry for registry " + registryRef + " named " + entryKey.location());
                }
            } else {
                DataResult<RegistryResourceAccess.ParsedEntry<E>> dataResult4 = optional.get();
                Optional<RegistryResourceAccess.ParsedEntry<E>> optional2 = dataResult4.result();
                if (optional2.isPresent()) {
                    RegistryResourceAccess.ParsedEntry<E> parsedEntry = optional2.get();
                    registry.registerOrOverride(parsedEntry.fixedId(), entryKey, parsedEntry.value(), dataResult4.lifecycle());
                }

                dataResult2 = dataResult4.map((entry) -> {
                    return holder;
                });
            }

            readCache.values.put(entryKey, dataResult2);
            return dataResult2;
        }
    }

    private <E> RegistryLoader.ReadCache<E> readCache(ResourceKey<? extends Registry<E>> registryRef) {
        return (RegistryLoader.ReadCache<E>) this.readCache.computeIfAbsent(registryRef, (ref) -> { // Paper - decompile fix
            return new RegistryLoader.ReadCache();
        });
    }

    public RegistryLoader.Bound bind(RegistryAccess.Writable dynamicRegistryManager) {
        return new RegistryLoader.Bound(dynamicRegistryManager, this);
    }

    public static record Bound(RegistryAccess.Writable access, RegistryLoader loader) {
        public <E> DataResult<? extends Registry<E>> overrideRegistryFromResources(ResourceKey<? extends Registry<E>> registryRef, Codec<E> codec, DynamicOps<JsonElement> ops) {
            WritableRegistry<E> writableRegistry = this.access.ownedWritableRegistryOrThrow(registryRef);
            return this.loader.overrideRegistryFromResources(writableRegistry, registryRef, codec, ops);
        }

        public <E> DataResult<Holder<E>> overrideElementFromResources(ResourceKey<? extends Registry<E>> registryRef, Codec<E> codec, ResourceKey<E> entryKey, DynamicOps<JsonElement> ops) {
            WritableRegistry<E> writableRegistry = this.access.ownedWritableRegistryOrThrow(registryRef);
            return this.loader.overrideElementFromResources(writableRegistry, registryRef, codec, entryKey, ops);
        }
    }

    static final class ReadCache<E> {
        final Map<ResourceKey<E>, DataResult<Holder<E>>> values = Maps.newIdentityHashMap();
    }
}
