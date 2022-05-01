package net.minecraft.tags;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public record TagKey<T>(ResourceKey<? extends Registry<T>> registry, ResourceLocation location) {
    private static final Interner<TagKey<?>> VALUES = Interners.newStrongInterner();

    public static <T> Codec<TagKey<T>> codec(ResourceKey<? extends Registry<T>> registry) {
        return ResourceLocation.CODEC.xmap((id) -> {
            return create(registry, id);
        }, TagKey::location);
    }

    public static <T> Codec<TagKey<T>> hashedCodec(ResourceKey<? extends Registry<T>> registry) {
        return Codec.STRING.comapFlatMap((string) -> {
            return string.startsWith("#") ? ResourceLocation.read(string.substring(1)).map((id) -> {
                return create(registry, id);
            }) : DataResult.error("Not a tag id");
        }, (string) -> {
            return "#" + string.location;
        });
    }

    public static <T> TagKey<T> create(ResourceKey<? extends Registry<T>> registry, ResourceLocation id) {
        return VALUES.intern(new TagKey<>(registry, id));
    }

    public boolean isFor(ResourceKey<? extends Registry<?>> registryRef) {
        return this.registry == registryRef;
    }

    public <E> Optional<TagKey<E>> cast(ResourceKey<? extends Registry<E>> registryRef) {
        return this.isFor(registryRef) ? Optional.of(this) : Optional.empty();
    }

    @Override
    public String toString() {
        return "TagKey[" + this.registry.location() + " / " + this.location + "]";
    }
}
