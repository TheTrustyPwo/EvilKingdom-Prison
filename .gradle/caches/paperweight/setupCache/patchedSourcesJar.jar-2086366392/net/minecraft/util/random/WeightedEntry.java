package net.minecraft.util.random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public interface WeightedEntry {
    Weight getWeight();

    static <T> WeightedEntry.Wrapper<T> wrap(T data, int weight) {
        return new WeightedEntry.Wrapper<>(data, Weight.of(weight));
    }

    public static class IntrusiveBase implements WeightedEntry {
        private final Weight weight;

        public IntrusiveBase(int weight) {
            this.weight = Weight.of(weight);
        }

        public IntrusiveBase(Weight weight) {
            this.weight = weight;
        }

        @Override
        public Weight getWeight() {
            return this.weight;
        }
    }

    public static class Wrapper<T> implements WeightedEntry {
        private final T data;
        private final Weight weight;

        Wrapper(T data, Weight weight) {
            this.data = data;
            this.weight = weight;
        }

        public T getData() {
            return this.data;
        }

        @Override
        public Weight getWeight() {
            return this.weight;
        }

        public static <E> Codec<WeightedEntry.Wrapper<E>> codec(Codec<E> dataCodec) {
            return RecordCodecBuilder.create((instance) -> {
                return instance.group(dataCodec.fieldOf("data").forGetter(WeightedEntry.Wrapper::getData), Weight.CODEC.fieldOf("weight").forGetter(WeightedEntry.Wrapper::getWeight)).apply(instance, WeightedEntry.Wrapper::new);
            });
        }
    }
}
