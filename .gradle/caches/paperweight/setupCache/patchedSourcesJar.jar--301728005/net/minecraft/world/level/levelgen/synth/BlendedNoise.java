package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import java.util.stream.IntStream;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseSamplingSettings;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class BlendedNoise implements DensityFunction.SimpleFunction {
    public static final BlendedNoise UNSEEDED = new BlendedNoise(new XoroshiroRandomSource(0L), new NoiseSamplingSettings(1.0D, 1.0D, 80.0D, 160.0D), 4, 8);
    public static final Codec<BlendedNoise> CODEC = Codec.unit(UNSEEDED);
    private final PerlinNoise minLimitNoise;
    private final PerlinNoise maxLimitNoise;
    private final PerlinNoise mainNoise;
    private final double xzScale;
    private final double yScale;
    private final double xzMainScale;
    private final double yMainScale;
    private final int cellWidth;
    private final int cellHeight;
    private final double maxValue;

    private BlendedNoise(PerlinNoise lowerInterpolatedNoise, PerlinNoise upperInterpolatedNoise, PerlinNoise interpolationNoise, NoiseSamplingSettings config, int cellWidth, int cellHeight) {
        this.minLimitNoise = lowerInterpolatedNoise;
        this.maxLimitNoise = upperInterpolatedNoise;
        this.mainNoise = interpolationNoise;
        this.xzScale = 684.412D * config.xzScale();
        this.yScale = 684.412D * config.yScale();
        this.xzMainScale = this.xzScale / config.xzFactor();
        this.yMainScale = this.yScale / config.yFactor();
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.maxValue = lowerInterpolatedNoise.maxBrokenValue(this.yScale);
    }

    public BlendedNoise(RandomSource random, NoiseSamplingSettings config, int cellWidth, int cellHeight) {
        this(PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)), PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)), PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-7, 0)), config, cellWidth, cellHeight);
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        int i = Math.floorDiv(pos.blockX(), this.cellWidth);
        int j = Math.floorDiv(pos.blockY(), this.cellHeight);
        int k = Math.floorDiv(pos.blockZ(), this.cellWidth);
        double d = 0.0D;
        double e = 0.0D;
        double f = 0.0D;
        boolean bl = true;
        double g = 1.0D;

        for(int l = 0; l < 8; ++l) {
            ImprovedNoise improvedNoise = this.mainNoise.getOctaveNoise(l);
            if (improvedNoise != null) {
                f += improvedNoise.noise(PerlinNoise.wrap((double)i * this.xzMainScale * g), PerlinNoise.wrap((double)j * this.yMainScale * g), PerlinNoise.wrap((double)k * this.xzMainScale * g), this.yMainScale * g, (double)j * this.yMainScale * g) / g;
            }

            g /= 2.0D;
        }

        double h = (f / 10.0D + 1.0D) / 2.0D;
        boolean bl2 = h >= 1.0D;
        boolean bl3 = h <= 0.0D;
        g = 1.0D;

        for(int m = 0; m < 16; ++m) {
            double n = PerlinNoise.wrap((double)i * this.xzScale * g);
            double o = PerlinNoise.wrap((double)j * this.yScale * g);
            double p = PerlinNoise.wrap((double)k * this.xzScale * g);
            double q = this.yScale * g;
            if (!bl2) {
                ImprovedNoise improvedNoise2 = this.minLimitNoise.getOctaveNoise(m);
                if (improvedNoise2 != null) {
                    d += improvedNoise2.noise(n, o, p, q, (double)j * q) / g;
                }
            }

            if (!bl3) {
                ImprovedNoise improvedNoise3 = this.maxLimitNoise.getOctaveNoise(m);
                if (improvedNoise3 != null) {
                    e += improvedNoise3.noise(n, o, p, q, (double)j * q) / g;
                }
            }

            g /= 2.0D;
        }

        return Mth.clampedLerp(d / 512.0D, e / 512.0D, h) / 128.0D;
    }

    @Override
    public double minValue() {
        return -this.maxValue();
    }

    @Override
    public double maxValue() {
        return this.maxValue;
    }

    @VisibleForTesting
    public void parityConfigString(StringBuilder info) {
        info.append("BlendedNoise{minLimitNoise=");
        this.minLimitNoise.parityConfigString(info);
        info.append(", maxLimitNoise=");
        this.maxLimitNoise.parityConfigString(info);
        info.append(", mainNoise=");
        this.mainNoise.parityConfigString(info);
        info.append(String.format(", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=%d, cellHeight=%d", this.xzScale, this.yScale, this.xzMainScale, this.yMainScale, this.cellWidth, this.cellHeight)).append('}');
    }

    @Override
    public Codec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
