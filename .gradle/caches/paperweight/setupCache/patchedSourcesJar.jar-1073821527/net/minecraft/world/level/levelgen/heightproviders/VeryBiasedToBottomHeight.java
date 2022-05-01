package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.slf4j.Logger;

public class VeryBiasedToBottomHeight extends HeightProvider {
    public static final Codec<VeryBiasedToBottomHeight> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter((provider) -> {
            return provider.minInclusive;
        }), VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter((provider) -> {
            return provider.maxInclusive;
        }), Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("inner", 1).forGetter((provider) -> {
            return provider.inner;
        })).apply(instance, VeryBiasedToBottomHeight::new);
    });
    private static final Logger LOGGER = LogUtils.getLogger();
    private final VerticalAnchor minInclusive;
    private final VerticalAnchor maxInclusive;
    private final int inner;

    private VeryBiasedToBottomHeight(VerticalAnchor minOffset, VerticalAnchor maxOffset, int inner) {
        this.minInclusive = minOffset;
        this.maxInclusive = maxOffset;
        this.inner = inner;
    }

    public static VeryBiasedToBottomHeight of(VerticalAnchor minOffset, VerticalAnchor maxOffset, int inner) {
        return new VeryBiasedToBottomHeight(minOffset, maxOffset, inner);
    }

    @Override
    public int sample(Random random, WorldGenerationContext context) {
        int i = this.minInclusive.resolveY(context);
        int j = this.maxInclusive.resolveY(context);
        if (j - i - this.inner + 1 <= 0) {
            LOGGER.warn("Empty height range: {}", (Object)this);
            return i;
        } else {
            int k = Mth.nextInt(random, i + this.inner, j);
            int l = Mth.nextInt(random, i, k - 1);
            return Mth.nextInt(random, i, l - 1 + this.inner);
        }
    }

    @Override
    public HeightProviderType<?> getType() {
        return HeightProviderType.VERY_BIASED_TO_BOTTOM;
    }

    @Override
    public String toString() {
        return "biased[" + this.minInclusive + "-" + this.maxInclusive + " inner: " + this.inner + "]";
    }
}
