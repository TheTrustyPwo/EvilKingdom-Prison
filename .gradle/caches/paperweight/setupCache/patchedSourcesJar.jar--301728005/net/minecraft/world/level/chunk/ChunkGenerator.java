package net.minecraft.world.level.chunk;

import com.google.common.base.Stopwatch;
import com.mojang.datafixers.Products.P1;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
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
import net.minecraft.core.Vec3i;
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
    private final Map<ConfiguredStructureFeature<?, ?>, List<StructurePlacement>> placementsForFeature;
    private final Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> ringPositions;
    private boolean hasGeneratedPositions;
    /** @deprecated */
    @Deprecated
    public final long ringPlacementSeed;
    public org.spigotmc.SpigotWorldConfig conf; // Spigot

    protected static final <T extends ChunkGenerator> P1<Mu<T>, Registry<StructureSet>> commonCodec(Instance<T> instance) {
        return instance.group(RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY).forGetter((chunkgenerator) -> {
            return chunkgenerator.structureSets;
        }));
    }

    public ChunkGenerator(Registry<StructureSet> iregistry, Optional<HolderSet<StructureSet>> optional, BiomeSource worldchunkmanager) {
        this(iregistry, optional, worldchunkmanager, worldchunkmanager, 0L);
    }

    public ChunkGenerator(Registry<StructureSet> iregistry, Optional<HolderSet<StructureSet>> optional, BiomeSource worldchunkmanager, BiomeSource worldchunkmanager1, long i) {
        this.placementsForFeature = new Object2ObjectOpenHashMap();
        this.ringPositions = new Object2ObjectArrayMap();
        this.structureSets = iregistry;
        this.biomeSource = worldchunkmanager;
        this.runtimeBiomeSource = worldchunkmanager1;
        this.structureOverrides = optional;
        this.ringPlacementSeed = i;
    }

    public Stream<Holder<StructureSet>> possibleStructureSets() {
        return this.structureOverrides.isPresent() ? ((HolderSet) this.structureOverrides.get()).stream() : this.structureSets.holders().map(Holder::hackyErase);
    }

    // Spigot start
    private Stream<StructureSet> possibleStructureSetsSpigot() {
        return this.possibleStructureSets().map(Holder::value).map((structureset) -> {
            if (structureset.placement() instanceof RandomSpreadStructurePlacement randomConfig) {
                String name = this.structureSets.getKey(structureset).getPath();
                int seed = randomConfig.salt();

                switch (name) {
                    case "desert_pyramids":
                        seed = conf.desertSeed;
                        break;
                    case "end_cities":
                        seed = conf.endCitySeed;
                        break;
                    case "nether_complexes":
                        seed = conf.netherSeed;
                        break;
                    case "igloos":
                        seed = conf.iglooSeed;
                        break;
                    case "jungle_temples":
                        seed = conf.jungleSeed;
                        break;
                    case "woodland_mansions":
                        seed = conf.mansionSeed;
                        break;
                    case "ocean_monuments":
                        seed = conf.monumentSeed;
                        break;
                    case "nether_fossils":
                        seed = conf.fossilSeed;
                        break;
                    case "ocean_ruins":
                        seed = conf.oceanSeed;
                        break;
                    case "pillager_outposts":
                        seed = conf.outpostSeed;
                        break;
                    case "ruined_portals":
                        seed = conf.portalSeed;
                        break;
                    case "shipwrecks":
                        seed = conf.shipwreckSeed;
                        break;
                    case "swamp_huts":
                        seed = conf.swampSeed;
                        break;
                    case "villages":
                        seed = conf.villageSeed;
                        break;
                }

                structureset = new StructureSet(structureset.structures(), new RandomSpreadStructurePlacement(randomConfig.spacing(), randomConfig.separation(), randomConfig.spreadType(), seed, randomConfig.locateOffset()));
            }
            return structureset;
        });
    }
    // Spigot end

    private void generatePositions() {
        Set<Holder<Biome>> set = this.runtimeBiomeSource.possibleBiomes();

        // Spigot start
        this.possibleStructureSetsSpigot().forEach((holder) -> {
            StructureSet structureset = (StructureSet) holder;
            // Spigot end
            Iterator iterator = structureset.structures().iterator();

            while (iterator.hasNext()) {
                StructureSet.StructureSelectionEntry structureset_a = (StructureSet.StructureSelectionEntry) iterator.next();

                ((List) this.placementsForFeature.computeIfAbsent((ConfiguredStructureFeature) structureset_a.structure().value(), (structurefeature) -> {
                    return new ArrayList();
                })).add(structureset.placement());
            }

            StructurePlacement structureplacement = structureset.placement();

            if (structureplacement instanceof ConcentricRingsStructurePlacement) {
                ConcentricRingsStructurePlacement concentricringsstructureplacement = (ConcentricRingsStructurePlacement) structureplacement;

                if (structureset.structures().stream().anyMatch((structureset_a1) -> {
                    Objects.requireNonNull(set);
                    return structureset_a1.generatesInMatchingBiome(set::contains);
                })) {
                    this.ringPositions.put(concentricringsstructureplacement, this.generateRingPositions(holder, concentricringsstructureplacement));
                }
            }

        });
    }

    private CompletableFuture<List<ChunkPos>> generateRingPositions(StructureSet holder, ConcentricRingsStructurePlacement concentricringsstructureplacement) { // Spigot
        return concentricringsstructureplacement.count() == 0 ? CompletableFuture.completedFuture(List.of()) : CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("placement calculation", () -> {
            Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
            List<ChunkPos> list = new ArrayList();
            Set<Holder<Biome>> set = (Set) ((StructureSet) holder).structures().stream().flatMap((structureset_a) -> { // Spigot
                return ((ConfiguredStructureFeature) structureset_a.structure().value()).biomes().stream();
            }).collect(Collectors.toSet());
            int i = concentricringsstructureplacement.distance();
            int j = concentricringsstructureplacement.count();
            int k = concentricringsstructureplacement.spread();
            Random random = new Random();

            // Paper start
            if (this.conf.strongholdSeed != null && this.structureSets.getResourceKey(holder).orElse(null) == net.minecraft.world.level.levelgen.structure.BuiltinStructureSets.STRONGHOLDS) {
                random.setSeed(this.conf.strongholdSeed);
            } else {
            // Paper end
            random.setSeed(this.ringPlacementSeed);
            } // Paper
            double d0 = random.nextDouble() * 3.141592653589793D * 2.0D;
            int l = 0;
            int i1 = 0;

            for (int j1 = 0; j1 < j; ++j1) {
                double d1 = (double) (4 * i + i * i1 * 6) + (random.nextDouble() - 0.5D) * (double) i * 2.5D;
                int k1 = (int) Math.round(Math.cos(d0) * d1);
                int l1 = (int) Math.round(Math.sin(d0) * d1);
                BiomeSource worldchunkmanager = this.biomeSource;
                int i2 = SectionPos.sectionToBlockCoord(k1, 8);
                int j2 = SectionPos.sectionToBlockCoord(l1, 8);

                Objects.requireNonNull(set);
                Pair<BlockPos, Holder<Biome>> pair = worldchunkmanager.findBiomeHorizontal(i2, 0, j2, 112, set::contains, random, this.climateSampler());

                if (pair != null) {
                    BlockPos blockposition = (BlockPos) pair.getFirst();

                    k1 = SectionPos.blockToSectionCoord(blockposition.getX());
                    l1 = SectionPos.blockToSectionCoord(blockposition.getZ());
                }

                list.add(new ChunkPos(k1, l1));
                d0 += 6.283185307179586D / (double) k;
                ++l;
                if (l == k) {
                    ++i1;
                    l = 0;
                    k += 2 * k / (i1 + 1);
                    k = Math.min(k, j - j1);
                    d0 += random.nextDouble() * 3.141592653589793D * 2.0D;
                }
            }

            double d2 = (double) stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) / 1000.0D;

            ChunkGenerator.LOGGER.debug("Calculation for {} took {}s", holder, d2);
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
            BiomeSource worldchunkmanager = this.runtimeBiomeSource;

            Objects.requireNonNull(this.runtimeBiomeSource);
            chunk.fillBiomesFromNoise(worldchunkmanager::getNoiseBiome, this.climateSampler());
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
    public Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> findNearestMapFeature(ServerLevel worldserver, HolderSet<ConfiguredStructureFeature<?, ?>> holderset, BlockPos center, int radius, boolean skipExistingChunks) {
        // Paper start - StructureLocateEvent
        final org.bukkit.World world = worldserver.getWorld();
        final org.bukkit.Location origin = net.minecraft.server.MCUtil.toLocation(worldserver, center);
        final var paperRegistry = io.papermc.paper.registry.PaperRegistry.getRegistry(io.papermc.paper.registry.RegistryKey.CONFIGURED_STRUCTURE_REGISTRY);
        final List<io.papermc.paper.world.structure.ConfiguredStructure> configuredStructures = new ArrayList<>();
        for (Holder<net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature<?, ?>> holder : holderset) {
            configuredStructures.add(paperRegistry.convertToApi(holder));
        }
        final io.papermc.paper.event.world.StructuresLocateEvent event = new io.papermc.paper.event.world.StructuresLocateEvent(world, origin, configuredStructures, radius, skipExistingChunks);
        if (!event.callEvent()) {
            return null;
        }
        if (event.getResult() != null) {
            return Pair.of(net.minecraft.server.MCUtil.toBlockPosition(event.getResult().position()), paperRegistry.getMinecraftHolder(event.getResult().configuredStructure()));
        }
        center = net.minecraft.server.MCUtil.toBlockPosition(event.getOrigin());
        radius = event.getRadius();
        skipExistingChunks = event.shouldFindUnexplored();
        holderset = HolderSet.direct(paperRegistry::getMinecraftHolder, event.getConfiguredStructures());
        // Paper end
        Set<Holder<Biome>> set = (Set) holderset.stream().flatMap((holder) -> {
            return ((ConfiguredStructureFeature) holder.value()).biomes().stream();
        }).collect(Collectors.toSet());

        if (set.isEmpty()) {
            return null;
        } else {
            Set<Holder<Biome>> set1 = this.runtimeBiomeSource.possibleBiomes();

            if (Collections.disjoint(set1, set)) {
                return null;
            } else {
                Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> pair = null;
                double d0 = Double.MAX_VALUE;
                Map<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>> map = new Object2ObjectArrayMap();
                Iterator iterator = holderset.iterator();

                StructurePlacement structureplacement;

                while (iterator.hasNext()) {
                    Holder<ConfiguredStructureFeature<?, ?>> holder = (Holder) iterator.next();
                    Stream<Holder<Biome>> stream = set1.stream(); // CraftBukkit - decompile error
                    HolderSet holderset1 = ((ConfiguredStructureFeature) holder.value()).biomes();

                    Objects.requireNonNull(holderset1);
                    if (!stream.noneMatch(holderset1::contains)) {
                        Iterator iterator1 = this.getPlacementsForFeature(holder).iterator();

                        while (iterator1.hasNext()) {
                            structureplacement = (StructurePlacement) iterator1.next();
                            ((Set) map.computeIfAbsent(structureplacement, (structureplacement1) -> {
                                return new ObjectArraySet();
                            })).add(holder);
                        }
                    }
                }

                List<Entry<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>>> list = new ArrayList(map.size());
                Iterator iterator2 = map.entrySet().iterator();

                while (iterator2.hasNext()) {
                    Entry<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>> entry = (Entry) iterator2.next();

                    structureplacement = (StructurePlacement) entry.getKey();
                    if (structureplacement instanceof ConcentricRingsStructurePlacement) {
                        ConcentricRingsStructurePlacement concentricringsstructureplacement = (ConcentricRingsStructurePlacement) structureplacement;
                        BlockPos blockposition1 = this.getNearestGeneratedStructure(center, concentricringsstructureplacement);
                        double d1 = center.distSqr(blockposition1);

                        if (d1 < d0) {
                            d0 = d1;
                            pair = Pair.of(blockposition1, (Holder) ((Set) entry.getValue()).iterator().next());
                        }
                    } else if (structureplacement instanceof RandomSpreadStructurePlacement) {
                        list.add(entry);
                    }
                }

                if (!list.isEmpty()) {
                    int j = SectionPos.blockToSectionCoord(center.getX());
                    int k = SectionPos.blockToSectionCoord(center.getZ());

                    for (int l = 0; l <= radius; ++l) {
                        boolean flag1 = false;
                        Iterator iterator3 = list.iterator();

                        while (iterator3.hasNext()) {
                            Entry<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>> entry1 = (Entry) iterator3.next();
                            RandomSpreadStructurePlacement randomspreadstructureplacement = (RandomSpreadStructurePlacement) entry1.getKey();
                            Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> pair1 = ChunkGenerator.getNearestGeneratedStructure((Set) entry1.getValue(), worldserver, worldserver.structureFeatureManager(), j, k, l, skipExistingChunks, worldserver.getSeed(), randomspreadstructureplacement);

                            if (pair1 != null) {
                                flag1 = true;
                                double d2 = center.distSqr((Vec3i) pair1.getFirst());

                                if (d2 < d0) {
                                    d0 = d2;
                                    pair = pair1;
                                }
                            }
                        }

                        if (flag1) {
                            return pair;
                        }
                    }
                }

                return pair;
            }
        }
    }

    @Nullable
    private BlockPos getNearestGeneratedStructure(BlockPos blockposition, ConcentricRingsStructurePlacement concentricringsstructureplacement) {
        List<ChunkPos> list = this.getRingPositionsFor(concentricringsstructureplacement);

        if (list == null) {
            throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
        } else {
            BlockPos blockposition1 = null;
            double d0 = Double.MAX_VALUE;
            BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                ChunkPos chunkcoordintpair = (ChunkPos) iterator.next();

                blockposition_mutableblockposition.set(SectionPos.sectionToBlockCoord(chunkcoordintpair.x, 8), 32, SectionPos.sectionToBlockCoord(chunkcoordintpair.z, 8));
                double d1 = blockposition_mutableblockposition.distSqr(blockposition);

                if (blockposition1 == null) {
                    blockposition1 = new BlockPos(blockposition_mutableblockposition);
                    d0 = d1;
                } else if (d1 < d0) {
                    blockposition1 = new BlockPos(blockposition_mutableblockposition);
                    d0 = d1;
                }
            }

            return blockposition1;
        }
    }

    @Nullable
    private static Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> getNearestGeneratedStructure(Set<Holder<ConfiguredStructureFeature<?, ?>>> set, LevelReader iworldreader, StructureFeatureManager structuremanager, int i, int j, int k, boolean flag, long l, RandomSpreadStructurePlacement randomspreadstructureplacement) {
        int i1 = randomspreadstructureplacement.spacing();

        for (int j1 = -k; j1 <= k; ++j1) {
            boolean flag1 = j1 == -k || j1 == k;

            for (int k1 = -k; k1 <= k; ++k1) {
                boolean flag2 = k1 == -k || k1 == k;

                if (flag1 || flag2) {
                    int l1 = i + i1 * j1;
                    int i2 = j + i1 * k1;
                    ChunkPos chunkcoordintpair = randomspreadstructureplacement.getPotentialFeatureChunk(l, l1, i2);
                    if (!iworldreader.getWorldBorder().isChunkInBounds(chunkcoordintpair.x, chunkcoordintpair.z)) { continue; } // Paper
                    Iterator iterator = set.iterator();

                    while (iterator.hasNext()) {
                        Holder<ConfiguredStructureFeature<?, ?>> holder = (Holder) iterator.next();
                        StructureCheckResult structurecheckresult = structuremanager.checkStructurePresence(chunkcoordintpair, (ConfiguredStructureFeature) holder.value(), flag);

                        if (structurecheckresult != StructureCheckResult.START_NOT_PRESENT) {
                            if (!flag && structurecheckresult == StructureCheckResult.START_PRESENT) {
                                return Pair.of(StructureFeature.getLocatePos(randomspreadstructureplacement, chunkcoordintpair), holder);
                            }

                            ChunkAccess ichunkaccess = iworldreader.getChunk(chunkcoordintpair.x, chunkcoordintpair.z, ChunkStatus.STRUCTURE_STARTS);
                            StructureStart structurestart = structuremanager.getStartForFeature(SectionPos.bottomOf(ichunkaccess), (ConfiguredStructureFeature) holder.value(), ichunkaccess);

                            if (structurestart != null && structurestart.isValid()) {
                                if (flag && structurestart.canBeReferenced()) {
                                    structuremanager.addReference(structurestart);
                                    return Pair.of(StructureFeature.getLocatePos(randomspreadstructureplacement, structurestart.getChunkPos()), holder);
                                }

                                if (!flag) {
                                    return Pair.of(StructureFeature.getLocatePos(randomspreadstructureplacement, structurestart.getChunkPos()), holder);
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public void addVanillaDecorations(WorldGenLevel generatoraccessseed, ChunkAccess ichunkaccess, StructureFeatureManager structuremanager) { // CraftBukkit
        ChunkPos chunkcoordintpair = ichunkaccess.getPos();

        if (!SharedConstants.debugVoidTerrain(chunkcoordintpair)) {
            SectionPos sectionposition = SectionPos.of(chunkcoordintpair, generatoraccessseed.getMinSection());
            BlockPos blockposition = sectionposition.origin();
            Registry<ConfiguredStructureFeature<?, ?>> iregistry = generatoraccessseed.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
            Map<Integer, List<ConfiguredStructureFeature<?, ?>>> map = (Map) iregistry.stream().collect(Collectors.groupingBy((structurefeature) -> {
                return structurefeature.feature.step().ordinal();
            }));
            List<BiomeSource.StepFeatureData> list = this.biomeSource.featuresPerStep();
            WorldgenRandom seededrandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.seedUniquifier()));
            long i = seededrandom.setDecorationSeed(generatoraccessseed.getSeed(), blockposition.getX(), blockposition.getZ());
            Set<Biome> set = new ObjectArraySet();

            if (this instanceof FlatLevelSource) {
                Stream<Biome> stream = this.biomeSource.possibleBiomes().stream().map(Holder::value); // CraftBukkit - decompile error

                Objects.requireNonNull(set);
                stream.forEach(set::add);
            } else {
                ChunkPos.rangeClosed(sectionposition.chunk(), 1).forEach((chunkcoordintpair1) -> {
                    ChunkAccess ichunkaccess1 = generatoraccessseed.getChunk(chunkcoordintpair1.x, chunkcoordintpair1.z);
                    LevelChunkSection[] achunksection = ichunkaccess1.getSections();
                    int j = achunksection.length;

                    for (int k = 0; k < j; ++k) {
                        LevelChunkSection chunksection = achunksection[k];

                        chunksection.getBiomes().getAll((holder) -> {
                            set.add((Biome) holder.value());
                        });
                    }

                });
                set.retainAll((Collection) this.biomeSource.possibleBiomes().stream().map(Holder::value).collect(Collectors.toSet()));
            }

            int j = list.size();

            try {
                Registry<PlacedFeature> iregistry1 = generatoraccessseed.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY); // Paper - diff on change
                int k = Math.max(GenerationStep.Decoration.values().length, j);

                for (int l = 0; l < k; ++l) {
                    int i1 = 0;
                    Iterator iterator;
                    CrashReportCategory crashreportsystemdetails;

                    if (structuremanager.shouldGenerateFeatures()) {
                        List<ConfiguredStructureFeature<?, ?>> list1 = (List) map.getOrDefault(l, Collections.emptyList());

                        for (iterator = list1.iterator(); iterator.hasNext(); ++i1) {
                            ConfiguredStructureFeature<?, ?> structurefeature = (ConfiguredStructureFeature) iterator.next();

                            seededrandom.setFeatureSeed(i, i1, l);
                            Supplier<String> supplier = () -> { // CraftBukkit - decompile error
                                Optional optional = iregistry.getResourceKey(structurefeature).map(Object::toString);

                                Objects.requireNonNull(structurefeature);
                                return (String) optional.orElseGet(structurefeature::toString);
                            };

                            try {
                                generatoraccessseed.setCurrentlyGenerating(supplier);
                                structuremanager.startsForFeature(sectionposition, structurefeature).forEach((structurestart) -> {
                                    structurestart.placeInChunk(generatoraccessseed, structuremanager, this, seededrandom, ChunkGenerator.getWritableArea(ichunkaccess), chunkcoordintpair);
                                });
                            } catch (Exception exception) {
                                CrashReport crashreport = CrashReport.forThrowable(exception, "Feature placement");

                                crashreportsystemdetails = crashreport.addCategory("Feature");
                                Objects.requireNonNull(supplier);
                                crashreportsystemdetails.setDetail("Description", supplier::get);
                                throw new ReportedException(crashreport);
                            }
                        }
                    }

                    if (l < j) {
                        IntArraySet intarrayset = new IntArraySet();

                        iterator = set.iterator();

                        while (iterator.hasNext()) {
                            Biome biomebase = (Biome) iterator.next();
                            List<HolderSet<PlacedFeature>> list2 = biomebase.getGenerationSettings().features();

                            if (l < list2.size()) {
                                HolderSet<PlacedFeature> holderset = (HolderSet) list2.get(l);
                                BiomeSource.StepFeatureData worldchunkmanager_b = (BiomeSource.StepFeatureData) list.get(l);

                                holderset.stream().map(Holder::value).forEach((placedfeature) -> {
                                    intarrayset.add(worldchunkmanager_b.indexMapping().applyAsInt(placedfeature));
                                });
                            }
                        }

                        int j1 = intarrayset.size();
                        int[] aint = intarrayset.toIntArray();

                        Arrays.sort(aint);
                        BiomeSource.StepFeatureData worldchunkmanager_b1 = (BiomeSource.StepFeatureData) list.get(l);

                        for (int k1 = 0; k1 < j1; ++k1) {
                            int l1 = aint[k1];
                            PlacedFeature placedfeature = (PlacedFeature) worldchunkmanager_b1.features().get(l1);
                            Supplier<String> supplier1 = () -> {
                                Optional optional = iregistry1.getResourceKey(placedfeature).map(Object::toString);

                                Objects.requireNonNull(placedfeature);
                                return (String) optional.orElseGet(placedfeature::toString);
                            };

                            // Paper start - change populationSeed used in random
                            long featurePopulationSeed = i;
                            final net.minecraft.resources.ResourceLocation location = iregistry1.getKey(placedfeature);
                            final long configFeatureSeed = generatoraccessseed.getMinecraftWorld().paperConfig.featureSeeds.getLong(location);
                            if (configFeatureSeed != -1) {
                                featurePopulationSeed = seededrandom.setDecorationSeed(configFeatureSeed, blockposition.getX(), blockposition.getZ()); // See seededrandom.setDecorationSeed from above
                            }
                            seededrandom.setFeatureSeed(featurePopulationSeed, l1, l);
                            // Paper end

                            try {
                                generatoraccessseed.setCurrentlyGenerating(supplier1);
                                placedfeature.placeWithBiomeCheck(generatoraccessseed, this, seededrandom, blockposition);
                            } catch (Exception exception1) {
                                CrashReport crashreport1 = CrashReport.forThrowable(exception1, "Feature placement");

                                crashreportsystemdetails = crashreport1.addCategory("Feature");
                                Objects.requireNonNull(supplier1);
                                crashreportsystemdetails.setDetail("Description", supplier1::get);
                                throw new ReportedException(crashreport1);
                            }
                        }
                    }
                }

                generatoraccessseed.setCurrentlyGenerating((Supplier) null);
            } catch (Exception exception2) {
                CrashReport crashreport2 = CrashReport.forThrowable(exception2, "Biome decoration");

                crashreport2.addCategory("Generation").setDetail("CenterX", (Object) chunkcoordintpair.x).setDetail("CenterZ", (Object) chunkcoordintpair.z).setDetail("Seed", (Object) i);
                throw new ReportedException(crashreport2);
            }
        }
    }

    public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureFeatureManager structureAccessor) {
        // CraftBukkit start
        this.applyBiomeDecoration(world, chunk, structureAccessor, true);
    }

    public void applyBiomeDecoration(WorldGenLevel generatoraccessseed, ChunkAccess ichunkaccess, StructureFeatureManager structuremanager, boolean vanilla) {
        if (vanilla) {
            this.addVanillaDecorations(generatoraccessseed, ichunkaccess, structuremanager);
        }

        org.bukkit.World world = generatoraccessseed.getMinecraftWorld().getWorld();
        // only call when a populator is present (prevents unnecessary entity conversion)
        if (!world.getPopulators().isEmpty()) {
            org.bukkit.craftbukkit.v1_18_R2.generator.CraftLimitedRegion limitedRegion = new org.bukkit.craftbukkit.v1_18_R2.generator.CraftLimitedRegion(generatoraccessseed, ichunkaccess.getPos());
            int x = ichunkaccess.getPos().x;
            int z = ichunkaccess.getPos().z;
            for (org.bukkit.generator.BlockPopulator populator : world.getPopulators()) {
                WorldgenRandom seededrandom = new WorldgenRandom(new net.minecraft.world.level.levelgen.LegacyRandomSource(generatoraccessseed.getSeed()));
                seededrandom.setDecorationSeed(generatoraccessseed.getSeed(), x, z);
                populator.populate(world, seededrandom, x, z, limitedRegion);
            }
            limitedRegion.saveEntities();
            limitedRegion.breakLink();
        }
        // CraftBukkit end
    }

    public boolean hasFeatureChunkInRange(ResourceKey<StructureSet> resourcekey, long i, int j, int k, int l) {
        StructureSet structureset = (StructureSet) this.structureSets.get(resourcekey);

        if (structureset == null) {
            return false;
        } else {
            StructurePlacement structureplacement = structureset.placement();

            for (int i1 = j - l; i1 <= j + l; ++i1) {
                for (int j1 = k - l; j1 <= k + l; ++j1) {
                    if (structureplacement.isFeatureChunk(this, i, i1, j1)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private static BoundingBox getWritableArea(ChunkAccess chunk) {
        ChunkPos chunkcoordintpair = chunk.getPos();
        int i = chunkcoordintpair.getMinBlockX();
        int j = chunkcoordintpair.getMinBlockZ();
        LevelHeightAccessor levelheightaccessor = chunk.getHeightAccessorForGeneration();
        int k = levelheightaccessor.getMinBuildHeight() + 1;
        int l = levelheightaccessor.getMaxBuildHeight() - 1;

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
        Iterator iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<ConfiguredStructureFeature<?, ?>, LongSet> entry = (Entry) iterator.next();
            ConfiguredStructureFeature<?, ?> structurefeature = (ConfiguredStructureFeature) entry.getKey();
            StructureSpawnOverride structurespawnoverride = (StructureSpawnOverride) structurefeature.spawnOverrides.get(group);

            if (structurespawnoverride != null) {
                MutableBoolean mutableboolean = new MutableBoolean(false);
                Predicate<StructureStart> predicate = structurespawnoverride.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE ? (structurestart) -> {
                    return accessor.structureHasPieceAt(pos, structurestart);
                } : (structurestart) -> {
                    return structurestart.getBoundingBox().isInside(pos);
                };

                accessor.fillStartsForFeature(structurefeature, (LongSet) entry.getValue(), (structurestart) -> {
                    if (mutableboolean.isFalse() && predicate.test(structurestart)) {
                        mutableboolean.setTrue();
                    }

                });
                if (mutableboolean.isTrue()) {
                    return structurespawnoverride.spawns();
                }
            }
        }

        return ((Biome) biome.value()).getMobSettings().getMobs(group);
    }

    public static Stream<ConfiguredStructureFeature<?, ?>> allConfigurations(Registry<ConfiguredStructureFeature<?, ?>> iregistry, StructureFeature<?> structuregenerator) {
        return iregistry.stream().filter((structurefeature) -> {
            return structurefeature.feature == structuregenerator;
        });
    }

    public void createStructures(RegistryAccess registryManager, StructureFeatureManager world, ChunkAccess chunk, StructureManager structureManager, long worldSeed) {
        ChunkPos chunkcoordintpair = chunk.getPos();
        SectionPos sectionposition = SectionPos.bottomOf(chunk);

        // Spigot start
        this.possibleStructureSetsSpigot().forEach((holder) -> {
            StructurePlacement structureplacement = ((StructureSet) holder).placement();
            List<StructureSet.StructureSelectionEntry> list = ((StructureSet) holder).structures();
            // Spigot end
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                StructureSet.StructureSelectionEntry structureset_a = (StructureSet.StructureSelectionEntry) iterator.next();
                StructureStart structurestart = world.getStartForFeature(sectionposition, (ConfiguredStructureFeature) structureset_a.structure().value(), chunk);

                if (structurestart != null && structurestart.isValid()) {
                    return;
                }
            }

            if (structureplacement.isFeatureChunk(this, worldSeed, chunkcoordintpair.x, chunkcoordintpair.z)) {
                if (list.size() == 1) {
                    this.tryGenerateStructure((StructureSet.StructureSelectionEntry) list.get(0), world, registryManager, structureManager, worldSeed, chunk, chunkcoordintpair, sectionposition);
                } else {
                    ArrayList<StructureSet.StructureSelectionEntry> arraylist = new ArrayList(list.size());

                    arraylist.addAll(list);
                    WorldgenRandom seededrandom = new WorldgenRandom(new LegacyRandomSource(0L));

                    seededrandom.setLargeFeatureSeed(worldSeed, chunkcoordintpair.x, chunkcoordintpair.z);
                    int j = 0;

                    StructureSet.StructureSelectionEntry structureset_a1;

                    for (Iterator iterator1 = arraylist.iterator(); iterator1.hasNext(); j += structureset_a1.weight()) {
                        structureset_a1 = (StructureSet.StructureSelectionEntry) iterator1.next();
                    }

                    while (!arraylist.isEmpty()) {
                        int k = seededrandom.nextInt(j);
                        int l = 0;
                        Iterator iterator2 = arraylist.iterator();

                        while (true) {
                            if (iterator2.hasNext()) {
                                StructureSet.StructureSelectionEntry structureset_a2 = (StructureSet.StructureSelectionEntry) iterator2.next();

                                k -= structureset_a2.weight();
                                if (k >= 0) {
                                    ++l;
                                    continue;
                                }
                            }

                            StructureSet.StructureSelectionEntry structureset_a3 = (StructureSet.StructureSelectionEntry) arraylist.get(l);

                            if (this.tryGenerateStructure(structureset_a3, world, registryManager, structureManager, worldSeed, chunk, chunkcoordintpair, sectionposition)) {
                                return;
                            }

                            arraylist.remove(l);
                            j -= structureset_a3.weight();
                            break;
                        }
                    }

                }
            }
        });
    }

    private boolean tryGenerateStructure(StructureSet.StructureSelectionEntry structureset_a, StructureFeatureManager structuremanager, RegistryAccess iregistrycustom, StructureManager definedstructuremanager, long i, ChunkAccess ichunkaccess, ChunkPos chunkcoordintpair, SectionPos sectionposition) {
        ConfiguredStructureFeature<?, ?> structurefeature = (ConfiguredStructureFeature) structureset_a.structure().value();
        int j = ChunkGenerator.fetchReferences(structuremanager, ichunkaccess, sectionposition, structurefeature);
        HolderSet<Biome> holderset = structurefeature.biomes();
        Predicate<Holder<Biome>> predicate = (holder) -> {
            return holderset.contains(this.adjustBiome(holder));
        };
        StructureStart structurestart = structurefeature.generate(iregistrycustom, this, this.biomeSource, definedstructuremanager, i, chunkcoordintpair, j, ichunkaccess, predicate);

        if (structurestart.isValid()) {
            structuremanager.setStartForFeature(sectionposition, structurefeature, structurestart, ichunkaccess);
            return true;
        } else {
            return false;
        }
    }

    private static int fetchReferences(StructureFeatureManager structureAccessor, ChunkAccess chunk, SectionPos sectionPos, ConfiguredStructureFeature<?, ?> structurefeature) {
        StructureStart structurestart = structureAccessor.getStartForFeature(sectionPos, structurefeature, chunk);

        return structurestart != null ? structurestart.getReferences() : 0;
    }

    protected Holder<Biome> adjustBiome(Holder<Biome> biome) {
        return biome;
    }

    public void createReferences(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        boolean flag = true;
        ChunkPos chunkcoordintpair = chunk.getPos();
        int i = chunkcoordintpair.x;
        int j = chunkcoordintpair.z;
        int k = chunkcoordintpair.getMinBlockX();
        int l = chunkcoordintpair.getMinBlockZ();
        SectionPos sectionposition = SectionPos.bottomOf(chunk);

        for (int i1 = i - 8; i1 <= i + 8; ++i1) {
            for (int j1 = j - 8; j1 <= j + 8; ++j1) {
                long k1 = ChunkPos.asLong(i1, j1);
                Iterator iterator = world.getChunk(i1, j1).getAllStarts().values().iterator();

                while (iterator.hasNext()) {
                    StructureStart structurestart = (StructureStart) iterator.next();

                    try {
                        if (structurestart.isValid() && structurestart.getBoundingBox().intersects(k, l, k + 15, l + 15)) {
                            structureAccessor.addReferenceForFeature(sectionposition, structurestart.getFeature(), k1, chunk);
                            DebugPackets.sendStructurePacket(world, structurestart);
                        }
                    } catch (Exception exception) {
                        CrashReport crashreport = CrashReport.forThrowable(exception, "Generating structure reference");
                        CrashReportCategory crashreportsystemdetails = crashreport.addCategory("Structure");
                        Optional<? extends Registry<ConfiguredStructureFeature<?, ?>>> optional = world.registryAccess().registry(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);

                        crashreportsystemdetails.setDetail("Id", () -> {
                            return (String) optional.map((iregistry) -> {
                                return iregistry.getKey(structurestart.getFeature()).toString();
                            }).orElse("UNKNOWN");
                        });
                        crashreportsystemdetails.setDetail("Name", () -> {
                            return Registry.STRUCTURE_FEATURE.getKey(structurestart.getFeature().feature).toString();
                        });
                        crashreportsystemdetails.setDetail("Class", () -> {
                            return structurestart.getFeature().getClass().getCanonicalName();
                        });
                        throw new ReportedException(crashreport);
                    }
                }
            }
        }

    }

    public abstract CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk);

    public abstract int getSeaLevel();

    public abstract int getMinY();

    public abstract int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world);

    public abstract net.minecraft.world.level.NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world);

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
        CompletableFuture<List<ChunkPos>> completablefuture = (CompletableFuture) this.ringPositions.get(structurePlacement);

        return completablefuture != null ? (List) completablefuture.join() : null;
    }

    private List<StructurePlacement> getPlacementsForFeature(Holder<ConfiguredStructureFeature<?, ?>> holder) {
        this.ensureStructuresGenerated();
        return (List) this.placementsForFeature.getOrDefault(holder.value(), List.of());
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
