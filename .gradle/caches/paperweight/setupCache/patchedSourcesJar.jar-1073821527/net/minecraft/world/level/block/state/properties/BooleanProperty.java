package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Optional;

public class BooleanProperty extends Property<Boolean> {
    private final ImmutableSet<Boolean> values = ImmutableSet.of(true, false);

    protected BooleanProperty(String name) {
        super(name, Boolean.class);
    }

    @Override
    public Collection<Boolean> getPossibleValues() {
        return this.values;
    }

    public static BooleanProperty create(String name) {
        return new BooleanProperty(name);
    }

    @Override
    public Optional<Boolean> getValue(String name) {
        return !"true".equals(name) && !"false".equals(name) ? Optional.empty() : Optional.of(Boolean.valueOf(name));
    }

    @Override
    public String getName(Boolean value) {
        return value.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object instanceof BooleanProperty && super.equals(object)) {
            BooleanProperty booleanProperty = (BooleanProperty)object;
            return this.values.equals(booleanProperty.values);
        } else {
            return false;
        }
    }

    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + this.values.hashCode();
    }
}
