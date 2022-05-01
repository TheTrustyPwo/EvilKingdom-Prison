package net.minecraft.tags;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class TagNetworkSerialization {
    public static Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> serializeTagsToNetwork(RegistryAccess dynamicRegistryManager) {
        return dynamicRegistryManager.networkSafeRegistries().map((registryEntry) -> {
            return Pair.of(registryEntry.key(), serializeToNetwork(registryEntry.value()));
        }).filter((pair) -> {
            return !pair.getSecond().isEmpty();
        }).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    private static <T> TagNetworkSerialization.NetworkPayload serializeToNetwork(Registry<T> registry) {
        Map<ResourceLocation, IntList> map = new HashMap<>();
        registry.getTags().forEach((pair) -> {
            HolderSet<T> holderSet = pair.getSecond();
            IntList intList = new IntArrayList(holderSet.size());

            for(Holder<T> holder : holderSet) {
                if (holder.kind() != Holder.Kind.REFERENCE) {
                    throw new IllegalStateException("Can't serialize unregistered value " + holder);
                }

                intList.add(registry.getId(holder.value()));
            }

            map.put(pair.getFirst().location(), intList);
        });
        return new TagNetworkSerialization.NetworkPayload(map);
    }

    public static <T> void deserializeTagsFromNetwork(ResourceKey<? extends Registry<T>> registryKey, Registry<T> registry, TagNetworkSerialization.NetworkPayload serialized, TagNetworkSerialization.TagOutput<T> loader) {
        serialized.tags.forEach((tagId, rawIds) -> {
            TagKey<T> tagKey = TagKey.create(registryKey, tagId);
            List<Holder<T>> list = rawIds.intStream().mapToObj(registry::getHolder).flatMap(Optional::stream).toList();
            loader.accept(tagKey, list);
        });
    }

    public static final class NetworkPayload {
        final Map<ResourceLocation, IntList> tags;

        NetworkPayload(Map<ResourceLocation, IntList> contents) {
            this.tags = contents;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeMap(this.tags, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeIntIdList);
        }

        public static TagNetworkSerialization.NetworkPayload read(FriendlyByteBuf buf) {
            return new TagNetworkSerialization.NetworkPayload(buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readIntIdList));
        }

        public boolean isEmpty() {
            return this.tags.isEmpty();
        }
    }

    @FunctionalInterface
    public interface TagOutput<T> {
        void accept(TagKey<T> tag, List<Holder<T>> entries);
    }
}
