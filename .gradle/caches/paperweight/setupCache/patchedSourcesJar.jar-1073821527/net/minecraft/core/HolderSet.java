package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HolderSet<T> extends Iterable<Holder<T>> {
    Stream<Holder<T>> stream();

    int size();

    Either<TagKey<T>, List<Holder<T>>> unwrap();

    Optional<Holder<T>> getRandomElement(Random random);

    Holder<T> get(int index);

    boolean contains(Holder<T> entry);

    boolean isValidInRegistry(Registry<T> registry);

    @SafeVarargs
    static <T> HolderSet.Direct<T> direct(Holder<T>... entries) {
        return new HolderSet.Direct<>(List.of(entries));
    }

    static <T> HolderSet.Direct<T> direct(List<? extends Holder<T>> entries) {
        return new HolderSet.Direct<>(List.copyOf(entries));
    }

    @SafeVarargs
    static <E, T> HolderSet.Direct<T> direct(Function<E, Holder<T>> mapper, E... values) {
        return direct(Stream.of(values).map(mapper).toList());
    }

    static <E, T> HolderSet.Direct<T> direct(Function<E, Holder<T>> mapper, List<E> values) {
        return direct(values.stream().map(mapper).toList());
    }

    public static class Direct<T> extends HolderSet.ListBacked<T> {
        private final List<Holder<T>> contents;
        @Nullable
        private Set<Holder<T>> contentsSet;

        Direct(List<Holder<T>> entries) {
            this.contents = entries;
        }

        @Override
        protected List<Holder<T>> contents() {
            return this.contents;
        }

        @Override
        public Either<TagKey<T>, List<Holder<T>>> unwrap() {
            return Either.right(this.contents);
        }

        @Override
        public boolean contains(Holder<T> entry) {
            if (this.contentsSet == null) {
                this.contentsSet = Set.copyOf(this.contents);
            }

            return this.contentsSet.contains(entry);
        }

        @Override
        public String toString() {
            return "DirectSet[" + this.contents + "]";
        }
    }

    public abstract static class ListBacked<T> implements HolderSet<T> {
        protected abstract List<Holder<T>> contents();

        @Override
        public int size() {
            return this.contents().size();
        }

        @Override
        public Spliterator<Holder<T>> spliterator() {
            return this.contents().spliterator();
        }

        @NotNull
        @Override
        public Iterator<Holder<T>> iterator() {
            return this.contents().iterator();
        }

        @Override
        public Stream<Holder<T>> stream() {
            return this.contents().stream();
        }

        @Override
        public Optional<Holder<T>> getRandomElement(Random random) {
            return Util.getRandomSafe(this.contents(), random);
        }

        @Override
        public Holder<T> get(int index) {
            return this.contents().get(index);
        }

        @Override
        public boolean isValidInRegistry(Registry<T> registry) {
            return true;
        }
    }

    public static class Named<T> extends HolderSet.ListBacked<T> {
        private final Registry<T> registry;
        private final TagKey<T> key;
        private List<Holder<T>> contents = List.of();

        Named(Registry<T> registry, TagKey<T> tag) {
            this.registry = registry;
            this.key = tag;
        }

        void bind(List<Holder<T>> entries) {
            this.contents = List.copyOf(entries);
        }

        public TagKey<T> key() {
            return this.key;
        }

        @Override
        protected List<Holder<T>> contents() {
            return this.contents;
        }

        @Override
        public Either<TagKey<T>, List<Holder<T>>> unwrap() {
            return Either.left(this.key);
        }

        @Override
        public boolean contains(Holder<T> entry) {
            return entry.is(this.key);
        }

        @Override
        public String toString() {
            return "NamedSet(" + this.key + ")[" + this.contents + "]";
        }

        @Override
        public boolean isValidInRegistry(Registry<T> registry) {
            return this.registry == registry;
        }
    }
}
