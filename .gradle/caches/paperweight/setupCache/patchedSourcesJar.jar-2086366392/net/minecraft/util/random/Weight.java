package net.minecraft.util.random;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import org.slf4j.Logger;

public class Weight {
    public static final Codec<Weight> CODEC = Codec.INT.xmap(Weight::of, Weight::asInt);
    private static final Weight ONE = new Weight(1);
    private static final Logger LOGGER = LogUtils.getLogger();
    private final int value;

    private Weight(int weight) {
        this.value = weight;
    }

    public static Weight of(int weight) {
        if (weight == 1) {
            return ONE;
        } else {
            validateWeight(weight);
            return new Weight(weight);
        }
    }

    public int asInt() {
        return this.value;
    }

    private static void validateWeight(int weight) {
        if (weight < 0) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Weight should be >= 0"));
        } else {
            if (weight == 0 && SharedConstants.IS_RUNNING_IN_IDE) {
                LOGGER.warn("Found 0 weight, make sure this is intentional!");
            }

        }
    }

    @Override
    public String toString() {
        return Integer.toString(this.value);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.value);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof Weight && this.value == ((Weight)object).value;
        }
    }
}
