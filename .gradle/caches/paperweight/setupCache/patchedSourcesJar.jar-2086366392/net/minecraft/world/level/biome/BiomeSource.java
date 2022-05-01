package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.util.Graph;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;

public abstract class BiomeSource implements BiomeResolver {
    public static final Codec<BiomeSource> CODEC;
    private final Set<Holder<Biome>> possibleBiomes;
    private final Supplier<List<BiomeSource.StepFeatureData>> featuresPerStep;

    protected BiomeSource(Stream<Holder<Biome>> biomeStream) {
        this(biomeStream.distinct().toList());
    }

    protected BiomeSource(List<Holder<Biome>> biomes) {
        this.possibleBiomes = new ObjectLinkedOpenHashSet<>(biomes);
        this.featuresPerStep = Suppliers.memoize(() -> {
            return this.buildFeaturesPerStep(biomes, true);
        });
    }

    private List<BiomeSource.StepFeatureData> buildFeaturesPerStep(List<Holder<Biome>> biomes, boolean bl) {
        Object2IntMap<PlacedFeature> object2IntMap = new Object2IntOpenHashMap<>();
        MutableInt mutableInt = new MutableInt(0);

        record FeatureData(int featureIndex, int step, PlacedFeature feature) {
        }

        Comparator<FeatureData> comparator = Comparator.comparingInt(FeatureData::step).thenComparingInt(FeatureData::featureIndex);
        Map<FeatureData, Set<FeatureData>> map = new TreeMap<>(comparator);
        int i = 0;

        for(Holder<Biome> holder : biomes) {
            Biome biome = holder.value();
            List<FeatureData> list = Lists.newArrayList();
            List<HolderSet<PlacedFeature>> list2 = biome.getGenerationSettings().features();
            i = Math.max(i, list2.size());

            for(int j = 0; j < list2.size(); ++j) {
                for(Holder<PlacedFeature> holder2 : list2.get(j)) {
                    PlacedFeature placedFeature = holder2.value();
                    list.add(new FeatureData(object2IntMap.computeIfAbsent(placedFeature, (object) -> {
                        return mutableInt.getAndIncrement();
                    }), j, placedFeature));
                }
            }

            for(int k = 0; k < list.size(); ++k) {
                Set<FeatureData> set = map.computeIfAbsent(list.get(k), (arg) -> {
                    return new TreeSet<>(comparator);
                });
                if (k < list.size() - 1) {
                    set.add(list.get(k + 1));
                }
            }
        }

        Set<FeatureData> set2 = new TreeSet<>(comparator);
        Set<FeatureData> set3 = new TreeSet<>(comparator);
        List<FeatureData> list3 = Lists.newArrayList();

        for(FeatureData lv : map.keySet()) {
            if (!set3.isEmpty()) {
                throw new IllegalStateException("You somehow broke the universe; DFS bork (iteration finished with non-empty in-progress vertex set");
            }

            if (!set2.contains(lv) && Graph.depthFirstSearch(map, set2, set3, list3::add, lv)) {
                if (!bl) {
                    throw new IllegalStateException("Feature order cycle found");
                }

                List<Holder<Biome>> list4 = new ArrayList<>(biomes);

                int l;
                do {
                    l = list4.size();
                    ListIterator<Holder<Biome>> listIterator = list4.listIterator();

                    while(listIterator.hasNext()) {
                        Holder<Biome> holder3 = listIterator.next();
                        listIterator.remove();

                        try {
                            this.buildFeaturesPerStep(list4, false);
                        } catch (IllegalStateException var18) {
                            continue;
                        }

                        listIterator.add(holder3);
                    }
                } while(l != list4.size());

                throw new IllegalStateException("Feature order cycle found, involved biomes: " + list4);
            }
        }

        Collections.reverse(list3);
        Builder<BiomeSource.StepFeatureData> builder = ImmutableList.builder();

        for(int m = 0; m < i; ++m) {
            List<PlacedFeature> list5 = list3.stream().filter((arg) -> {
                return arg.step() == m;
            }).map(FeatureData::feature).collect(Collectors.toList());
            int o = list5.size();
            Object2IntMap<PlacedFeature> object2IntMap2 = new Object2IntOpenCustomHashMap<>(o, Util.identityStrategy());

            for(int p = 0; p < o; ++p) {
                object2IntMap2.put(list5.get(p), p);
            }

            builder.add(new BiomeSource.StepFeatureData(list5, object2IntMap2));
        }

        return builder.build();
    }

    protected abstract Codec<? extends BiomeSource> codec();

    public abstract BiomeSource withSeed(long seed);

    public Set<Holder<Biome>> possibleBiomes() {
        return this.possibleBiomes;
    }

    public Set<Holder<Biome>> getBiomesWithin(int x, int y, int z, int radius, Climate.Sampler sampler) {
        int i = QuartPos.fromBlock(x - radius);
        int j = QuartPos.fromBlock(y - radius);
        int k = QuartPos.fromBlock(z - radius);
        int l = QuartPos.fromBlock(x + radius);
        int m = QuartPos.fromBlock(y + radius);
        int n = QuartPos.fromBlock(z + radius);
        int o = l - i + 1;
        int p = m - j + 1;
        int q = n - k + 1;
        Set<Holder<Biome>> set = Sets.newHashSet();

        for(int r = 0; r < q; ++r) {
            for(int s = 0; s < o; ++s) {
                for(int t = 0; t < p; ++t) {
                    int u = i + s;
                    int v = j + t;
                    int w = k + r;
                    set.add(this.getNoiseBiome(u, v, w, sampler));
                }
            }
        }

        return set;
    }

    @Nullable
    public Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(int x, int y, int z, int radius, Predicate<Holder<Biome>> predicate, Random random, Climate.Sampler noiseSampler) {
        return this.findBiomeHorizontal(x, y, z, radius, 1, predicate, random, false, noiseSampler);
    }

    @Nullable
    public Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(int x, int y, int z, int radius, int blockCheckInterval, Predicate<Holder<Biome>> predicate, Random random, boolean bl, Climate.Sampler noiseSampler) {
        int i = QuartPos.fromBlock(x);
        int j = QuartPos.fromBlock(z);
        int k = QuartPos.fromBlock(radius);
        int l = QuartPos.fromBlock(y);
        Pair<BlockPos, Holder<Biome>> pair = null;
        int m = 0;
        int n = bl ? 0 : k;

        for(int o = n; o <= k; o += blockCheckInterval) {
            for(int p = SharedConstants.debugGenerateSquareTerrainWithoutNoise ? 0 : -o; p <= o; p += blockCheckInterval) {
                boolean bl2 = Math.abs(p) == o;

                for(int q = -o; q <= o; q += blockCheckInterval) {
                    if (bl) {
                        boolean bl3 = Math.abs(q) == o;
                        if (!bl3 && !bl2) {
                            continue;
                        }
                    }

                    int r = i + q;
                    int s = j + p;
                    Holder<Biome> holder = this.getNoiseBiome(r, l, s, noiseSampler);
                    if (predicate.test(holder)) {
                        if (pair == null || random.nextInt(m + 1) == 0) {
                            BlockPos blockPos = new BlockPos(QuartPos.toBlock(r), y, QuartPos.toBlock(s));
                            if (bl) {
                                return Pair.of(blockPos, holder);
                            }

                            pair = Pair.of(blockPos, holder);
                        }

                        ++m;
                    }
                }
            }
        }

        return pair;
    }

    @Override
    public abstract Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler noise);

    public void addDebugInfo(List<String> info, BlockPos pos, Climate.Sampler noiseSampler) {
    }

    public List<BiomeSource.StepFeatureData> featuresPerStep() {
        return this.featuresPerStep.get();
    }

    static {
        Registry.register(Registry.BIOME_SOURCE, "fixed", FixedBiomeSource.CODEC);
        Registry.register(Registry.BIOME_SOURCE, "multi_noise", MultiNoiseBiomeSource.CODEC);
        Registry.register(Registry.BIOME_SOURCE, "checkerboard", CheckerboardColumnBiomeSource.CODEC);
        Registry.register(Registry.BIOME_SOURCE, "the_end", TheEndBiomeSource.CODEC);
        CODEC = Registry.BIOME_SOURCE.byNameCodec().dispatchStable(BiomeSource::codec, Function.identity());
    }

    public static record StepFeatureData(List<PlacedFeature> features, ToIntFunction<PlacedFeature> indexMapping) {
    }
}
