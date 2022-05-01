package net.minecraft.world.level.levelgen.synth;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.List;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class PerlinSimplexNoise {
    private final SimplexNoise[] noiseLevels;
    private final double highestFreqValueFactor;
    private final double highestFreqInputFactor;

    public PerlinSimplexNoise(RandomSource random, List<Integer> octaves) {
        this(random, new IntRBTreeSet(octaves));
    }

    private PerlinSimplexNoise(RandomSource random, IntSortedSet octaves) {
        if (octaves.isEmpty()) {
            throw new IllegalArgumentException("Need some octaves!");
        } else {
            int i = -octaves.firstInt();
            int j = octaves.lastInt();
            int k = i + j + 1;
            if (k < 1) {
                throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
            } else {
                SimplexNoise simplexNoise = new SimplexNoise(random);
                int l = j;
                this.noiseLevels = new SimplexNoise[k];
                if (j >= 0 && j < k && octaves.contains(0)) {
                    this.noiseLevels[j] = simplexNoise;
                }

                for(int m = j + 1; m < k; ++m) {
                    if (m >= 0 && octaves.contains(l - m)) {
                        this.noiseLevels[m] = new SimplexNoise(random);
                    } else {
                        random.consumeCount(262);
                    }
                }

                if (j > 0) {
                    long n = (long)(simplexNoise.getValue(simplexNoise.xo, simplexNoise.yo, simplexNoise.zo) * (double)9.223372E18F);
                    RandomSource randomSource = new WorldgenRandom(new LegacyRandomSource(n));

                    for(int o = l - 1; o >= 0; --o) {
                        if (o < k && octaves.contains(l - o)) {
                            this.noiseLevels[o] = new SimplexNoise(randomSource);
                        } else {
                            randomSource.consumeCount(262);
                        }
                    }
                }

                this.highestFreqInputFactor = Math.pow(2.0D, (double)j);
                this.highestFreqValueFactor = 1.0D / (Math.pow(2.0D, (double)k) - 1.0D);
            }
        }
    }

    public double getValue(double x, double y, boolean useOrigin) {
        double d = 0.0D;
        double e = this.highestFreqInputFactor;
        double f = this.highestFreqValueFactor;

        for(SimplexNoise simplexNoise : this.noiseLevels) {
            if (simplexNoise != null) {
                d += simplexNoise.getValue(x * e + (useOrigin ? simplexNoise.xo : 0.0D), y * e + (useOrigin ? simplexNoise.yo : 0.0D)) * f;
            }

            e /= 2.0D;
            f *= 2.0D;
        }

        return d;
    }
}
