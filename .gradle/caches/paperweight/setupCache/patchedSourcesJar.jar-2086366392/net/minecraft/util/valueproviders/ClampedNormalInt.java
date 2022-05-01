package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.util.Mth;

public class ClampedNormalInt extends IntProvider {
    public static final Codec<ClampedNormalInt> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.FLOAT.fieldOf("mean").forGetter((provider) -> {
            return provider.mean;
        }), Codec.FLOAT.fieldOf("deviation").forGetter((provider) -> {
            return provider.deviation;
        }), Codec.INT.fieldOf("min_inclusive").forGetter((provider) -> {
            return provider.min_inclusive;
        }), Codec.INT.fieldOf("max_inclusive").forGetter((provider) -> {
            return provider.max_inclusive;
        })).apply(instance, ClampedNormalInt::new);
    }).comapFlatMap((provider) -> {
        return provider.max_inclusive < provider.min_inclusive ? DataResult.error("Max must be larger than min: [" + provider.min_inclusive + ", " + provider.max_inclusive + "]") : DataResult.success(provider);
    }, Function.identity());
    private float mean;
    private float deviation;
    private int min_inclusive;
    private int max_inclusive;

    public static ClampedNormalInt of(float mean, float deviation, int min, int max) {
        return new ClampedNormalInt(mean, deviation, min, max);
    }

    private ClampedNormalInt(float mean, float deviation, int min, int max) {
        this.mean = mean;
        this.deviation = deviation;
        this.min_inclusive = min;
        this.max_inclusive = max;
    }

    @Override
    public int sample(Random random) {
        return sample(random, this.mean, this.deviation, (float)this.min_inclusive, (float)this.max_inclusive);
    }

    public static int sample(Random random, float mean, float deviation, float min, float max) {
        return (int)Mth.clamp(Mth.normal(random, mean, deviation), min, max);
    }

    @Override
    public int getMinValue() {
        return this.min_inclusive;
    }

    @Override
    public int getMaxValue() {
        return this.max_inclusive;
    }

    @Override
    public IntProviderType<?> getType() {
        return IntProviderType.CLAMPED_NORMAL;
    }

    @Override
    public String toString() {
        return "normal(" + this.mean + ", " + this.deviation + ") in [" + this.min_inclusive + "-" + this.max_inclusive + "]";
    }
}
