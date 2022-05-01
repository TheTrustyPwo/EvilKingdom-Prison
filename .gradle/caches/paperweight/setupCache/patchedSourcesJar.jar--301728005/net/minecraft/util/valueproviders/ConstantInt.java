package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;

public class ConstantInt extends IntProvider {
    public static final ConstantInt ZERO = new ConstantInt(0);
    public static final Codec<ConstantInt> CODEC = Codec.either(Codec.INT, RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.INT.fieldOf("value").forGetter((provider) -> {
            return provider.value;
        })).apply(instance, ConstantInt::new);
    })).xmap((either) -> {
        return either.map(ConstantInt::of, (provider) -> {
            return provider;
        });
    }, (provider) -> {
        return Either.left(provider.value);
    });
    private final int value;

    public static ConstantInt of(int value) {
        return value == 0 ? ZERO : new ConstantInt(value);
    }

    private ConstantInt(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    @Override
    public int sample(Random random) {
        return this.value;
    }

    @Override
    public int getMinValue() {
        return this.value;
    }

    @Override
    public int getMaxValue() {
        return this.value;
    }

    @Override
    public IntProviderType<?> getType() {
        return IntProviderType.CONSTANT;
    }

    @Override
    public String toString() {
        return Integer.toString(this.value);
    }
}
