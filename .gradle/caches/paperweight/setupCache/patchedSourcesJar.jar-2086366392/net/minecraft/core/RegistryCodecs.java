package net.minecraft.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.RegistryLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class RegistryCodecs {
    private static <T> MapCodec<RegistryCodecs.RegistryEntry<T>> withNameAndId(ResourceKey<? extends Registry<T>> registryRef, MapCodec<T> elementCodec) {
        return RecordCodecBuilder.mapCodec((instance) -> {
            return instance.group(ResourceLocation.CODEC.xmap(ResourceKey.elementKey(registryRef), ResourceKey::location).fieldOf("name").forGetter(RegistryCodecs.RegistryEntry::key), Codec.INT.fieldOf("id").forGetter(RegistryCodecs.RegistryEntry::id), elementCodec.forGetter(RegistryCodecs.RegistryEntry::value)).apply(instance, RegistryCodecs.RegistryEntry::new);
        });
    }

    public static <T> Codec<Registry<T>> networkCodec(ResourceKey<? extends Registry<T>> registryRef, Lifecycle lifecycle, Codec<T> elementCodec) {
        return withNameAndId(registryRef, elementCodec.fieldOf("element")).codec().listOf().xmap((entries) -> {
            WritableRegistry<T> writableRegistry = new MappedRegistry<>(registryRef, lifecycle, (Function<T, Holder.Reference<T>>)null);

            for(RegistryCodecs.RegistryEntry<T> registryEntry : entries) {
                writableRegistry.registerMapping(registryEntry.id(), registryEntry.key(), registryEntry.value(), lifecycle);
            }

            return writableRegistry;
        }, (registry) -> {
            Builder<RegistryCodecs.RegistryEntry<T>> builder = ImmutableList.builder();

            for(T object : registry) {
                builder.add(new RegistryCodecs.RegistryEntry<>(registry.getResourceKey(object).get(), registry.getId(object), object));
            }

            return builder.build();
        });
    }

    public static <E> Codec<Registry<E>> dataPackAwareCodec(ResourceKey<? extends Registry<E>> registryRef, Lifecycle lifecycle, Codec<E> elementCodec) {
        Codec<Map<ResourceKey<E>, E>> codec = directCodec(registryRef, elementCodec);
        Encoder<Registry<E>> encoder = codec.comap((registry) -> {
            return ImmutableMap.copyOf(registry.entrySet());
        });
        return Codec.of(encoder, dataPackAwareDecoder(registryRef, elementCodec, codec, lifecycle), "DataPackRegistryCodec for " + registryRef);
    }

    private static <E> Decoder<Registry<E>> dataPackAwareDecoder(ResourceKey<? extends Registry<E>> registryRef, Codec<E> codec, Decoder<Map<ResourceKey<E>, E>> entryMapDecoder, Lifecycle lifecycle) {
        final Decoder<WritableRegistry<E>> decoder = entryMapDecoder.map((map) -> {
            WritableRegistry<E> writableRegistry = new MappedRegistry<>(registryRef, lifecycle, (Function<E, Holder.Reference<E>>)null);
            map.forEach((key, value) -> {
                writableRegistry.register(key, value, lifecycle);
            });
            return writableRegistry;
        });
        return new Decoder<Registry<E>>() {
            public <T> DataResult<Pair<Registry<E>, T>> decode(DynamicOps<T> dynamicOps, T object) {
                DataResult<Pair<WritableRegistry<E>, T>> dataResult = decoder.decode(dynamicOps, object);
                if (dynamicOps instanceof RegistryOps) {
                    RegistryOps<?> registryOps = (RegistryOps)dynamicOps;
                    return registryOps.registryLoader().map((loaderAccess) -> {
                        return this.overrideFromResources(dataResult, registryOps, loaderAccess.loader());
                    }).orElseGet(() -> {
                        return DataResult.error("Can't load registry with this ops");
                    });
                } else {
                    return dataResult.map((pair) -> {
                        return pair.mapFirst((registry) -> {
                            return registry;
                        });
                    });
                }
            }

            private <T> DataResult<Pair<Registry<E>, T>> overrideFromResources(DataResult<Pair<WritableRegistry<E>, T>> result, RegistryOps<?> ops, RegistryLoader loader) {
                return result.flatMap((pair) -> {
                    return loader.overrideRegistryFromResources(pair.getFirst(), registryRef, codec, ops.getAsJson()).map((registry) -> {
                        return Pair.of(registry, (T)pair.getSecond());
                    });
                });
            }
        };
    }

    private static <T> Codec<Map<ResourceKey<T>, T>> directCodec(ResourceKey<? extends Registry<T>> registryRef, Codec<T> elementCodec) {
        return Codec.unboundedMap(ResourceLocation.CODEC.xmap(ResourceKey.elementKey(registryRef), ResourceKey::location), elementCodec);
    }

    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> registryRef, Codec<E> elementCodec) {
        return homogeneousList(registryRef, elementCodec, false);
    }

    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> registryRef, Codec<E> elementCodec, boolean alwaysSerializeAsList) {
        return HolderSetCodec.create(registryRef, RegistryFileCodec.create(registryRef, elementCodec), alwaysSerializeAsList);
    }

    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> registryRef) {
        return homogeneousList(registryRef, false);
    }

    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> registryRef, boolean alwaysSerializeAsList) {
        return HolderSetCodec.create(registryRef, RegistryFixedCodec.create(registryRef), alwaysSerializeAsList);
    }

    static record RegistryEntry<T>(ResourceKey<T> key, int id, T value) {
    }
}
