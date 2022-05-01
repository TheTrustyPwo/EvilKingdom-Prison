package net.minecraft;

import java.util.Objects;

@FunctionalInterface
public interface CharPredicate {
    boolean test(char c);

    default CharPredicate and(CharPredicate predicate) {
        Objects.requireNonNull(predicate);
        return (c) -> {
            return this.test(c) && predicate.test(c);
        };
    }

    default CharPredicate negate() {
        return (c) -> {
            return !this.test(c);
        };
    }

    default CharPredicate or(CharPredicate predicate) {
        Objects.requireNonNull(predicate);
        return (c) -> {
            return this.test(c) || predicate.test(c);
        };
    }
}
