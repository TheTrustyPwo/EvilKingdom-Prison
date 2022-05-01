package net.minecraft.resources;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.DataResult.PartialResult;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public interface RegistryResourceAccess {
    <E> Collection<ResourceKey<E>> listResources(ResourceKey<? extends Registry<E>> key);

    <E> Optional<DataResult<RegistryResourceAccess.ParsedEntry<E>>> parseElement(DynamicOps<JsonElement> json, ResourceKey<? extends Registry<E>> registryId, ResourceKey<E> entryId, Decoder<E> decoder);

    static RegistryResourceAccess forResourceManager(ResourceManager resourceManager) {
        return new RegistryResourceAccess() {
            private static final String JSON = ".json";

            @Override
            public <E> Collection<ResourceKey<E>> listResources(ResourceKey<? extends Registry<E>> key) {
                String string = registryDirPath(key);
                Set<ResourceKey<E>> set = new HashSet<>();
                resourceManager.listResources(string, (name) -> {
                    return name.endsWith(".json");
                }).forEach((id) -> {
                    String string2 = id.getPath();
                    String string3 = string2.substring(string.length() + 1, string2.length() - ".json".length());
                    set.add(ResourceKey.create(key, new ResourceLocation(id.getNamespace(), string3)));
                });
                return set;
            }

            @Override
            public <E> Optional<DataResult<RegistryResourceAccess.ParsedEntry<E>>> parseElement(DynamicOps<JsonElement> json, ResourceKey<? extends Registry<E>> registryId, ResourceKey<E> entryId, Decoder<E> decoder) {
                ResourceLocation resourceLocation = elementPath(registryId, entryId);
                if (!resourceManager.hasResource(resourceLocation)) {
                    return Optional.empty();
                } else {
                    try {
                        Resource resource = resourceManager.getResource(resourceLocation);

                        Optional var9;
                        try {
                            Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);

                            try {
                                JsonElement jsonElement = JsonParser.parseReader(reader);
                                var9 = Optional.of(decoder.parse(json, jsonElement).map(RegistryResourceAccess.ParsedEntry::createWithoutId));
                            } catch (Throwable var12) {
                                try {
                                    reader.close();
                                } catch (Throwable var11) {
                                    var12.addSuppressed(var11);
                                }

                                throw var12;
                            }

                            reader.close();
                        } catch (Throwable var13) {
                            if (resource != null) {
                                try {
                                    resource.close();
                                } catch (Throwable var10) {
                                    var13.addSuppressed(var10);
                                }
                            }

                            throw var13;
                        }

                        if (resource != null) {
                            resource.close();
                        }

                        return var9;
                    } catch (JsonIOException | JsonSyntaxException | IOException var14) {
                        return Optional.of(DataResult.error("Failed to parse " + resourceLocation + " file: " + var14.getMessage()));
                    }
                }
            }

            private static String registryDirPath(ResourceKey<? extends Registry<?>> registryKey) {
                return registryKey.location().getPath();
            }

            private static <E> ResourceLocation elementPath(ResourceKey<? extends Registry<E>> rootKey, ResourceKey<E> key) {
                return new ResourceLocation(key.location().getNamespace(), registryDirPath(rootKey) + "/" + key.location().getPath() + ".json");
            }

            @Override
            public String toString() {
                return "ResourceAccess[" + resourceManager + "]";
            }
        };
    }

    public static final class InMemoryStorage implements RegistryResourceAccess {
        private static final Logger LOGGER = LogUtils.getLogger();
        private final Map<ResourceKey<?>, RegistryResourceAccess.InMemoryStorage.Entry> entries = Maps.newIdentityHashMap();

        public <E> void add(RegistryAccess registryManager, ResourceKey<E> key, Encoder<E> encoder, int rawId, E entry, Lifecycle lifecycle) {
            DataResult<JsonElement> dataResult = encoder.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registryManager), entry);
            Optional<PartialResult<JsonElement>> optional = dataResult.error();
            if (optional.isPresent()) {
                LOGGER.error("Error adding element: {}", (Object)optional.get().message());
            } else {
                this.entries.put(key, new RegistryResourceAccess.InMemoryStorage.Entry(dataResult.result().get(), rawId, lifecycle));
            }

        }

        @Override
        public <E> Collection<ResourceKey<E>> listResources(ResourceKey<? extends Registry<E>> key) {
            return this.entries.keySet().stream().flatMap((registryKey) -> {
                return registryKey.cast(key).stream();
            }).collect(Collectors.toList());
        }

        @Override
        public <E> Optional<DataResult<RegistryResourceAccess.ParsedEntry<E>>> parseElement(DynamicOps<JsonElement> json, ResourceKey<? extends Registry<E>> registryId, ResourceKey<E> entryId, Decoder<E> decoder) {
            RegistryResourceAccess.InMemoryStorage.Entry entry = this.entries.get(entryId);
            return entry == null ? Optional.of(DataResult.error("Unknown element: " + entryId)) : Optional.of(decoder.parse(json, entry.data).setLifecycle(entry.lifecycle).map((value) -> {
                return RegistryResourceAccess.ParsedEntry.createWithId(value, entry.id);
            }));
        }

        static record Entry(JsonElement data, int id, Lifecycle lifecycle) {
        }
    }

    public static record ParsedEntry<E>(E value, OptionalInt fixedId) {
        public static <E> RegistryResourceAccess.ParsedEntry<E> createWithoutId(E value) {
            return new RegistryResourceAccess.ParsedEntry<>(value, OptionalInt.empty());
        }

        public static <E> RegistryResourceAccess.ParsedEntry<E> createWithId(E value, int id) {
            return new RegistryResourceAccess.ParsedEntry<>(value, OptionalInt.of(id));
        }
    }
}
