package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;

public class ConstantFloat extends FloatProvider {
    public static final ConstantFloat ZERO = new ConstantFloat(0.0F);
    public static final Codec<ConstantFloat> CODEC = Codec.either(Codec.FLOAT, RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.FLOAT.fieldOf("value").forGetter((provider) -> {
            return provider.value;
        })).apply(instance, ConstantFloat::new);
    })).xmap((either) -> {
        return either.map(ConstantFloat::of, (provider) -> {
            return provider;
        });
    }, (provider) -> {
        return Either.left(provider.value);
    });
    private final float value;

    public static ConstantFloat of(float value) {
        return value == 0.0F ? ZERO : new ConstantFloat(value);
    }

    private ConstantFloat(float value) {
        this.value = value;
    }

    public float getValue() {
        return this.value;
    }

    @Override
    public float sample(Random random) {
        return this.value;
    }

    @Override
    public float getMinValue() {
        return this.value;
    }

    @Override
    public float getMaxValue() {
        return this.value + 1.0F;
    }

    @Override
    public FloatProviderType<?> getType() {
        return FloatProviderType.CONSTANT;
    }

    @Override
    public String toString() {
        return Float.toString(this.value);
    }
}
