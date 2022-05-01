package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.Registry;

public abstract class IntProvider {
    private static final Codec<Either<Integer, IntProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(Codec.INT, Registry.INT_PROVIDER_TYPES.byNameCodec().dispatch(IntProvider::getType, IntProviderType::codec));
    public static final Codec<IntProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap((either) -> {
        return either.map(ConstantInt::of, (provider) -> {
            return provider;
        });
    }, (provider) -> {
        return provider.getType() == IntProviderType.CONSTANT ? Either.left(((ConstantInt)provider).getValue()) : Either.right(provider);
    });
    public static final Codec<IntProvider> NON_NEGATIVE_CODEC = codec(0, Integer.MAX_VALUE);
    public static final Codec<IntProvider> POSITIVE_CODEC = codec(1, Integer.MAX_VALUE);

    public static Codec<IntProvider> codec(int min, int max) {
        Function<IntProvider, DataResult<IntProvider>> function = (provider) -> {
            if (provider.getMinValue() < min) {
                return DataResult.error("Value provider too low: " + min + " [" + provider.getMinValue() + "-" + provider.getMaxValue() + "]");
            } else {
                return provider.getMaxValue() > max ? DataResult.error("Value provider too high: " + max + " [" + provider.getMinValue() + "-" + provider.getMaxValue() + "]") : DataResult.success(provider);
            }
        };
        return CODEC.flatXmap(function, function);
    }

    public abstract int sample(Random random);

    public abstract int getMinValue();

    public abstract int getMaxValue();

    public abstract IntProviderType<?> getType();
}
