package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.function.Function;

public record InclusiveRange<T extends Comparable<T>>(T minInclusive, T maxInclusive) {
    public static final Codec<InclusiveRange<Integer>> INT = codec(Codec.INT);

    public InclusiveRange {
        if (minInclusive.compareTo(maxInclusive) > 0) {
            throw new IllegalArgumentException("min_inclusive must be less than or equal to max_inclusive");
        }
    }

    public static <T extends Comparable<T>> Codec<InclusiveRange<T>> codec(Codec<T> elementCodec) {
        return ExtraCodecs.intervalCodec(elementCodec, "min_inclusive", "max_inclusive", InclusiveRange::create, InclusiveRange::minInclusive, InclusiveRange::maxInclusive);
    }

    public static <T extends Comparable<T>> Codec<InclusiveRange<T>> codec(Codec<T> codec, T minInclusive, T maxInclusive) {
        Function<InclusiveRange<T>, DataResult<InclusiveRange<T>>> function = (range) -> {
            if (range.minInclusive().compareTo(minInclusive) < 0) {
                return DataResult.error("Range limit too low, expected at least " + minInclusive + " [" + range.minInclusive() + "-" + range.maxInclusive() + "]");
            } else {
                return range.maxInclusive().compareTo(maxInclusive) > 0 ? DataResult.error("Range limit too high, expected at most " + maxInclusive + " [" + range.minInclusive() + "-" + range.maxInclusive() + "]") : DataResult.success(range);
            }
        };
        return codec(codec).flatXmap(function, function);
    }

    public static <T extends Comparable<T>> DataResult<InclusiveRange<T>> create(T minInclusive, T maxInclusive) {
        return minInclusive.compareTo(maxInclusive) <= 0 ? DataResult.success(new InclusiveRange<>(minInclusive, maxInclusive)) : DataResult.error("min_inclusive must be less than or equal to max_inclusive");
    }

    public boolean isValueInRange(T value) {
        return value.compareTo(this.minInclusive) >= 0 && value.compareTo(this.maxInclusive) <= 0;
    }

    public boolean contains(InclusiveRange<T> other) {
        return other.minInclusive().compareTo(this.minInclusive) >= 0 && other.maxInclusive.compareTo(this.maxInclusive) <= 0;
    }

    @Override
    public String toString() {
        return "[" + this.minInclusive + ", " + this.maxInclusive + "]";
    }
}
