package net.minecraft.world.level.levelgen.feature.featuresize;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.Registry;

public abstract class FeatureSize {
    public static final Codec<FeatureSize> CODEC = Registry.FEATURE_SIZE_TYPES.byNameCodec().dispatch(FeatureSize::type, FeatureSizeType::codec);
    protected static final int MAX_WIDTH = 16;
    protected final OptionalInt minClippedHeight;

    protected static <S extends FeatureSize> RecordCodecBuilder<S, OptionalInt> minClippedHeightCodec() {
        return Codec.intRange(0, 80).optionalFieldOf("min_clipped_height").xmap((optional) -> {
            return optional.map(OptionalInt::of).orElse(OptionalInt.empty());
        }, (optionalInt) -> {
            return optionalInt.isPresent() ? Optional.of(optionalInt.getAsInt()) : Optional.empty();
        }).forGetter((featureSize) -> {
            return featureSize.minClippedHeight;
        });
    }

    public FeatureSize(OptionalInt minClippedHeight) {
        this.minClippedHeight = minClippedHeight;
    }

    protected abstract FeatureSizeType<?> type();

    public abstract int getSizeAtHeight(int height, int y);

    public OptionalInt minClippedHeight() {
        return this.minClippedHeight;
    }
}
