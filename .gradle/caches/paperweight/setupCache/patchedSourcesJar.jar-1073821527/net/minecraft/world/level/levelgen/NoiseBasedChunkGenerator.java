package net.minecraft.world.level.levelgen;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class NoiseBasedChunkGenerator extends ChunkGenerator {
    public static final Codec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> {
        return commonCodec(instance).and(instance.group(RegistryOps.retrieveRegistry(Registry.NOISE_REGISTRY).forGetter((generator) -> {
            return generator.noises;
        }), BiomeSource.CODEC.fieldOf("biome_source").forGetter((generator) -> {
            return generator.biomeSource;
        }), Codec.LONG.fieldOf("seed").stable().forGetter((generator) -> {
            return generator.seed;
        }), NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter((generator) -> {
            return generator.settings;
        }))).apply(instance, instance.stable(NoiseBasedChunkGenerator::new));
    });
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState[] EMPTY_COLUMN = new BlockState[0];
    protected final BlockState defaultBlock;
    public final Registry<NormalNoise.NoiseParameters> noises;
    private final long seed;
    public final Holder<NoiseGeneratorSettings> settings;
    private final NoiseRouter router;
    private final Climate.Sampler sampler;
    private final SurfaceSystem surfaceSystem;
    private final Aquifer.FluidPicker globalFluidPicker;

    public NoiseBasedChunkGenerator(Registry<StructureSet> noiseRegistry, Registry<NormalNoise.NoiseParameters> structuresRegistry, BiomeSource biomeSource, long seed, Holder<NoiseGeneratorSettings> settings) {
        this(noiseRegistry, structuresRegistry, biomeSource, biomeSource, seed, settings);
    }

    private NoiseBasedChunkGenerator(Registry<StructureSet> noiseRegistry, Registry<NormalNoise.NoiseParameters> structuresRegistry, BiomeSource populationSource, BiomeSource biomeSource, long seed, Holder<NoiseGeneratorSettings> settings) {
        super(noiseRegistry, Optional.empty(), populationSource, biomeSource, seed);
        this.noises = structuresRegistry;
        this.seed = seed;
        this.settings = settings;
        NoiseGeneratorSettings noiseGeneratorSettings = this.settings.value();
        this.defaultBlock = noiseGeneratorSettings.defaultBlock();
        NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();
        this.router = noiseGeneratorSettings.createNoiseRouter(structuresRegistry, seed);
        this.sampler = new Climate.Sampler(this.router.temperature(), this.router.humidity(), this.router.continents(), this.router.erosion(), this.router.depth(), this.router.ridges(), this.router.spawnTarget());
        Aquifer.FluidStatus fluidStatus = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        int i = noiseGeneratorSettings.seaLevel();
        Aquifer.FluidStatus fluidStatus2 = new Aquifer.FluidStatus(i, noiseGeneratorSettings.defaultFluid());
        Aquifer.FluidStatus fluidStatus3 = new Aquifer.FluidStatus(noiseSettings.minY() - 1, Blocks.AIR.defaultBlockState());
        this.globalFluidPicker = (x, y, z) -> {
            return y < Math.min(-54, i) ? fluidStatus : fluidStatus2;
        };
        this.surfaceSystem = new SurfaceSystem(structuresRegistry, this.defaultBlock, i, seed, noiseGeneratorSettings.getRandomSource());
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> biomeRegistry, Executor executor, Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
            this.doCreateBiomes(blender, structureAccessor, chunk);
            return chunk;
        }), Util.backgroundExecutor());
    }

    private void doCreateBiomes(Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(this.router, () -> {
            return new Beardifier(structureAccessor, chunk);
        }, this.settings.value(), this.globalFluidPicker, blender);
        BiomeResolver biomeResolver = BelowZeroRetrogen.getBiomeResolver(blender.getBiomeResolver(this.runtimeBiomeSource), chunk);
        chunk.fillBiomesFromNoise(biomeResolver, noiseChunk.cachedClimateSampler(this.router));
    }

    @VisibleForDebug
    public NoiseRouter router() {
        return this.router;
    }

    @Override
    public Climate.Sampler climateSampler() {
        return this.sampler;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed) {
        return new NoiseBasedChunkGenerator(this.structureSets, this.noises, this.biomeSource.withSeed(seed), seed, this.settings);
    }

    public boolean stable(long seed, ResourceKey<NoiseGeneratorSettings> settingsKey) {
        return this.seed == seed && this.settings.is(settingsKey);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world) {
        NoiseSettings noiseSettings = this.settings.value().noiseSettings();
        int i = Math.max(noiseSettings.minY(), world.getMinBuildHeight());
        int j = Math.min(noiseSettings.minY() + noiseSettings.height(), world.getMaxBuildHeight());
        int k = Mth.intFloorDiv(i, noiseSettings.getCellHeight());
        int l = Mth.intFloorDiv(j - i, noiseSettings.getCellHeight());
        return l <= 0 ? world.getMinBuildHeight() : this.iterateNoiseColumn(x, z, (BlockState[])null, heightmap.isOpaque(), k, l).orElse(world.getMinBuildHeight());
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world) {
        NoiseSettings noiseSettings = this.settings.value().noiseSettings();
        int i = Math.max(noiseSettings.minY(), world.getMinBuildHeight());
        int j = Math.min(noiseSettings.minY() + noiseSettings.height(), world.getMaxBuildHeight());
        int k = Mth.intFloorDiv(i, noiseSettings.getCellHeight());
        int l = Mth.intFloorDiv(j - i, noiseSettings.getCellHeight());
        if (l <= 0) {
            return new NoiseColumn(i, EMPTY_COLUMN);
        } else {
            BlockState[] blockStates = new BlockState[l * noiseSettings.getCellHeight()];
            this.iterateNoiseColumn(x, z, blockStates, (Predicate<BlockState>)null, k, l);
            return new NoiseColumn(i, blockStates);
        }
    }

    @Override
    public void addDebugScreenInfo(List<String> text, BlockPos pos) {
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ());
        double d = this.router.ridges().compute(singlePointContext);
        text.add("NoiseRouter T: " + decimalFormat.format(this.router.temperature().compute(singlePointContext)) + " H: " + decimalFormat.format(this.router.humidity().compute(singlePointContext)) + " C: " + decimalFormat.format(this.router.continents().compute(singlePointContext)) + " E: " + decimalFormat.format(this.router.erosion().compute(singlePointContext)) + " D: " + decimalFormat.format(this.router.depth().compute(singlePointContext)) + " W: " + decimalFormat.format(d) + " PV: " + decimalFormat.format((double)TerrainShaper.peaksAndValleys((float)d)) + " AS: " + decimalFormat.format(this.router.initialDensityWithoutJaggedness().compute(singlePointContext)) + " N: " + decimalFormat.format(this.router.finalDensity().compute(singlePointContext)));
    }

    private OptionalInt iterateNoiseColumn(int i, int j, @Nullable BlockState[] states, @Nullable Predicate<BlockState> predicate, int k, int l) {
        NoiseSettings noiseSettings = this.settings.value().noiseSettings();
        int m = noiseSettings.getCellWidth();
        int n = noiseSettings.getCellHeight();
        int o = Math.floorDiv(i, m);
        int p = Math.floorDiv(j, m);
        int q = Math.floorMod(i, m);
        int r = Math.floorMod(j, m);
        int s = o * m;
        int t = p * m;
        double d = (double)q / (double)m;
        double e = (double)r / (double)m;
        NoiseChunk noiseChunk = NoiseChunk.forColumn(s, t, k, l, this.router, this.settings.value(), this.globalFluidPicker);
        noiseChunk.initializeForFirstCellX();
        noiseChunk.advanceCellX(0);

        for(int u = l - 1; u >= 0; --u) {
            noiseChunk.selectCellYZ(u, 0);

            for(int v = n - 1; v >= 0; --v) {
                int w = (k + u) * n + v;
                double f = (double)v / (double)n;
                noiseChunk.updateForY(w, f);
                noiseChunk.updateForX(i, d);
                noiseChunk.updateForZ(j, e);
                BlockState blockState = noiseChunk.getInterpolatedState();
                BlockState blockState2 = blockState == null ? this.defaultBlock : blockState;
                if (states != null) {
                    int x = u * n + v;
                    states[x] = blockState2;
                }

                if (predicate != null && predicate.test(blockState2)) {
                    noiseChunk.stopInterpolation();
                    return OptionalInt.of(w + 1);
                }
            }
        }

        noiseChunk.stopInterpolation();
        return OptionalInt.empty();
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureFeatureManager structures, ChunkAccess chunk) {
        if (!SharedConstants.debugVoidTerrain(chunk.getPos())) {
            WorldGenerationContext worldGenerationContext = new WorldGenerationContext(this, region);
            NoiseGeneratorSettings noiseGeneratorSettings = this.settings.value();
            NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(this.router, () -> {
                return new Beardifier(structures, chunk);
            }, noiseGeneratorSettings, this.globalFluidPicker, Blender.of(region));
            this.surfaceSystem.buildSurface(region.getBiomeManager(), region.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), noiseGeneratorSettings.useLegacyRandomSource(), worldGenerationContext, chunk, noiseChunk, noiseGeneratorSettings.surfaceRule());
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion chunkRegion, long seed, BiomeManager biomeAccess, StructureFeatureManager structureAccessor, ChunkAccess chunk, GenerationStep.Carving generationStep) {
        BiomeManager biomeManager = biomeAccess.withDifferentSource((x, y, z) -> {
            return this.biomeSource.getNoiseBiome(x, y, z, this.climateSampler());
        });
        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
        int i = 8;
        ChunkPos chunkPos = chunk.getPos();
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(this.router, () -> {
            return new Beardifier(structureAccessor, chunk);
        }, this.settings.value(), this.globalFluidPicker, Blender.of(chunkRegion));
        Aquifer aquifer = noiseChunk.aquifer();
        CarvingContext carvingContext = new CarvingContext(this, chunkRegion.registryAccess(), chunk.getHeightAccessorForGeneration(), noiseChunk);
        CarvingMask carvingMask = ((ProtoChunk)chunk).getOrCreateCarvingMask(generationStep);

        for(int j = -8; j <= 8; ++j) {
            for(int k = -8; k <= 8; ++k) {
                ChunkPos chunkPos2 = new ChunkPos(chunkPos.x + j, chunkPos.z + k);
                ChunkAccess chunkAccess = chunkRegion.getChunk(chunkPos2.x, chunkPos2.z);
                BiomeGenerationSettings biomeGenerationSettings = chunkAccess.carverBiome(() -> {
                    return this.biomeSource.getNoiseBiome(QuartPos.fromBlock(chunkPos2.getMinBlockX()), 0, QuartPos.fromBlock(chunkPos2.getMinBlockZ()), this.climateSampler());
                }).value().getGenerationSettings();
                Iterable<Holder<ConfiguredWorldCarver<?>>> iterable = biomeGenerationSettings.getCarvers(generationStep);
                int l = 0;

                for(Holder<ConfiguredWorldCarver<?>> holder : iterable) {
                    ConfiguredWorldCarver<?> configuredWorldCarver = holder.value();
                    worldgenRandom.setLargeFeatureSeed(seed + (long)l, chunkPos2.x, chunkPos2.z);
                    if (configuredWorldCarver.isStartChunk(worldgenRandom)) {
                        configuredWorldCarver.carve(carvingContext, chunk, biomeManager::getBiome, worldgenRandom, aquifer, chunkPos2, carvingMask);
                    }

                    ++l;
                }
            }
        }

    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        NoiseSettings noiseSettings = this.settings.value().noiseSettings();
        LevelHeightAccessor levelHeightAccessor = chunk.getHeightAccessorForGeneration();
        int i = Math.max(noiseSettings.minY(), levelHeightAccessor.getMinBuildHeight());
        int j = Math.min(noiseSettings.minY() + noiseSettings.height(), levelHeightAccessor.getMaxBuildHeight());
        int k = Mth.intFloorDiv(i, noiseSettings.getCellHeight());
        int l = Mth.intFloorDiv(j - i, noiseSettings.getCellHeight());
        if (l <= 0) {
            return CompletableFuture.completedFuture(chunk);
        } else {
            int m = chunk.getSectionIndex(l * noiseSettings.getCellHeight() - 1 + i);
            int n = chunk.getSectionIndex(i);
            Set<LevelChunkSection> set = Sets.newHashSet();

            for(int o = m; o >= n; --o) {
                LevelChunkSection levelChunkSection = chunk.getSection(o);
                levelChunkSection.acquire();
                set.add(levelChunkSection);
            }

            return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("wgen_fill_noise", () -> {
                return this.doFill(blender, structureAccessor, chunk, k, l);
            }), Util.backgroundExecutor()).whenCompleteAsync((chunkAccess, throwable) -> {
                for(LevelChunkSection levelChunkSection : set) {
                    levelChunkSection.release();
                }

            }, executor);
        }
    }

    private ChunkAccess doFill(Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk, int i, int j) {
        NoiseGeneratorSettings noiseGeneratorSettings = this.settings.value();
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(this.router, () -> {
            return new Beardifier(structureAccessor, chunk);
        }, noiseGeneratorSettings, this.globalFluidPicker, blender);
        Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        int k = chunkPos.getMinBlockX();
        int l = chunkPos.getMinBlockZ();
        Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        NoiseSettings noiseSettings = noiseGeneratorSettings.noiseSettings();
        int m = noiseSettings.getCellWidth();
        int n = noiseSettings.getCellHeight();
        int o = 16 / m;
        int p = 16 / m;

        for(int q = 0; q < o; ++q) {
            noiseChunk.advanceCellX(q);

            for(int r = 0; r < p; ++r) {
                LevelChunkSection levelChunkSection = chunk.getSection(chunk.getSectionsCount() - 1);

                for(int s = j - 1; s >= 0; --s) {
                    noiseChunk.selectCellYZ(s, r);

                    for(int t = n - 1; t >= 0; --t) {
                        int u = (i + s) * n + t;
                        int v = u & 15;
                        int w = chunk.getSectionIndex(u);
                        if (chunk.getSectionIndex(levelChunkSection.bottomBlockY()) != w) {
                            levelChunkSection = chunk.getSection(w);
                        }

                        double d = (double)t / (double)n;
                        noiseChunk.updateForY(u, d);

                        for(int x = 0; x < m; ++x) {
                            int y = k + q * m + x;
                            int z = y & 15;
                            double e = (double)x / (double)m;
                            noiseChunk.updateForX(y, e);

                            for(int aa = 0; aa < m; ++aa) {
                                int ab = l + r * m + aa;
                                int ac = ab & 15;
                                double f = (double)aa / (double)m;
                                noiseChunk.updateForZ(ab, f);
                                BlockState blockState = noiseChunk.getInterpolatedState();
                                if (blockState == null) {
                                    blockState = this.defaultBlock;
                                }

                                blockState = this.debugPreliminarySurfaceLevel(noiseChunk, y, u, ab, blockState);
                                if (blockState != AIR && !SharedConstants.debugVoidTerrain(chunk.getPos())) {
                                    if (blockState.getLightEmission() != 0 && chunk instanceof ProtoChunk) {
                                        mutableBlockPos.set(y, u, ab);
                                        ((ProtoChunk)chunk).addLight(mutableBlockPos);
                                    }

                                    levelChunkSection.setBlockState(z, v, ac, blockState, false);
                                    heightmap.update(z, u, ac, blockState);
                                    heightmap2.update(z, u, ac, blockState);
                                    if (aquifer.shouldScheduleFluidUpdate() && !blockState.getFluidState().isEmpty()) {
                                        mutableBlockPos.set(y, u, ab);
                                        chunk.markPosForPostprocessing(mutableBlockPos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            noiseChunk.swapSlices();
        }

        noiseChunk.stopInterpolation();
        return chunk;
    }

    private BlockState debugPreliminarySurfaceLevel(NoiseChunk chunkNoiseSampler, int x, int y, int z, BlockState state) {
        return state;
    }

    @Override
    public int getGenDepth() {
        return this.settings.value().noiseSettings().height();
    }

    @Override
    public int getSeaLevel() {
        return this.settings.value().seaLevel();
    }

    @Override
    public int getMinY() {
        return this.settings.value().noiseSettings().minY();
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        if (!this.settings.value().disableMobGeneration()) {
            ChunkPos chunkPos = region.getCenter();
            Holder<Biome> holder = region.getBiome(chunkPos.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
            WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
            worldgenRandom.setDecorationSeed(region.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
            NaturalSpawner.spawnMobsForChunkGeneration(region, holder, chunkPos, worldgenRandom);
        }
    }

    /** @deprecated */
    @Deprecated
    public Optional<BlockState> topMaterial(CarvingContext context, Function<BlockPos, Holder<Biome>> posToBiome, ChunkAccess chunk, NoiseChunk chunkNoiseSampler, BlockPos pos, boolean hasFluid) {
        return this.surfaceSystem.topMaterial(this.settings.value().surfaceRule(), context, posToBiome, chunk, chunkNoiseSampler, pos, hasFluid);
    }
}
