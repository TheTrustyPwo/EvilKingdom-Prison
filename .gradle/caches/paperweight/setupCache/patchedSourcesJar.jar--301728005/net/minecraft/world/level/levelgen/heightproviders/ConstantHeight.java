package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class ConstantHeight extends HeightProvider {
    public static final ConstantHeight ZERO = new ConstantHeight(VerticalAnchor.absolute(0));
    public static final Codec<ConstantHeight> CODEC = Codec.either(VerticalAnchor.CODEC, RecordCodecBuilder.create((instance) -> {
        return instance.group(VerticalAnchor.CODEC.fieldOf("value").forGetter((provider) -> {
            return provider.value;
        })).apply(instance, ConstantHeight::new);
    })).xmap((either) -> {
        return either.map(ConstantHeight::of, (provider) -> {
            return provider;
        });
    }, (provider) -> {
        return Either.left(provider.value);
    });
    private final VerticalAnchor value;

    public static ConstantHeight of(VerticalAnchor offset) {
        return new ConstantHeight(offset);
    }

    private ConstantHeight(VerticalAnchor offset) {
        this.value = offset;
    }

    public VerticalAnchor getValue() {
        return this.value;
    }

    @Override
    public int sample(Random random, WorldGenerationContext context) {
        return this.value.resolveY(context);
    }

    @Override
    public HeightProviderType<?> getType() {
        return HeightProviderType.CONSTANT;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }
}
