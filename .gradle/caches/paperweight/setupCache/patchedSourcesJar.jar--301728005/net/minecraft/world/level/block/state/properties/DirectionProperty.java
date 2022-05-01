package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.Direction;

public class DirectionProperty extends EnumProperty<Direction> {
    protected DirectionProperty(String name, Collection<Direction> values) {
        super(name, Direction.class, values);
    }

    public static DirectionProperty create(String name) {
        return create(name, (direction) -> {
            return true;
        });
    }

    public static DirectionProperty create(String name, Predicate<Direction> filter) {
        return create(name, Arrays.stream(Direction.values()).filter(filter).collect(Collectors.toList()));
    }

    public static DirectionProperty create(String name, Direction... values) {
        return create(name, Lists.newArrayList(values));
    }

    public static DirectionProperty create(String name, Collection<Direction> values) {
        return new DirectionProperty(name, values);
    }
}
