package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.Registry;

public abstract class FloatProvider {
    private static final Codec<Either<Float, FloatProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(Codec.FLOAT, Registry.FLOAT_PROVIDER_TYPES.byNameCodec().dispatch(FloatProvider::getType, FloatProviderType::codec));
    public static final Codec<FloatProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap((either) -> {
        return either.map(ConstantFloat::of, (provider) -> {
            return provider;
        });
    }, (provider) -> {
        return provider.getType() == FloatProviderType.CONSTANT ? Either.left(((ConstantFloat)provider).getValue()) : Either.right(provider);
    });

    public static Codec<FloatProvider> codec(float min, float max) {
        Function<FloatProvider, DataResult<FloatProvider>> function = (provider) -> {
            if (provider.getMinValue() < min) {
                return DataResult.error("Value provider too low: " + min + " [" + provider.getMinValue() + "-" + provider.getMaxValue() + "]");
            } else {
                return provider.getMaxValue() > max ? DataResult.error("Value provider too high: " + max + " [" + provider.getMinValue() + "-" + provider.getMaxValue() + "]") : DataResult.success(provider);
            }
        };
        return CODEC.flatXmap(function, function);
    }

    public abstract float sample(Random random);

    public abstract float getMinValue();

    public abstract float getMaxValue();

    public abstract FloatProviderType<?> getType();
}
