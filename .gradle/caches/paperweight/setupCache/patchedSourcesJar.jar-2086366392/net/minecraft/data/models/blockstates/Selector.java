package net.minecraft.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.state.properties.Property;

public final class Selector {
    private static final Selector EMPTY = new Selector(ImmutableList.of());
    private static final Comparator<Property.Value<?>> COMPARE_BY_NAME = Comparator.comparing((value) -> {
        return value.property().getName();
    });
    private final List<Property.Value<?>> values;

    public Selector extend(Property.Value<?> value) {
        return new Selector(ImmutableList.<Property.Value<?>>builder().addAll(this.values).add(value).build());
    }

    public Selector extend(Selector propertiesMap) {
        return new Selector(ImmutableList.<Property.Value<?>>builder().addAll(this.values).addAll(propertiesMap.values).build());
    }

    private Selector(List<Property.Value<?>> values) {
        this.values = values;
    }

    public static Selector empty() {
        return EMPTY;
    }

    public static Selector of(Property.Value<?>... values) {
        return new Selector(ImmutableList.copyOf(values));
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Selector && this.values.equals(((Selector)object).values);
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    public String getKey() {
        return this.values.stream().sorted(COMPARE_BY_NAME).map(Property.Value::toString).collect(Collectors.joining(","));
    }

    @Override
    public String toString() {
        return this.getKey();
    }
}
