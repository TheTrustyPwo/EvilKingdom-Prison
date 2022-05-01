package net.minecraft.core;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.RegistryLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.RegistryResourceAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.slf4j.Logger;

public interface RegistryAccess {
    Logger LOGGER = LogUtils.getLogger();
    Map<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> REGISTRIES = Util.make(() -> {
        Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> builder = ImmutableMap.builder();
        put(builder, Registry.DIMENSION_TYPE_REGISTRY, DimensionType.DIRECT_CODEC, DimensionType.DIRECT_CODEC);
        put(builder, Registry.BIOME_REGISTRY, Biome.DIRECT_CODEC, Biome.NETWORK_CODEC);
        put(builder, Registry.CONFIGURED_CARVER_REGISTRY, ConfiguredWorldCarver.DIRECT_CODEC);
        put(builder, Registry.CONFIGURED_FEATURE_REGISTRY, ConfiguredFeature.DIRECT_CODEC);
        put(builder, Registry.PLACED_FEATURE_REGISTRY, PlacedFeature.DIRECT_CODEC);
        put(builder, Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, ConfiguredStructureFeature.DIRECT_CODEC);
        put(builder, Registry.STRUCTURE_SET_REGISTRY, StructureSet.DIRECT_CODEC);
        put(builder, Registry.PROCESSOR_LIST_REGISTRY, StructureProcessorType.DIRECT_CODEC);
        put(builder, Registry.TEMPLATE_POOL_REGISTRY, StructureTemplatePool.DIRECT_CODEC);
        put(builder, Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, NoiseGeneratorSettings.DIRECT_CODEC);
        put(builder, Registry.NOISE_REGISTRY, NormalNoise.NoiseParameters.DIRECT_CODEC);
        put(builder, Registry.DENSITY_FUNCTION_REGISTRY, DensityFunction.DIRECT_CODEC);
        return builder.build();
    });
    Codec<RegistryAccess> NETWORK_CODEC = makeNetworkCodec();
    Supplier<RegistryAccess.Frozen> BUILTIN = Suppliers.memoize(() -> {
        return builtinCopy().freeze();
    });

    <E> Optional<Registry<E>> ownedRegistry(ResourceKey<? extends Registry<? extends E>> key);

    default <E> Registry<E> ownedRegistryOrThrow(ResourceKey<? extends Registry<? extends E>> key) {
        return this.ownedRegistry(key).orElseThrow(() -> {
            return new IllegalStateException("Missing registry: " + key);
        });
    }

    default <E> Optional<? extends Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> key) {
        Optional<? extends Registry<E>> optional = this.ownedRegistry(key);
        return optional.isPresent() ? optional : Registry.REGISTRY.getOptional(key.location());
    }

    default <E> Registry<E> registryOrThrow(ResourceKey<? extends Registry<? extends E>> key) {
        return this.registry(key).orElseThrow(() -> {
            return new IllegalStateException("Missing registry: " + key);
        });
    }

    private static <E> void put(Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> infosBuilder, ResourceKey<? extends Registry<E>> registryRef, Codec<E> entryCodec) {
        infosBuilder.put(registryRef, new RegistryAccess.RegistryData<>(registryRef, entryCodec, (Codec<E>)null));
    }

    private static <E> void put(Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> infosBuilder, ResourceKey<? extends Registry<E>> registryRef, Codec<E> entryCodec, Codec<E> networkEntryCodec) {
        infosBuilder.put(registryRef, new RegistryAccess.RegistryData<>(registryRef, entryCodec, networkEntryCodec));
    }

    static Iterable<RegistryAccess.RegistryData<?>> knownRegistries() {
        return REGISTRIES.values();
    }

    Stream<RegistryAccess.RegistryEntry<?>> ownedRegistries();

    private static Stream<RegistryAccess.RegistryEntry<Object>> globalRegistries() {
        return Registry.REGISTRY.holders().map(RegistryAccess.RegistryEntry::fromHolder);
    }

    default Stream<RegistryAccess.RegistryEntry<?>> registries() {
        return Stream.concat(this.ownedRegistries(), globalRegistries());
    }

    default Stream<RegistryAccess.RegistryEntry<?>> networkSafeRegistries() {
        return Stream.concat(this.ownedNetworkableRegistries(), globalRegistries());
    }

    private static <E> Codec<RegistryAccess> makeNetworkCodec() {
        Codec<ResourceKey<? extends Registry<E>>> codec = ResourceLocation.CODEC.xmap(ResourceKey::createRegistryKey, ResourceKey::location);
        Codec<Registry<E>> codec2 = codec.partialDispatch("type", (registry) -> {
            return DataResult.success(registry.key());
        }, (registryRef) -> {
            return getNetworkCodec(registryRef).map((codec) -> {
                return RegistryCodecs.networkCodec(registryRef, Lifecycle.experimental(), codec);
            });
        });
        UnboundedMapCodec<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> unboundedMapCodec = Codec.unboundedMap(codec, codec2);
        return captureMap(unboundedMapCodec);
    }

    private static <K extends ResourceKey<? extends Registry<?>>, V extends Registry<?>> Codec<RegistryAccess> captureMap(UnboundedMapCodec<K, V> originalCodec) {
        return originalCodec.xmap(RegistryAccess.ImmutableRegistryAccess::new, (dynamicRegistryManager) -> {
            return dynamicRegistryManager.ownedNetworkableRegistries().collect(ImmutableMap.toImmutableMap((entry) -> {
                return entry.key();
            }, (entry) -> {
                return entry.value();
            }));
        });
    }

    private Stream<RegistryAccess.RegistryEntry<?>> ownedNetworkableRegistries() {
        return this.ownedRegistries().filter((entry) -> {
            return REGISTRIES.get(entry.key).sendToClient();
        });
    }

    private static <E> DataResult<? extends Codec<E>> getNetworkCodec(ResourceKey<? extends Registry<E>> registryKey) {
        return Optional.ofNullable(REGISTRIES.get(registryKey)).map((info) -> {
            return info.networkCodec();
        }).map(DataResult::success).orElseGet(() -> {
            return DataResult.error("Unknown or not serializable registry: " + registryKey);
        });
    }

    private static Map<ResourceKey<? extends Registry<?>>, ? extends WritableRegistry<?>> createFreshRegistries() {
        return REGISTRIES.keySet().stream().collect(Collectors.toMap(Function.identity(), RegistryAccess::createRegistry));
    }

    private static RegistryAccess.Writable blankWriteable() {
        return new RegistryAccess.WritableRegistryAccess(createFreshRegistries());
    }

    static RegistryAccess.Frozen fromRegistryOfRegistries(Registry<? extends Registry<?>> registries) {
        return new RegistryAccess.Frozen() {
            @Override
            public <T> Optional<Registry<T>> ownedRegistry(ResourceKey<? extends Registry<? extends T>> key) {
                Registry<Registry<T>> registry = registries;
                return registry.getOptional(key);
            }

            @Override
            public Stream<RegistryAccess.RegistryEntry<?>> ownedRegistries() {
                return registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
            }
        };
    }

    static RegistryAccess.Writable builtinCopy() {
        RegistryAccess.Writable writable = blankWriteable();
        RegistryResourceAccess.InMemoryStorage inMemoryStorage = new RegistryResourceAccess.InMemoryStorage();

        for(Entry<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> entry : REGISTRIES.entrySet()) {
            if (!entry.getKey().equals(Registry.DIMENSION_TYPE_REGISTRY)) {
                addBuiltinElements(inMemoryStorage, entry.getValue());
            }
        }

        RegistryOps.createAndLoad(JsonOps.INSTANCE, writable, inMemoryStorage);
        return DimensionType.registerBuiltin(writable);
    }

    private static <E> void addBuiltinElements(RegistryResourceAccess.InMemoryStorage entryLoader, RegistryAccess.RegistryData<E> info) {
        ResourceKey<? extends Registry<E>> resourceKey = info.key();
        Registry<E> registry = BuiltinRegistries.ACCESS.registryOrThrow(resourceKey);

        for(Entry<ResourceKey<E>, E> entry : registry.entrySet()) {
            ResourceKey<E> resourceKey2 = entry.getKey();
            E object = entry.getValue();
            entryLoader.add(BuiltinRegistries.ACCESS, resourceKey2, info.codec(), registry.getId(object), object, registry.lifecycle(object));
        }

    }

    static void load(RegistryAccess.Writable dynamicRegistryManager, DynamicOps<JsonElement> ops, RegistryLoader registryLoader) {
        RegistryLoader.Bound bound = registryLoader.bind(dynamicRegistryManager);

        for(RegistryAccess.RegistryData<?> registryData : REGISTRIES.values()) {
            readRegistry(ops, bound, registryData);
        }

    }

    private static <E> void readRegistry(DynamicOps<JsonElement> ops, RegistryLoader.Bound loaderAccess, RegistryAccess.RegistryData<E> info) {
        DataResult<? extends Registry<E>> dataResult = loaderAccess.overrideRegistryFromResources(info.key(), info.codec(), ops);
        dataResult.error().ifPresent((partialResult) -> {
            throw new JsonParseException("Error loading registry data: " + partialResult.message());
        });
    }

    static RegistryAccess readFromDisk(Dynamic<?> dynamic) {
        return new RegistryAccess.ImmutableRegistryAccess(REGISTRIES.keySet().stream().collect(Collectors.toMap(Function.identity(), (registryRef) -> {
            return retrieveRegistry(registryRef, dynamic);
        })));
    }

    static <E> Registry<E> retrieveRegistry(ResourceKey<? extends Registry<? extends E>> registryRef, Dynamic<?> dynamic) {
        return RegistryOps.retrieveRegistry(registryRef).codec().parse(dynamic).resultOrPartial(Util.prefix(registryRef + " registry: ", LOGGER::error)).orElseThrow(() -> {
            return new IllegalStateException("Failed to get " + registryRef + " registry");
        });
    }

    static <E> WritableRegistry<?> createRegistry(ResourceKey<? extends Registry<?>> registryRef) {
        return new MappedRegistry<>(registryRef, Lifecycle.stable(), (Function<?, Holder.Reference<?>>)null);
    }

    default RegistryAccess.Frozen freeze() {
        return new RegistryAccess.ImmutableRegistryAccess(this.ownedRegistries().map(RegistryAccess.RegistryEntry::freeze));
    }

    default Lifecycle allElementsLifecycle() {
        return this.ownedRegistries().map((entry) -> {
            return entry.value.elementsLifecycle();
        }).reduce(Lifecycle.stable(), Lifecycle::add);
    }

    public interface Frozen extends RegistryAccess {
        @Override
        default RegistryAccess.Frozen freeze() {
            return this;
        }
    }

    public static final class ImmutableRegistryAccess implements RegistryAccess.Frozen {
        private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries;

        public ImmutableRegistryAccess(Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries) {
            this.registries = Map.copyOf(registries);
        }

        ImmutableRegistryAccess(Stream<RegistryAccess.RegistryEntry<?>> stream) {
            this.registries = stream.collect(ImmutableMap.toImmutableMap(RegistryAccess.RegistryEntry::key, RegistryAccess.RegistryEntry::value));
        }

        @Override
        public <E> Optional<Registry<E>> ownedRegistry(ResourceKey<? extends Registry<? extends E>> key) {
            return Optional.ofNullable(this.registries.get(key)).map((registry) -> {
                return registry;
            });
        }

        @Override
        public Stream<RegistryAccess.RegistryEntry<?>> ownedRegistries() {
            return this.registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
        }
    }

    public static record RegistryData<E>(ResourceKey<? extends Registry<E>> key, Codec<E> codec, @Nullable Codec<E> networkCodec) {
        public boolean sendToClient() {
            return this.networkCodec != null;
        }
    }

    public static record RegistryEntry<T>(ResourceKey<? extends Registry<T>> key, Registry<T> value) {
        private static <T, R extends Registry<? extends T>> RegistryAccess.RegistryEntry<T> fromMapEntry(Entry<? extends ResourceKey<? extends Registry<?>>, R> entry) {
            return fromUntyped(entry.getKey(), entry.getValue());
        }

        private static <T> RegistryAccess.RegistryEntry<T> fromHolder(Holder.Reference<? extends Registry<? extends T>> entry) {
            return fromUntyped(entry.key(), entry.value());
        }

        private static <T> RegistryAccess.RegistryEntry<T> fromUntyped(ResourceKey<? extends Registry<?>> key, Registry<?> value) {
            return new RegistryAccess.RegistryEntry<>(key, value);
        }

        private RegistryAccess.RegistryEntry<T> freeze() {
            return new RegistryAccess.RegistryEntry<>(this.key, this.value.freeze());
        }
    }

    public interface Writable extends RegistryAccess {
        <E> Optional<WritableRegistry<E>> ownedWritableRegistry(ResourceKey<? extends Registry<? extends E>> key);

        default <E> WritableRegistry<E> ownedWritableRegistryOrThrow(ResourceKey<? extends Registry<? extends E>> key) {
            return this.<E>ownedWritableRegistry(key).orElseThrow(() -> {
                return new IllegalStateException("Missing registry: " + key);
            });
        }
    }

    public static final class WritableRegistryAccess implements RegistryAccess.Writable {
        private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends WritableRegistry<?>> registries;

        WritableRegistryAccess(Map<? extends ResourceKey<? extends Registry<?>>, ? extends WritableRegistry<?>> mutableRegistries) {
            this.registries = mutableRegistries;
        }

        @Override
        public <E> Optional<Registry<E>> ownedRegistry(ResourceKey<? extends Registry<? extends E>> key) {
            return Optional.ofNullable(this.registries.get(key)).map((registry) -> {
                return registry;
            });
        }

        @Override
        public <E> Optional<WritableRegistry<E>> ownedWritableRegistry(ResourceKey<? extends Registry<? extends E>> key) {
            return Optional.ofNullable(this.registries.get(key)).map((registry) -> {
                return registry;
            });
        }

        @Override
        public Stream<RegistryAccess.RegistryEntry<?>> ownedRegistries() {
            return this.registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
        }
    }
}
