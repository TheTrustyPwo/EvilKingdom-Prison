package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.slf4j.Logger;

public class TrapezoidHeight extends HeightProvider {
    public static final Codec<TrapezoidHeight> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter((provider) -> {
            return provider.minInclusive;
        }), VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter((provider) -> {
            return provider.maxInclusive;
        }), Codec.INT.optionalFieldOf("plateau", Integer.valueOf(0)).forGetter((trapezoidHeight) -> {
            return trapezoidHeight.plateau;
        })).apply(instance, TrapezoidHeight::new);
    });
    private static final Logger LOGGER = LogUtils.getLogger();
    private final VerticalAnchor minInclusive;
    private final VerticalAnchor maxInclusive;
    private final int plateau;

    private TrapezoidHeight(VerticalAnchor minOffset, VerticalAnchor maxOffset, int plateau) {
        this.minInclusive = minOffset;
        this.maxInclusive = maxOffset;
        this.plateau = plateau;
    }

    public static TrapezoidHeight of(VerticalAnchor minOffset, VerticalAnchor maxOffset, int plateau) {
        return new TrapezoidHeight(minOffset, maxOffset, plateau);
    }

    public static TrapezoidHeight of(VerticalAnchor minOffset, VerticalAnchor maxOffset) {
        return of(minOffset, maxOffset, 0);
    }

    @Override
    public int sample(Random random, WorldGenerationContext context) {
        int i = this.minInclusive.resolveY(context);
        int j = this.maxInclusive.resolveY(context);
        if (i > j) {
            LOGGER.warn("Empty height range: {}", (Object)this);
            return i;
        } else {
            int k = j - i;
            if (this.plateau >= k) {
                return Mth.randomBetweenInclusive(random, i, j);
            } else {
                int l = (k - this.plateau) / 2;
                int m = k - l;
                return i + Mth.randomBetweenInclusive(random, 0, m) + Mth.randomBetweenInclusive(random, 0, l);
            }
        }
    }

    @Override
    public HeightProviderType<?> getType() {
        return HeightProviderType.TRAPEZOID;
    }

    @Override
    public String toString() {
        return this.plateau == 0 ? "triangle (" + this.minInclusive + "-" + this.maxInclusive + ")" : "trapezoid(" + this.plateau + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
    }
}
