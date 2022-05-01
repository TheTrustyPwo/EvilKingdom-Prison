package net.minecraft.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;

public class HolderSetCodec<E> implements Codec<HolderSet<E>> {
    private final ResourceKey<? extends Registry<E>> registryKey;
    private final Codec<Holder<E>> elementCodec;
    private final Codec<List<Holder<E>>> homogenousListCodec;
    private final Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;

    private static <E> Codec<List<Holder<E>>> homogenousList(Codec<Holder<E>> entryCodec, boolean alwaysSerializeAsList) {
        Function<List<Holder<E>>, DataResult<List<Holder<E>>>> function = ExtraCodecs.ensureHomogenous(Holder::kind);
        Codec<List<Holder<E>>> codec = entryCodec.listOf().flatXmap(function, function);
        return alwaysSerializeAsList ? codec : Codec.either(codec, entryCodec).xmap((either) -> {
            return either.map((entries) -> {
                return entries;
            }, List::of);
        }, (entries) -> {
            return entries.size() == 1 ? Either.right(entries.get(0)) : Either.left(entries);
        });
    }

    public static <E> Codec<HolderSet<E>> create(ResourceKey<? extends Registry<E>> registryRef, Codec<Holder<E>> entryCodec, boolean alwaysSerializeAsList) {
        return new HolderSetCodec<>(registryRef, entryCodec, alwaysSerializeAsList);
    }

    private HolderSetCodec(ResourceKey<? extends Registry<E>> registry, Codec<Holder<E>> entryCodec, boolean alwaysSerializeAsList) {
        this.registryKey = registry;
        this.elementCodec = entryCodec;
        this.homogenousListCodec = homogenousList(entryCodec, alwaysSerializeAsList);
        this.registryAwareCodec = Codec.either(TagKey.hashedCodec(registry), this.homogenousListCodec);
    }

    public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> dynamicOps, T object) {
        if (dynamicOps instanceof RegistryOps) {
            RegistryOps<T> registryOps = (RegistryOps)dynamicOps;
            Optional<? extends Registry<E>> optional = registryOps.registry(this.registryKey);
            if (optional.isPresent()) {
                Registry<E> registry = optional.get();
                return this.registryAwareCodec.decode(dynamicOps, object).map((pair) -> {
                    return pair.mapFirst((either) -> {
                        return either.map(registry::getOrCreateTag, HolderSet::direct);
                    });
                });
            }
        }

        return this.decodeWithoutRegistry(dynamicOps, object);
    }

    public <T> DataResult<T> encode(HolderSet<E> holderSet, DynamicOps<T> dynamicOps, T object) {
        if (dynamicOps instanceof RegistryOps) {
            RegistryOps<T> registryOps = (RegistryOps)dynamicOps;
            Optional<? extends Registry<E>> optional = registryOps.registry(this.registryKey);
            if (optional.isPresent()) {
                if (!holderSet.isValidInRegistry(optional.get())) {
                    return DataResult.error("HolderSet " + holderSet + " is not valid in current registry set");
                }

                return this.registryAwareCodec.encode(holderSet.unwrap().mapRight(List::copyOf), dynamicOps, object);
            }
        }

        return this.encodeWithoutRegistry(holderSet, dynamicOps, object);
    }

    private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(DynamicOps<T> ops, T input) {
        return this.elementCodec.listOf().decode(ops, input).flatMap((pair) -> {
            List<Holder.Direct<E>> list = new ArrayList<>();

            for(Holder<E> holder : pair.getFirst()) {
                if (!(holder instanceof Holder.Direct)) {
                    return DataResult.error("Can't decode element " + holder + " without registry");
                }

                Holder.Direct<E> direct = (Holder.Direct)holder;
                list.add(direct);
            }

            return DataResult.success(new Pair<>(HolderSet.direct(list), pair.getSecond()));
        });
    }

    private <T> DataResult<T> encodeWithoutRegistry(HolderSet<E> entryList, DynamicOps<T> ops, T prefix) {
        return this.homogenousListCodec.encode(entryList.stream().toList(), ops, prefix);
    }
}
