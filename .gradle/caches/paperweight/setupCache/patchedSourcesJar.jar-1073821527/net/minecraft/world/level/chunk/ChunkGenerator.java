package net.minecraft.world.level.chunk;

import com.google.common.base.Stopwatch;
import com.mojang.datafixers.Products.P1;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public abstract class ChunkGenerator implements BiomeManager.NoiseBiomeSource {
    private static final Logger LOGGER;
    public static final Codec<ChunkGenerator> CODEC;
    public final Registry<StructureSet> structureSets;
    protected final BiomeSource biomeSource;
    protected final BiomeSource runtimeBiomeSource;
    public final Optional<HolderSet<StructureSet>> structureOverrides;
    private final Map<ConfiguredStructureFeature<?, ?>, List<StructurePlacement>> placementsForFeature = new Object2ObjectOpenHashMap<>();
    private final Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> ringPositions = new Object2ObjectArrayMap<>();
    private boolean hasGeneratedPositions;
    /** @deprecated */
    @Deprecated
    public final long ringPlacementSeed;

    protected static final <T extends ChunkGenerator> P1<Mu<T>, Registry<StructureSet>> commonCodec(Instance<T> instance) {
        return instance.group(RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY).forGetter((chunkGenerator) -> {
            return chunkGenerator.structureSets;
        }));
    }

    public ChunkGenerator(Registry<StructureSet> registry, Optional<HolderSet<StructureSet>> optional, BiomeSource biomeSource) {
        this(registry, optional, biomeSource, biomeSource, 0L);
    }

    public ChunkGenerator(Registry<StructureSet> registry, Optional<HolderSet<StructureSet>> optional, BiomeSource biomeSource, BiomeSource biomeSource2, long l) {
        this.structureSets = registry;
        this.biomeSource = biomeSource;
        this.runtimeBiomeSource = biomeSource2;
        this.structureOverrides = optional;
        this.ringPlacementSeed = l;
    }

    public Stream<Holder<StructureSet>> possibleStructureSets() {
        return this.structureOverrides.isPresent() ? this.structureOverrides.get().stream() : this.structureSets.holders().map(Holder::hackyErase);
    }

    private void generatePositions() {
        Set<Holder<Biome>> set = this.runtimeBiomeSource.possibleBiomes();
        this.possibleStructureSets().forEach((holder) -> {
            StructureSet structureSet = holder.value();

            for(StructureSet.StructureSelectionEntry structureSelectionEntry : structureSet.structures()) {
                this.placementsForFeature.computeIfAbsent(structureSelectionEntry.structure().value(), (configuredStructureFeature) -> {
                    return new ArrayList();
                }).add(structureSet.placement());
            }

            StructurePlacement structurePlacement = structureSet.placement();
            if (structurePlacement instanceof ConcentricRingsStructurePlacement) {
                ConcentricRingsStructurePlacement concentricRingsStructurePlacement = (ConcentricRingsStructurePlacement)structurePlacement;
                if (structureSet.structures().stream().anyMatch((structureSelectionEntry) -> {
                    return structureSelectionEntry.generatesInMatchingBiome(set::contains);
                })) {
                    this.ringPositions.put(concentricRingsStructurePlacement, this.generateRingPositions(holder, concentricRingsStructurePlacement));
                }
            }

        });
    }

    private CompletableFuture<List<ChunkPos>> generateRingPositions(Holder<StructureSet> strongholdSet, ConcentricRingsStructurePlacement placement) {
        return placement.count() == 0 ? CompletableFuture.completedFuture(List.of()) : CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("placement calculation", () -> {
            Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
            List<ChunkPos> list = new ArrayList<>();
            Set<Holder<Biome>> set = strongholdSet.value().structures().stream().flatMap((entry) -> {
                return entry.structure().value().biomes().stream();
            }).collect(Collectors.toSet());
            int i = placement.distance();
            int j = placement.count();
            int k = placement.spread();
            Random random = new Random();
            random.setSeed(this.ringPlacementSeed);
            double d = random.nextDouble() * Math.PI * 2.0D;
            int l = 0;
            int m = 0;

            for(int n = 0; n < j; ++n) {
                double e = (double)(4 * i + i * m * 6) + (random.nextDouble() - 0.5D) * (double)i * 2.5D;
                int o = (int)Math.round(Math.cos(d) * e);
                int p = (int)Math.round(Math.sin(d) * e);
                Pair<BlockPos, Holder<Biome>> pair = this.biomeSource.findBiomeHorizontal(SectionPos.sectionToBlockCoord(o, 8), 0, SectionPos.sectionToBlockCoord(p, 8), 112, set::contains, random, this.climateSampler());
                if (pair != null) {
                    BlockPos blockPos = pair.getFirst();
                    o = SectionPos.blockToSectionCoord(blockPos.getX());
                    p = SectionPos.blockToSectionCoord(blockPos.getZ());
                }

                list.add(new ChunkPos(o, p));
                d += (Math.PI * 2D) / (double)k;
                ++l;
                if (l == k) {
                    ++m;
                    l = 0;
                    k += 2 * k / (m + 1);
                    k = Math.min(k, j - n);
                    d += random.nextDouble() * Math.PI * 2.0D;
                }
            }

            double f = (double)stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) / 1000.0D;
            LOGGER.debug("Calculation for {} took {}s", strongholdSet, f);
            return list;
        }), Util.backgroundExecutor());
    }

    protected abstract Codec<? extends ChunkGenerator> codec();

    public Optional<ResourceKey<Codec<? extends ChunkGenerator>>> getTypeNameForDataFixer() {
        return Registry.CHUNK_GENERATOR.getResourceKey(this.codec());
    }

    public abstract ChunkGenerator withSeed(long seed);

    public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> biomeRegistry, Executor executor, Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
            chunk.fillBiomesFromNoise(this.runtimeBiomeSource::getNoiseBiome, this.climateSampler());
            return chunk;
        }), Util.backgroundExecutor());
    }

    public abstract Climate.Sampler climateSampler();

    @Override
    public Holder<Biome> getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return this.getBiomeSource().getNoiseBiome(biomeX, biomeY, biomeZ, this.climateSampler());
    }

    public abstract void applyCarvers(WorldGenRegion chunkRegion, long seed, BiomeManager biomeAccess, StructureFeatureManager structureAccessor, ChunkAccess chunk, GenerationStep.Carving generationStep);

    @Nullable
    public Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> findNearestMapFeature(ServerLevel serverLevel, HolderSet<ConfiguredStructureFeature<?, ?>> holderSet, BlockPos center, int radius, boolean skipExistingChunks) {
        Set<Holder<Biome>> set = holderSet.stream().flatMap((holder) -> {
            return holder.value().biomes().stream();
        }).collect(Collectors.toSet());
        if (set.isEmpty()) {
            return null;
        } else {
            Set<Holder<Biome>> set2 = this.runtimeBiomeSource.possibleBiomes();
            if (Collections.disjoint(set2, set)) {
                return null;
            } else {
                Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> pair = null;
                double d = Double.MAX_VALUE;
                Map<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>> map = new Object2ObjectArrayMap<>();

                for(Holder<ConfiguredStructureFeature<?, ?>> holder : holderSet) {
                    if (!set2.stream().noneMatch(holder.value().biomes()::contains)) {
                        for(StructurePlacement structurePlacement : this.getPlacementsForFeature(holder)) {
                            map.computeIfAbsent(structurePlacement, (structurePlacement) -> {
                                return new ObjectArraySet();
                            }).add(holder);
                        }
                    }
                }

                List<Entry<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>>> list = new ArrayList<>(map.size());

                for(Entry<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>> entry : map.entrySet()) {
                    StructurePlacement structurePlacement2 = entry.getKey();
                    if (structurePlacement2 instanceof ConcentricRingsStructurePlacement) {
                        ConcentricRingsStructurePlacement concentricRingsStructurePlacement = (ConcentricRingsStructurePlacement)structurePlacement2;
                        BlockPos blockPos = this.getNearestGeneratedStructure(center, concentricRingsStructurePlacement);
                        double e = center.distSqr(blockPos);
                        if (e < d) {
                            d = e;
                            pair = Pair.of(blockPos, entry.getValue().iterator().next());
                        }
                    } else if (structurePlacement2 instanceof RandomSpreadStructurePlacement) {
                        list.add(entry);
                    }
                }

                if (!list.isEmpty()) {
                    int i = SectionPos.blockToSectionCoord(center.getX());
                    int j = SectionPos.blockToSectionCoord(center.getZ());

                    for(int k = 0; k <= radius; ++k) {
                        boolean bl = false;

                        for(Entry<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>> entry2 : list) {
                            RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement)entry2.getKey();
                            Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> pair2 = getNearestGeneratedStructure(entry2.getValue(), serverLevel, serverLevel.structureFeatureManager(), i, j, k, skipExistingChunks, serverLevel.getSeed(), randomSpreadStructurePlacement);
                            if (pair2 != null) {
                                bl = true;
                                double f = center.distSqr(pair2.getFirst());
                                if (f < d) {
                                    d = f;
                                    pair = pair2;
                                }
                            }
                        }

                        if (bl) {
                            return pair;
                        }
                    }
                }

                return pair;
            }
        }
    }

    @Nullable
    private BlockPos getNearestGeneratedStructure(BlockPos blockPos, ConcentricRingsStructurePlacement concentricRingsStructurePlacement) {
        List<ChunkPos> list = this.getRingPositionsFor(concentricRingsStructurePlacement);
        if (list == null) {
            throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
        } else {
            BlockPos blockPos2 = null;
            double d = Double.MAX_VALUE;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for(ChunkPos chunkPos : list) {
                mutableBlockPos.set(SectionPos.sectionToBlockCoord(chunkPos.x, 8), 32, SectionPos.sectionToBlockCoord(chunkPos.z, 8));
                double e = mutableBlockPos.distSqr(blockPos);
                if (blockPos2 == null) {
                    blockPos2 = new BlockPos(mutableBlockPos);
                    d = e;
                } else if (e < d) {
                    blockPos2 = new BlockPos(mutableBlockPos);
                    d = e;
                }
            }

            return blockPos2;
        }
    }

    @Nullable
    private static Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> getNearestGeneratedStructure(Set<Holder<ConfiguredStructureFeature<?, ?>>> set, LevelReader levelReader, StructureFeatureManager structureFeatureManager, int i, int j, int k, boolean bl, long l, RandomSpreadStructurePlacement randomSpreadStructurePlacement) {
        int m = randomSpreadStructurePlacement.spacing();

        for(int n = -k; n <= k; ++n) {
            boolean bl2 = n == -k || n == k;

            for(int o = -k; o <= k; ++o) {
                boolean bl3 = o == -k || o == k;
                if (bl2 || bl3) {
                    int p = i + m * n;
                    int q = j + m * o;
                    ChunkPos chunkPos = randomSpreadStructurePlacement.getPotentialFeatureChunk(l, p, q);

                    for(Holder<ConfiguredStructureFeature<?, ?>> holder : set) {
                        StructureCheckResult structureCheckResult = structureFeatureManager.checkStructurePresence(chunkPos, holder.value(), bl);
                        if (structureCheckResult != StructureCheckResult.START_NOT_PRESENT) {
                            if (!bl && structureCheckResult == StructureCheckResult.START_PRESENT) {
                                return Pair.of(StructureFeature.getLocatePos(randomSpreadStructurePlacement, chunkPos), holder);
                            }

                            ChunkAccess chunkAccess = levelReader.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
                            StructureStart structureStart = structureFeatureManager.getStartForFeature(SectionPos.bottomOf(chunkAccess), holder.value(), chunkAccess);
                            if (structureStart != null && structureStart.isValid()) {
                                if (bl && structureStart.canBeReferenced()) {
                                    structureFeatureManager.addReference(structureStart);
                                    return Pair.of(StructureFeature.getLocatePos(randomSpreadStructurePlacement, structureStart.getChunkPos()), holder);
                                }

                                if (!bl) {
                                    return Pair.of(StructureFeature.getLocatePos(randomSpreadStructurePlacement, structureStart.getChunkPos()), holder);
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureFeatureManager structureAccessor) {
        ChunkPos chunkPos = chunk.getPos();
        if (!SharedConstants.debugVoidTerrain(chunkPos)) {
            SectionPos sectionPos = SectionPos.of(chunkPos, world.getMinSection());
            BlockPos blockPos = sectionPos.origin();
            Registry<ConfiguredStructureFeature<?, ?>> registry = world.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
            Map<Integer, List<ConfiguredStructureFeature<?, ?>>> map = registry.stream().collect(Collectors.groupingBy((configuredStructureFeature) -> {
                return configuredStructureFeature.feature.step().ordinal();
            }));
            List<BiomeSource.StepFeatureData> list = this.biomeSource.featuresPerStep();
            WorldgenRandom worldgenRandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.seedUniquifier()));
            long l = worldgenRandom.setDecorationSeed(world.getSeed(), blockPos.getX(), blockPos.getZ());
            Set<Biome> set = new ObjectArraySet<>();
            if (this instanceof FlatLevelSource) {
                this.biomeSource.possibleBiomes().stream().map(Holder::value).forEach(set::add);
            } else {
                ChunkPos.rangeClosed(sectionPos.chunk(), 1).forEach((chunkPosx) -> {
                    ChunkAccess chunkAccess = world.getChunk(chunkPosx.x, chunkPosx.z);

                    for(LevelChunkSection levelChunkSection : chunkAccess.getSections()) {
                        levelChunkSection.getBiomes().getAll((holder) -> {
                            set.add(holder.value());
                        });
                    }

                });
                set.retainAll(this.biomeSource.possibleBiomes().stream().map(Holder::value).collect(Collectors.toSet()));
            }

            int i = list.size();

            try {
                Registry<PlacedFeature> registry2 = world.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
                int j = Math.max(GenerationStep.Decoration.values().length, i);

                for(int k = 0; k < j; ++k) {
                    int m = 0;
                    if (structureAccessor.shouldGenerateFeatures()) {
                        for(ConfiguredStructureFeature<?, ?> configuredStructureFeature : map.getOrDefault(k, Collections.emptyList())) {
                            worldgenRandom.setFeatureSeed(l, m, k);
                            Supplier<String> supplier = () -> {
                                return registry.getResourceKey(configuredStructureFeature).map(Object::toString).orElseGet(configuredStructureFeature::toString);
                            };

                            try {
                                world.setCurrentlyGenerating(supplier);
                                structureAccessor.startsForFeature(sectionPos, configuredStructureFeature).forEach((structureStart) -> {
                                    structureStart.placeInChunk(world, structureAccessor, this, worldgenRandom, getWritableArea(chunk), chunkPos);
                                });
                            } catch (Exception var29) {
                                CrashReport crashReport = CrashReport.forThrowable(var29, "Feature placement");
                                crashReport.addCategory("Feature").setDetail("Description", supplier::get);
                                throw new ReportedException(crashReport);
                            }

                            ++m;
                        }
                    }

                    if (k < i) {
                        IntSet intSet = new IntArraySet();

                        for(Biome biome : set) {
                            List<HolderSet<PlacedFeature>> list3 = biome.getGenerationSettings().features();
                            if (k < list3.size()) {
                                HolderSet<PlacedFeature> holderSet = list3.get(k);
                                BiomeSource.StepFeatureData stepFeatureData = list.get(k);
                                holderSet.stream().map(Holder::value).forEach((placedFeaturex) -> {
                                    intSet.add(stepFeatureData.indexMapping().applyAsInt(placedFeaturex));
                                });
                            }
                        }

                        int n = intSet.size();
                        int[] is = intSet.toIntArray();
                        Arrays.sort(is);
                        BiomeSource.StepFeatureData stepFeatureData2 = list.get(k);

                        for(int o = 0; o < n; ++o) {
                            int p = is[o];
                            PlacedFeature placedFeature = stepFeatureData2.features().get(p);
                            Supplier<String> supplier2 = () -> {
                                return registry2.getResourceKey(placedFeature).map(Object::toString).orElseGet(placedFeature::toString);
                            };
                            worldgenRandom.setFeatureSeed(l, p, k);

                            try {
                                world.setCurrentlyGenerating(supplier2);
                                placedFeature.placeWithBiomeCheck(world, this, worldgenRandom, blockPos);
                            } catch (Exception var30) {
                                CrashReport crashReport2 = CrashReport.forThrowable(var30, "Feature placement");
                                crashReport2.addCategory("Feature").setDetail("Description", supplier2::get);
                                throw new ReportedException(crashReport2);
                            }
                        }
                    }
                }

                world.setCurrentlyGenerating((Supplier<String>)null);
            } catch (Exception var31) {
                CrashReport crashReport3 = CrashReport.forThrowable(var31, "Biome decoration");
                crashReport3.addCategory("Generation").setDetail("CenterX", chunkPos.x).setDetail("CenterZ", chunkPos.z).setDetail("Seed", l);
                throw new ReportedException(crashReport3);
            }
        }
    }

    public boolean hasFeatureChunkInRange(ResourceKey<StructureSet> resourceKey, long l, int i, int j, int k) {
        StructureSet structureSet = this.structureSets.get(resourceKey);
        if (structureSet == null) {
            return false;
        } else {
            StructurePlacement structurePlacement = structureSet.placement();

            for(int m = i - k; m <= i + k; ++m) {
                for(int n = j - k; n <= j + k; ++n) {
                    if (structurePlacement.isFeatureChunk(this, l, m, n)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private static BoundingBox getWritableArea(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        LevelHeightAccessor levelHeightAccessor = chunk.getHeightAccessorForGeneration();
        int k = levelHeightAccessor.getMinBuildHeight() + 1;
        int l = levelHeightAccessor.getMaxBuildHeight() - 1;
        return new BoundingBox(i, k, j, i + 15, l, j + 15);
    }

    public abstract void buildSurface(WorldGenRegion region, StructureFeatureManager structures, ChunkAccess chunk);

    public abstract void spawnOriginalMobs(WorldGenRegion region);

    public int getSpawnHeight(LevelHeightAccessor world) {
        return 64;
    }

    public BiomeSource getBiomeSource() {
        return this.runtimeBiomeSource;
    }

    public abstract int getGenDepth();

    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> biome, StructureFeatureManager accessor, MobCategory group, BlockPos pos) {
        Map<ConfiguredStructureFeature<?, ?>, LongSet> map = accessor.getAllStructuresAt(pos);

        for(Entry<ConfiguredStructureFeature<?, ?>, LongSet> entry : map.entrySet()) {
            ConfiguredStructureFeature<?, ?> configuredStructureFeature = entry.getKey();
            StructureSpawnOverride structureSpawnOverride = configuredStructureFeature.spawnOverrides.get(group);
            if (structureSpawnOverride != null) {
                MutableBoolean mutableBoolean = new MutableBoolean(false);
                Predicate<StructureStart> predicate = structureSpawnOverride.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE ? (structureStart) -> {
                    return accessor.structureHasPieceAt(pos, structureStart);
                } : (structureStart) -> {
                    return structureStart.getBoundingBox().isInside(pos);
                };
                accessor.fillStartsForFeature(configuredStructureFeature, entry.getValue(), (structureStart) -> {
                    if (mutableBoolean.isFalse() && predicate.test(structureStart)) {
                        mutableBoolean.setTrue();
                    }

                });
                if (mutableBoolean.isTrue()) {
                    return structureSpawnOverride.spawns();
                }
            }
        }

        return biome.value().getMobSettings().getMobs(group);
    }

    public static Stream<ConfiguredStructureFeature<?, ?>> allConfigurations(Registry<ConfiguredStructureFeature<?, ?>> registry, StructureFeature<?> structureFeature) {
        return registry.stream().filter((configuredStructureFeature) -> {
            return configuredStructureFeature.feature == structureFeature;
        });
    }

    public void createStructures(RegistryAccess registryManager, StructureFeatureManager world, ChunkAccess chunk, StructureManager structureManager, long worldSeed) {
        ChunkPos chunkPos = chunk.getPos();
        SectionPos sectionPos = SectionPos.bottomOf(chunk);
        this.possibleStructureSets().forEach((holder) -> {
            StructurePlacement structurePlacement = holder.value().placement();
            List<StructureSet.StructureSelectionEntry> list = holder.value().structures();

            for(StructureSet.StructureSelectionEntry structureSelectionEntry : list) {
                StructureStart structureStart = world.getStartForFeature(sectionPos, structureSelectionEntry.structure().value(), chunk);
                if (structureStart != null && structureStart.isValid()) {
                    return;
                }
            }

            if (structurePlacement.isFeatureChunk(this, worldSeed, chunkPos.x, chunkPos.z)) {
                if (list.size() == 1) {
                    this.tryGenerateStructure(list.get(0), world, registryManager, structureManager, worldSeed, chunk, chunkPos, sectionPos);
                } else {
                    ArrayList<StructureSet.StructureSelectionEntry> arrayList = new ArrayList<>(list.size());
                    arrayList.addAll(list);
                    WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(0L));
                    worldgenRandom.setLargeFeatureSeed(worldSeed, chunkPos.x, chunkPos.z);
                    int i = 0;

                    for(StructureSet.StructureSelectionEntry structureSelectionEntry2 : arrayList) {
                        i += structureSelectionEntry2.weight();
                    }

                    while(!arrayList.isEmpty()) {
                        int j = worldgenRandom.nextInt(i);
                        int k = 0;

                        for(StructureSet.StructureSelectionEntry structureSelectionEntry3 : arrayList) {
                            j -= structureSelectionEntry3.weight();
                            if (j < 0) {
                                break;
                            }

                            ++k;
                        }

                        StructureSet.StructureSelectionEntry structureSelectionEntry4 = arrayList.get(k);
                        if (this.tryGenerateStructure(structureSelectionEntry4, world, registryManager, structureManager, worldSeed, chunk, chunkPos, sectionPos)) {
                            return;
                        }

                        arrayList.remove(k);
                        i -= structureSelectionEntry4.weight();
                    }

                }
            }
        });
    }

    private boolean tryGenerateStructure(StructureSet.StructureSelectionEntry structureSelectionEntry, StructureFeatureManager structureFeatureManager, RegistryAccess registryAccess, StructureManager structureManager, long l, ChunkAccess chunkAccess, ChunkPos chunkPos, SectionPos sectionPos) {
        ConfiguredStructureFeature<?, ?> configuredStructureFeature = structureSelectionEntry.structure().value();
        int i = fetchReferences(structureFeatureManager, chunkAccess, sectionPos, configuredStructureFeature);
        HolderSet<Biome> holderSet = configuredStructureFeature.biomes();
        Predicate<Holder<Biome>> predicate = (holder) -> {
            return holderSet.contains(this.adjustBiome(holder));
        };
        StructureStart structureStart = configuredStructureFeature.generate(registryAccess, this, this.biomeSource, structureManager, l, chunkPos, i, chunkAccess, predicate);
        if (structureStart.isValid()) {
            structureFeatureManager.setStartForFeature(sectionPos, configuredStructureFeature, structureStart, chunkAccess);
            return true;
        } else {
            return false;
        }
    }

    private static int fetchReferences(StructureFeatureManager structureAccessor, ChunkAccess chunk, SectionPos sectionPos, ConfiguredStructureFeature<?, ?> configuredStructureFeature) {
        StructureStart structureStart = structureAccessor.getStartForFeature(sectionPos, configuredStructureFeature, chunk);
        return structureStart != null ? structureStart.getReferences() : 0;
    }

    protected Holder<Biome> adjustBiome(Holder<Biome> biome) {
        return biome;
    }

    public void createReferences(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        int i = 8;
        ChunkPos chunkPos = chunk.getPos();
        int j = chunkPos.x;
        int k = chunkPos.z;
        int l = chunkPos.getMinBlockX();
        int m = chunkPos.getMinBlockZ();
        SectionPos sectionPos = SectionPos.bottomOf(chunk);

        for(int n = j - 8; n <= j + 8; ++n) {
            for(int o = k - 8; o <= k + 8; ++o) {
                long p = ChunkPos.asLong(n, o);

                for(StructureStart structureStart : world.getChunk(n, o).getAllStarts().values()) {
                    try {
                        if (structureStart.isValid() && structureStart.getBoundingBox().intersects(l, m, l + 15, m + 15)) {
                            structureAccessor.addReferenceForFeature(sectionPos, structureStart.getFeature(), p, chunk);
                            DebugPackets.sendStructurePacket(world, structureStart);
                        }
                    } catch (Exception var21) {
                        CrashReport crashReport = CrashReport.forThrowable(var21, "Generating structure reference");
                        CrashReportCategory crashReportCategory = crashReport.addCategory("Structure");
                        Optional<? extends Registry<ConfiguredStructureFeature<?, ?>>> optional = world.registryAccess().registry(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
                        crashReportCategory.setDetail("Id", () -> {
                            return optional.map((registry) -> {
                                return registry.getKey(structureStart.getFeature()).toString();
                            }).orElse("UNKNOWN");
                        });
                        crashReportCategory.setDetail("Name", () -> {
                            return Registry.STRUCTURE_FEATURE.getKey(structureStart.getFeature().feature).toString();
                        });
                        crashReportCategory.setDetail("Class", () -> {
                            return structureStart.getFeature().getClass().getCanonicalName();
                        });
                        throw new ReportedException(crashReport);
                    }
                }
            }
        }

    }

    public abstract CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk);

    public abstract int getSeaLevel();

    public abstract int getMinY();

    public abstract int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world);

    public abstract NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world);

    public int getFirstFreeHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world) {
        return this.getBaseHeight(x, z, heightmap, world);
    }

    public int getFirstOccupiedHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world) {
        return this.getBaseHeight(x, z, heightmap, world) - 1;
    }

    public void ensureStructuresGenerated() {
        if (!this.hasGeneratedPositions) {
            this.generatePositions();
            this.hasGeneratedPositions = true;
        }

    }

    @Nullable
    public List<ChunkPos> getRingPositionsFor(ConcentricRingsStructurePlacement structurePlacement) {
        this.ensureStructuresGenerated();
        CompletableFuture<List<ChunkPos>> completableFuture = this.ringPositions.get(structurePlacement);
        return completableFuture != null ? completableFuture.join() : null;
    }

    private List<StructurePlacement> getPlacementsForFeature(Holder<ConfiguredStructureFeature<?, ?>> holder) {
        this.ensureStructuresGenerated();
        return this.placementsForFeature.getOrDefault(holder.value(), List.of());
    }

    public abstract void addDebugScreenInfo(List<String> text, BlockPos pos);

    static {
        Registry.register(Registry.CHUNK_GENERATOR, "noise", NoiseBasedChunkGenerator.CODEC);
        Registry.register(Registry.CHUNK_GENERATOR, "flat", FlatLevelSource.CODEC);
        Registry.register(Registry.CHUNK_GENERATOR, "debug", DebugLevelSource.CODEC);
        LOGGER = LogUtils.getLogger();
        CODEC = Registry.CHUNK_GENERATOR.byNameCodec().dispatchStable(ChunkGenerator::codec, Function.identity());
    }
}
