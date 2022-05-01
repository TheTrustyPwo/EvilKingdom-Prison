package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public class TheEndBiomeSource extends BiomeSource {
    public static final Codec<TheEndBiomeSource> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter((theEndBiomeSource) -> {
            return null;
        }), Codec.LONG.fieldOf("seed").stable().forGetter((theEndBiomeSource) -> {
            return theEndBiomeSource.seed;
        })).apply(instance, instance.stable(TheEndBiomeSource::new));
    });
    private static final float ISLAND_THRESHOLD = -0.9F;
    public static final int ISLAND_CHUNK_DISTANCE = 64;
    private static final long ISLAND_CHUNK_DISTANCE_SQR = 4096L;
    private final SimplexNoise islandNoise;
    private final long seed;
    private final Holder<Biome> end;
    private final Holder<Biome> highlands;
    private final Holder<Biome> midlands;
    private final Holder<Biome> islands;
    private final Holder<Biome> barrens;
    // Paper start
    private static final class NoiseCache {
        public long[] keys = new long[8192];
        public float[] values = new float[8192];
        public NoiseCache() {
            java.util.Arrays.fill(keys, Long.MIN_VALUE);
        }
    }
    private static final ThreadLocal<java.util.Map<SimplexNoise, NoiseCache>> noiseCache = ThreadLocal.withInitial(java.util.WeakHashMap::new);
    // Paper end

    public TheEndBiomeSource(Registry<Biome> biomeRegistry, long seed) {
        this(seed, biomeRegistry.getOrCreateHolder(Biomes.THE_END), biomeRegistry.getOrCreateHolder(Biomes.END_HIGHLANDS), biomeRegistry.getOrCreateHolder(Biomes.END_MIDLANDS), biomeRegistry.getOrCreateHolder(Biomes.SMALL_END_ISLANDS), biomeRegistry.getOrCreateHolder(Biomes.END_BARRENS));
    }

    private TheEndBiomeSource(long l, Holder<Biome> holder, Holder<Biome> holder2, Holder<Biome> holder3, Holder<Biome> holder4, Holder<Biome> holder5) {
        super(ImmutableList.of(holder, holder2, holder3, holder4, holder5));
        this.seed = l;
        this.end = holder;
        this.highlands = holder2;
        this.midlands = holder3;
        this.islands = holder4;
        this.barrens = holder5;
        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(l));
        worldgenRandom.consumeCount(17292);
        this.islandNoise = new SimplexNoise(worldgenRandom);
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public BiomeSource withSeed(long seed) {
        return new TheEndBiomeSource(seed, this.end, this.highlands, this.midlands, this.islands, this.barrens);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        int i = x >> 2;
        int j = z >> 2;
        if ((long)i * (long)i + (long)j * (long)j <= 4096L) {
            return this.end;
        } else {
            float f = getHeightValue(this.islandNoise, i * 2 + 1, j * 2 + 1);
            if (f > 40.0F) {
                return this.highlands;
            } else if (f >= 0.0F) {
                return this.midlands;
            } else {
                return f < -20.0F ? this.islands : this.barrens;
            }
        }
    }

    public boolean stable(long seed) {
        return this.seed == seed;
    }

    public static float getHeightValue(SimplexNoise simplexNoise, int i, int j) {
        int k = i / 2;
        int l = j / 2;
        int m = i % 2;
        int n = j % 2;
        float f = 100.0F - Mth.sqrt((long) i * (long) i + (long) j * (long) j) * 8.0F; // Paper - cast ints to long to avoid integer overflow
        f = Mth.clamp(f, -100.0F, 80.0F);

        NoiseCache cache = noiseCache.get().computeIfAbsent(simplexNoise, noiseKey -> new NoiseCache()); // Paper
        for(int o = -12; o <= 12; ++o) {
            for(int p = -12; p <= 12; ++p) {
                long q = (long)(k + o);
                long r = (long)(l + p);
                // Paper start - Significantly improve end generation performance by using a noise cache
                long key = net.minecraft.world.level.ChunkPos.asLong((int) q, (int) r);
                int index = (int) it.unimi.dsi.fastutil.HashCommon.mix(key) & 8191;
                float g = Float.MIN_VALUE;
                if (cache.keys[index] == key) {
                    g = cache.values[index];
                } else {
                    if (q * q + r * r > 4096L && simplexNoise.getValue((double)q, (double)r) < (double)-0.9F) {
                        g = (Mth.abs((float) q) * 3439.0F + Mth.abs((float) r) * 147.0F) % 13.0F + 9.0F;
                    }
                    cache.keys[index] = key;
                    cache.values[index] = g;
                }
                if (g != Float.MIN_VALUE) {
                    // Paper end
                    float h = (float)(m - o * 2);
                    float s = (float)(n - p * 2);
                    float t = 100.0F - Mth.sqrt(h * h + s * s) * g;
                    t = Mth.clamp(t, -100.0F, 80.0F);
                    f = Math.max(f, t);
                }
            }
        }

        return f;
    }
}
