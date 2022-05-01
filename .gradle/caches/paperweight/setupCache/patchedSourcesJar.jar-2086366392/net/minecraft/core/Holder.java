package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public interface Holder<T> {
    T value();

    boolean isBound();

    boolean is(ResourceLocation id);

    boolean is(ResourceKey<T> key);

    boolean is(Predicate<ResourceKey<T>> predicate);

    boolean is(TagKey<T> tag);

    Stream<TagKey<T>> tags();

    Either<ResourceKey<T>, T> unwrap();

    Optional<ResourceKey<T>> unwrapKey();

    Holder.Kind kind();

    boolean isValidInRegistry(Registry<T> registry);

    static <T> Holder<T> direct(T value) {
        return new Holder.Direct<>(value);
    }

    static <T> Holder<T> hackyErase(Holder<? extends T> entry) {
        return entry;
    }

    public static record Direct<T>(T value) implements Holder<T> {
        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public boolean is(ResourceLocation id) {
            return false;
        }

        @Override
        public boolean is(ResourceKey<T> key) {
            return false;
        }

        @Override
        public boolean is(TagKey<T> tag) {
            return false;
        }

        @Override
        public boolean is(Predicate<ResourceKey<T>> predicate) {
            return false;
        }

        @Override
        public Either<ResourceKey<T>, T> unwrap() {
            return Either.right(this.value);
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.empty();
        }

        @Override
        public Holder.Kind kind() {
            return Holder.Kind.DIRECT;
        }

        @Override
        public String toString() {
            return "Direct{" + this.value + "}";
        }

        @Override
        public boolean isValidInRegistry(Registry<T> registry) {
            return true;
        }

        @Override
        public Stream<TagKey<T>> tags() {
            return Stream.of();
        }
    }

    public static enum Kind {
        REFERENCE,
        DIRECT;
    }

    public static class Reference<T> implements Holder<T> {
        private final Registry<T> registry;
        private Set<TagKey<T>> tags = Set.of();
        private final Holder.Reference.Type type;
        @Nullable
        private ResourceKey<T> key;
        @Nullable
        private T value;

        private Reference(Holder.Reference.Type referenceType, Registry<T> registry, @Nullable ResourceKey<T> registryKey, @Nullable T value) {
            this.registry = registry;
            this.type = referenceType;
            this.key = registryKey;
            this.value = value;
        }

        public static <T> Holder.Reference<T> createStandAlone(Registry<T> registry, ResourceKey<T> registryKey) {
            return new Holder.Reference<>(Holder.Reference.Type.STAND_ALONE, registry, registryKey, (T)null);
        }

        /** @deprecated */
        @Deprecated
        public static <T> Holder.Reference<T> createIntrusive(Registry<T> registry, @Nullable T registryKey) {
            return new Holder.Reference<>(Holder.Reference.Type.INTRUSIVE, registry, (ResourceKey<T>)null, registryKey);
        }

        public ResourceKey<T> key() {
            if (this.key == null) {
                throw new IllegalStateException("Trying to access unbound value '" + this.value + "' from registry " + this.registry);
            } else {
                return this.key;
            }
        }

        @Override
        public T value() {
            if (this.value == null) {
                throw new IllegalStateException("Trying to access unbound value '" + this.key + "' from registry " + this.registry);
            } else {
                return this.value;
            }
        }

        @Override
        public boolean is(ResourceLocation id) {
            return this.key().location().equals(id);
        }

        @Override
        public boolean is(ResourceKey<T> key) {
            return this.key() == key;
        }

        @Override
        public boolean is(TagKey<T> tag) {
            return this.tags.contains(tag);
        }

        @Override
        public boolean is(Predicate<ResourceKey<T>> predicate) {
            return predicate.test(this.key());
        }

        @Override
        public boolean isValidInRegistry(Registry<T> registry) {
            return this.registry == registry;
        }

        @Override
        public Either<ResourceKey<T>, T> unwrap() {
            return Either.left(this.key());
        }

        @Override
        public Optional<ResourceKey<T>> unwrapKey() {
            return Optional.of(this.key());
        }

        @Override
        public Holder.Kind kind() {
            return Holder.Kind.REFERENCE;
        }

        @Override
        public boolean isBound() {
            return this.key != null && this.value != null;
        }

        void bind(ResourceKey<T> key, T value) {
            if (this.key != null && key != this.key) {
                throw new IllegalStateException("Can't change holder key: existing=" + this.key + ", new=" + key);
            } else if (this.type == Holder.Reference.Type.INTRUSIVE && this.value != value) {
                throw new IllegalStateException("Can't change holder " + key + " value: existing=" + this.value + ", new=" + value);
            } else {
                this.key = key;
                this.value = value;
            }
        }

        void bindTags(Collection<TagKey<T>> tags) {
            this.tags = Set.copyOf(tags);
        }

        @Override
        public Stream<TagKey<T>> tags() {
            return this.tags.stream();
        }

        @Override
        public String toString() {
            return "Reference{" + this.key + "=" + this.value + "}";
        }

        static enum Type {
            STAND_ALONE,
            INTRUSIVE;
        }
    }
}
