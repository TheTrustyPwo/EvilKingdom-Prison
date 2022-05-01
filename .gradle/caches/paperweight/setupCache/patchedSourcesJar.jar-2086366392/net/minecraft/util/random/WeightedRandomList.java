package net.minecraft.util.random;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class WeightedRandomList<E extends WeightedEntry> {
    private final int totalWeight;
    private final ImmutableList<E> items;

    WeightedRandomList(List<? extends E> entries) {
        this.items = ImmutableList.copyOf(entries);
        this.totalWeight = WeightedRandom.getTotalWeight(entries);
    }

    public static <E extends WeightedEntry> WeightedRandomList<E> create() {
        return new WeightedRandomList<>(ImmutableList.of());
    }

    @SafeVarargs
    public static <E extends WeightedEntry> WeightedRandomList<E> create(E... entries) {
        return new WeightedRandomList<>(ImmutableList.copyOf(entries));
    }

    public static <E extends WeightedEntry> WeightedRandomList<E> create(List<E> entries) {
        return new WeightedRandomList<>(entries);
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public Optional<E> getRandom(Random random) {
        if (this.totalWeight == 0) {
            return Optional.empty();
        } else {
            int i = random.nextInt(this.totalWeight);
            return WeightedRandom.getWeightedItem(this.items, i);
        }
    }

    public List<E> unwrap() {
        return this.items;
    }

    public static <E extends WeightedEntry> Codec<WeightedRandomList<E>> codec(Codec<E> entryCodec) {
        return entryCodec.listOf().xmap(WeightedRandomList::create, WeightedRandomList::unwrap);
    }
}
