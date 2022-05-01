package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class IntegerProperty extends Property<Integer> {

    private final ImmutableSet<Integer> values;
    // CraftBukkit start
    public final int min;
    public final int max;

    // Paper start - optimise iblockdata state lookup
    @Override
    public final int getIdFor(final Integer value) {
        final int val = value.intValue();
        final int ret = val - this.min;

        return ret | ((this.max - ret) >> 31);
    }
    // Paper end - optimise iblockdata state lookup

    protected IntegerProperty(String name, int min, int max) {
        super(name, Integer.class);
        this.min = min;
        this.max = max;
        // CraftBukkit end
        if (min < 0) {
            throw new IllegalArgumentException("Min value of " + name + " must be 0 or greater");
        } else if (max <= min) {
            throw new IllegalArgumentException("Max value of " + name + " must be greater than min (" + min + ")");
        } else {
            Set<Integer> set = Sets.newHashSet();

            for (int k = min; k <= max; ++k) {
                set.add(k);
            }

            this.values = ImmutableSet.copyOf(set);
        }
    }

    @Override
    public Collection<Integer> getPossibleValues() {
        return this.values;
    }

    public boolean equals_unused(Object object) { // Paper
        if (this == object) {
            return true;
        } else if (object instanceof IntegerProperty && super.equals(object)) {
            IntegerProperty blockstateinteger = (IntegerProperty) object;

            return this.values.equals(blockstateinteger.values);
        } else {
            return false;
        }
    }

    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + this.values.hashCode();
    }

    public static IntegerProperty create(String name, int min, int max) {
        return new IntegerProperty(name, min, max);
    }

    @Override
    public Optional<Integer> getValue(String name) {
        try {
            Integer integer = Integer.valueOf(name);

            return this.values.contains(integer) ? Optional.of(integer) : Optional.empty();
        } catch (NumberFormatException numberformatexception) {
            return Optional.empty();
        }
    }

    public String getName(Integer value) {
        return value.toString();
    }
}
