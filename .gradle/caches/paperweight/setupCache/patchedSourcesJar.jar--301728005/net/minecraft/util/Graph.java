package net.minecraft.util;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class Graph {
    private Graph() {
    }

    public static <T> boolean depthFirstSearch(Map<T, Set<T>> successors, Set<T> visited, Set<T> visiting, Consumer<T> reversedOrderConsumer, T now) {
        if (visited.contains(now)) {
            return false;
        } else if (visiting.contains(now)) {
            return true;
        } else {
            visiting.add(now);

            for(T object : successors.getOrDefault(now, ImmutableSet.of())) {
                if (depthFirstSearch(successors, visited, visiting, reversedOrderConsumer, object)) {
                    return true;
                }
            }

            visiting.remove(now);
            visited.add(now);
            reversedOrderConsumer.accept(now);
            return false;
        }
    }
}
