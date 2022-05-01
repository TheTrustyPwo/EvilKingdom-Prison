package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;

public final class RegistryFixedCodec<E> implements Codec<Holder<E>> {
    private final ResourceKey<? extends Registry<E>> registryKey;

    public static <E> RegistryFixedCodec<E> create(ResourceKey<? extends Registry<E>> registry) {
        return new RegistryFixedCodec<>(registry);
    }

    private RegistryFixedCodec(ResourceKey<? extends Registry<E>> registry) {
        this.registryKey = registry;
    }

    public <T> DataResult<T> encode(Holder<E> holder, DynamicOps<T> dynamicOps, T object) {
        if (dynamicOps instanceof RegistryOps) {
            RegistryOps<?> registryOps = (RegistryOps)dynamicOps;
            Optional<? extends Registry<E>> optional = registryOps.registry(this.registryKey);
            if (optional.isPresent()) {
                if (!holder.isValidInRegistry(optional.get())) {
                    return DataResult.error("Element " + holder + " is not valid in current registry set");
                }

                return holder.unwrap().map((registryKey) -> {
                    return ResourceLocation.CODEC.encode(registryKey.location(), dynamicOps, object);
                }, (value) -> {
                    return DataResult.error("Elements from registry " + this.registryKey + " can't be serialized to a value");
                });
            }
        }

        return DataResult.error("Can't access registry " + this.registryKey);
    }

    public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> dynamicOps, T object) {
        if (dynamicOps instanceof RegistryOps) {
            RegistryOps<?> registryOps = (RegistryOps)dynamicOps;
            Optional<? extends Registry<E>> optional = registryOps.registry(this.registryKey);
            if (optional.isPresent()) {
                return ResourceLocation.CODEC.decode(dynamicOps, object).map((pair) -> {
                    return pair.mapFirst((value) -> {
                        return optional.get().getOrCreateHolder(ResourceKey.create(this.registryKey, value));
                    });
                });
            }
        }

        return DataResult.error("Can't access registry " + this.registryKey);
    }

    @Override
    public String toString() {
        return "RegistryFixedCodec[" + this.registryKey + "]";
    }
}
