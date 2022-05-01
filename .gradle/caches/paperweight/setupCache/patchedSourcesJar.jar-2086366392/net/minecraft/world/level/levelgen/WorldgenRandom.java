package net.minecraft.world.level.levelgen;

import java.util.Random;
import java.util.function.LongFunction;

public class WorldgenRandom extends Random implements RandomSource {
    private final RandomSource randomSource;
    private int count;

    public WorldgenRandom(RandomSource baseRandom) {
        super(0L);
        this.randomSource = baseRandom;
    }

    public int getCount() {
        return this.count;
    }

    @Override
    public RandomSource fork() {
        return this.randomSource.fork();
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return this.randomSource.forkPositional();
    }

    @Override
    public int next(int i) {
        ++this.count;
        RandomSource var3 = this.randomSource;
        if (var3 instanceof LegacyRandomSource) {
            LegacyRandomSource legacyRandomSource = (LegacyRandomSource)var3;
            return legacyRandomSource.next(i);
        } else {
            return (int)(this.randomSource.nextLong() >>> 64 - i);
        }
    }

    @Override
    public synchronized void setSeed(long seed) {
        if (this.randomSource != null) {
            this.randomSource.setSeed(seed);
        }
    }

    public long setDecorationSeed(long worldSeed, int blockX, int blockZ) {
        this.setSeed(worldSeed);
        long l = this.nextLong() | 1L;
        long m = this.nextLong() | 1L;
        long n = (long)blockX * l + (long)blockZ * m ^ worldSeed;
        this.setSeed(n);
        return n;
    }

    public void setFeatureSeed(long populationSeed, int index, int step) {
        long l = populationSeed + (long)index + (long)(10000 * step);
        this.setSeed(l);
    }

    public void setLargeFeatureSeed(long worldSeed, int chunkX, int chunkZ) {
        this.setSeed(worldSeed);
        long l = this.nextLong();
        long m = this.nextLong();
        long n = (long)chunkX * l ^ (long)chunkZ * m ^ worldSeed;
        this.setSeed(n);
    }

    public void setLargeFeatureWithSalt(long worldSeed, int regionX, int regionZ, int salt) {
        long l = (long)regionX * 341873128712L + (long)regionZ * 132897987541L + worldSeed + (long)salt;
        this.setSeed(l);
    }

    public static Random seedSlimeChunk(int chunkX, int chunkZ, long worldSeed, long scrambler) {
        return new Random(worldSeed + (long)(chunkX * chunkX * 4987142) + (long)(chunkX * 5947611) + (long)(chunkZ * chunkZ) * 4392871L + (long)(chunkZ * 389711) ^ scrambler);
    }

    public static enum Algorithm {
        LEGACY(LegacyRandomSource::new),
        XOROSHIRO(XoroshiroRandomSource::new);

        private final LongFunction<RandomSource> constructor;

        private Algorithm(LongFunction<RandomSource> provider) {
            this.constructor = provider;
        }

        public RandomSource newInstance(long seed) {
            return this.constructor.apply(seed);
        }
    }
}
