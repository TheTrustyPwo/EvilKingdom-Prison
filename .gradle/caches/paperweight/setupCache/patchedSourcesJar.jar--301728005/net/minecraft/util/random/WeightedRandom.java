package net.minecraft.util.random;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.Util;

public class WeightedRandom {
    private WeightedRandom() {
    }

    public static int getTotalWeight(List<? extends WeightedEntry> pool) {
        long l = 0L;

        for(WeightedEntry weightedEntry : pool) {
            l += (long)weightedEntry.getWeight().asInt();
        }

        if (l > 2147483647L) {
            throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
        } else {
            return (int)l;
        }
    }

    public static <T extends WeightedEntry> Optional<T> getRandomItem(Random random, List<T> pool, int totalWeight) {
        if (totalWeight < 0) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Negative total weight in getRandomItem"));
        } else if (totalWeight == 0) {
            return Optional.empty();
        } else {
            int i = random.nextInt(totalWeight);
            return getWeightedItem(pool, i);
        }
    }

    public static <T extends WeightedEntry> Optional<T> getWeightedItem(List<T> pool, int totalWeight) {
        for(T weightedEntry : pool) {
            totalWeight -= weightedEntry.getWeight().asInt();
            if (totalWeight < 0) {
                return Optional.of(weightedEntry);
            }
        }

        return Optional.empty();
    }

    public static <T extends WeightedEntry> Optional<T> getRandomItem(Random random, List<T> pool) {
        return getRandomItem(random, pool, getTotalWeight(pool));
    }
}
