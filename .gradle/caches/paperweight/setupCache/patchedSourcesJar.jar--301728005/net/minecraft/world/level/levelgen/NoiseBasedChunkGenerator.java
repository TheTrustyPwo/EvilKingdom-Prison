// keep
package net.minecraft.world.level.levelgen;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
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
        return commonCodec(instance).and(instance.group(RegistryOps.retrieveRegistry(Registry.NOISE_REGISTRY).forGetter((chunkgeneratorabstract) -> {
            return chunkgeneratorabstract.noises;
        }), BiomeSource.CODEC.fieldOf("biome_source").forGetter((chunkgeneratorabstract) -> {
            return chunkgeneratorabstract.biomeSource;
        }), Codec.LONG.fieldOf("seed").stable().forGetter((chunkgeneratorabstract) -> {
            return chunkgeneratorabstract.seed;
        }), NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter((chunkgeneratorabstract) -> {
            return chunkgeneratorabstract.settings;
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
        NoiseGeneratorSettings generatorsettingbase = (NoiseGeneratorSettings) this.settings.value();

        this.defaultBlock = generatorsettingbase.defaultBlock();
        NoiseSettings noisesettings = generatorsettingbase.noiseSettings();

        this.router = generatorsettingbase.createNoiseRouter(structuresRegistry, seed);
        this.sampler = new Climate.Sampler(this.router.temperature(), this.router.humidity(), this.router.continents(), this.router.erosion(), this.router.depth(), this.router.ridges(), this.router.spawnTarget());
        Aquifer.FluidStatus aquifer_b = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        int j = generatorsettingbase.seaLevel();
        Aquifer.FluidStatus aquifer_b1 = new Aquifer.FluidStatus(j, generatorsettingbase.defaultFluid());
        Aquifer.FluidStatus aquifer_b2 = new Aquifer.FluidStatus(noisesettings.minY() - 1, Blocks.AIR.defaultBlockState());

        this.globalFluidPicker = (k, l, i1) -> {
            return l < Math.min(-54, j) ? aquifer_b : aquifer_b1;
        };
        this.surfaceSystem = new SurfaceSystem(structuresRegistry, this.defaultBlock, j, seed, generatorsettingbase.getRandomSource());
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> biomeRegistry, Executor executor, Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
            this.doCreateBiomes(blender, structureAccessor, chunk);
            return chunk;
        }), Util.backgroundExecutor());
    }

    private void doCreateBiomes(Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        NoiseChunk noisechunk = chunk.getOrCreateNoiseChunk(this.router, () -> {
            return new Beardifier(structureAccessor, chunk);
        }, (NoiseGeneratorSettings) this.settings.value(), this.globalFluidPicker, blender);
        BiomeResolver biomeresolver = BelowZeroRetrogen.getBiomeResolver(blender.getBiomeResolver(this.runtimeBiomeSource), chunk);

        chunk.fillBiomesFromNoise(biomeresolver, noisechunk.cachedClimateSampler(this.router));
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
        return NoiseBasedChunkGenerator.CODEC;
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
        NoiseSettings noisesettings = ((NoiseGeneratorSettings) this.settings.value()).noiseSettings();
        int k = Math.max(noisesettings.minY(), world.getMinBuildHeight());
        int l = Math.min(noisesettings.minY() + noisesettings.height(), world.getMaxBuildHeight());
        int i1 = Mth.intFloorDiv(k, noisesettings.getCellHeight());
        int j1 = Mth.intFloorDiv(l - k, noisesettings.getCellHeight());

        return j1 <= 0 ? world.getMinBuildHeight() : this.iterateNoiseColumn(x, z, (BlockState[]) null, heightmap.isOpaque(), i1, j1).orElse(world.getMinBuildHeight());
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world) {
        NoiseSettings noisesettings = ((NoiseGeneratorSettings) this.settings.value()).noiseSettings();
        int k = Math.max(noisesettings.minY(), world.getMinBuildHeight());
        int l = Math.min(noisesettings.minY() + noisesettings.height(), world.getMaxBuildHeight());
        int i1 = Mth.intFloorDiv(k, noisesettings.getCellHeight());
        int j1 = Mth.intFloorDiv(l - k, noisesettings.getCellHeight());

        if (j1 <= 0) {
            return new NoiseColumn(k, NoiseBasedChunkGenerator.EMPTY_COLUMN);
        } else {
            BlockState[] aiblockdata = new BlockState[j1 * noisesettings.getCellHeight()];

            this.iterateNoiseColumn(x, z, aiblockdata, (Predicate) null, i1, j1);
            return new NoiseColumn(k, aiblockdata);
        }
    }

    @Override
    public void addDebugScreenInfo(List<String> text, BlockPos pos) {
        DecimalFormat decimalformat = new DecimalFormat("0.000");
        DensityFunction.SinglePointContext densityfunction_d = new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ());
        double d0 = this.router.ridges().compute(densityfunction_d);
        String s = decimalformat.format(this.router.temperature().compute(densityfunction_d));

        text.add("NoiseRouter T: " + s + " H: " + decimalformat.format(this.router.humidity().compute(densityfunction_d)) + " C: " + decimalformat.format(this.router.continents().compute(densityfunction_d)) + " E: " + decimalformat.format(this.router.erosion().compute(densityfunction_d)) + " D: " + decimalformat.format(this.router.depth().compute(densityfunction_d)) + " W: " + decimalformat.format(d0) + " PV: " + decimalformat.format((double) TerrainShaper.peaksAndValleys((float) d0)) + " AS: " + decimalformat.format(this.router.initialDensityWithoutJaggedness().compute(densityfunction_d)) + " N: " + decimalformat.format(this.router.finalDensity().compute(densityfunction_d)));
    }

    private OptionalInt iterateNoiseColumn(int i, int j, @Nullable BlockState[] states, @Nullable Predicate<BlockState> predicate, int k, int l) {
        NoiseSettings noisesettings = ((NoiseGeneratorSettings) this.settings.value()).noiseSettings();
        int i1 = noisesettings.getCellWidth();
        int j1 = noisesettings.getCellHeight();
        int k1 = Math.floorDiv(i, i1);
        int l1 = Math.floorDiv(j, i1);
        int i2 = Math.floorMod(i, i1);
        int j2 = Math.floorMod(j, i1);
        int k2 = k1 * i1;
        int l2 = l1 * i1;
        double d0 = (double) i2 / (double) i1;
        double d1 = (double) j2 / (double) i1;
        NoiseChunk noisechunk = NoiseChunk.forColumn(k2, l2, k, l, this.router, (NoiseGeneratorSettings) this.settings.value(), this.globalFluidPicker);

        noisechunk.initializeForFirstCellX();
        noisechunk.advanceCellX(0);

        for (int i3 = l - 1; i3 >= 0; --i3) {
            noisechunk.selectCellYZ(i3, 0);

            for (int j3 = j1 - 1; j3 >= 0; --j3) {
                int k3 = (k + i3) * j1 + j3;
                double d2 = (double) j3 / (double) j1;

                noisechunk.updateForY(k3, d2);
                noisechunk.updateForX(i, d0);
                noisechunk.updateForZ(j, d1);
                BlockState iblockdata = noisechunk.getInterpolatedState();
                BlockState iblockdata1 = iblockdata == null ? this.defaultBlock : iblockdata;

                if (states != null) {
                    int l3 = i3 * j1 + j3;

                    states[l3] = iblockdata1;
                }

                if (predicate != null && predicate.test(iblockdata1)) {
                    noisechunk.stopInterpolation();
                    return OptionalInt.of(k3 + 1);
                }
            }
        }

        noisechunk.stopInterpolation();
        return OptionalInt.empty();
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureFeatureManager structures, ChunkAccess chunk) {
        if (!SharedConstants.debugVoidTerrain(chunk.getPos())) {
            WorldGenerationContext worldgenerationcontext = new WorldGenerationContext(this, region, region.getMinecraftWorld()); // Paper
            NoiseGeneratorSettings generatorsettingbase = (NoiseGeneratorSettings) this.settings.value();
            NoiseChunk noisechunk = chunk.getOrCreateNoiseChunk(this.router, () -> {
                return new Beardifier(structures, chunk);
            }, generatorsettingbase, this.globalFluidPicker, Blender.of(region));

            this.surfaceSystem.buildSurface(region.getBiomeManager(), region.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), generatorsettingbase.useLegacyRandomSource(), worldgenerationcontext, chunk, noisechunk, generatorsettingbase.surfaceRule());
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion chunkRegion, long seed, BiomeManager biomeAccess, StructureFeatureManager structureAccessor, ChunkAccess chunk, GenerationStep.Carving generationStep) {
        BiomeManager biomemanager1 = biomeAccess.withDifferentSource((j, k, l) -> {
            return this.biomeSource.getNoiseBiome(j, k, l, this.climateSampler());
        });
        WorldgenRandom seededrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));
        boolean flag = true;
        ChunkPos chunkcoordintpair = chunk.getPos();
        NoiseChunk noisechunk = chunk.getOrCreateNoiseChunk(this.router, () -> {
            return new Beardifier(structureAccessor, chunk);
        }, (NoiseGeneratorSettings) this.settings.value(), this.globalFluidPicker, Blender.of(chunkRegion));
        Aquifer aquifer = noisechunk.aquifer();
        CarvingContext carvingcontext = new CarvingContext(this, chunkRegion.registryAccess(), chunk.getHeightAccessorForGeneration(), noisechunk, chunkRegion.getMinecraftWorld()); // Paper
        CarvingMask carvingmask = ((ProtoChunk) chunk).getOrCreateCarvingMask(generationStep);

        for (int j = -8; j <= 8; ++j) {
            for (int k = -8; k <= 8; ++k) {
                ChunkPos chunkcoordintpair1 = new ChunkPos(chunkcoordintpair.x + j, chunkcoordintpair.z + k);
                ChunkAccess ichunkaccess1 = chunkRegion.getChunk(chunkcoordintpair1.x, chunkcoordintpair1.z);
                BiomeGenerationSettings biomesettingsgeneration = ((Biome) ichunkaccess1.carverBiome(() -> {
                    return this.biomeSource.getNoiseBiome(QuartPos.fromBlock(chunkcoordintpair1.getMinBlockX()), 0, QuartPos.fromBlock(chunkcoordintpair1.getMinBlockZ()), this.climateSampler());
                }).value()).getGenerationSettings();
                Iterable<Holder<ConfiguredWorldCarver<?>>> iterable = biomesettingsgeneration.getCarvers(generationStep);
                int l = 0;

                for (Iterator iterator = iterable.iterator(); iterator.hasNext(); ++l) {
                    Holder<ConfiguredWorldCarver<?>> holder = (Holder) iterator.next();
                    ConfiguredWorldCarver<?> worldgencarverwrapper = (ConfiguredWorldCarver) holder.value();

                    seededrandom.setLargeFeatureSeed(seed + (long) l, chunkcoordintpair1.x, chunkcoordintpair1.z);
                    if (worldgencarverwrapper.isStartChunk(seededrandom)) {
                        Objects.requireNonNull(biomemanager1);
                        worldgencarverwrapper.carve(carvingcontext, chunk, biomemanager1::getBiome, seededrandom, aquifer, chunkcoordintpair1, carvingmask);
                    }
                }
            }
        }

    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk) {
        NoiseSettings noisesettings = ((NoiseGeneratorSettings) this.settings.value()).noiseSettings();
        LevelHeightAccessor levelheightaccessor = chunk.getHeightAccessorForGeneration();
        int i = Math.max(noisesettings.minY(), levelheightaccessor.getMinBuildHeight());
        int j = Math.min(noisesettings.minY() + noisesettings.height(), levelheightaccessor.getMaxBuildHeight());
        int k = Mth.intFloorDiv(i, noisesettings.getCellHeight());
        int l = Mth.intFloorDiv(j - i, noisesettings.getCellHeight());

        if (l <= 0) {
            return CompletableFuture.completedFuture(chunk);
        } else {
            int i1 = chunk.getSectionIndex(l * noisesettings.getCellHeight() - 1 + i);
            int j1 = chunk.getSectionIndex(i);
            Set<LevelChunkSection> set = Sets.newHashSet();

            for (int k1 = i1; k1 >= j1; --k1) {
                LevelChunkSection chunksection = chunk.getSection(k1);

                chunksection.acquire();
                set.add(chunksection);
            }

            return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("wgen_fill_noise", () -> {
                return this.doFill(blender, structureAccessor, chunk, k, l);
            }), Util.backgroundExecutor()).whenCompleteAsync((ichunkaccess1, throwable) -> {
                Iterator iterator = set.iterator();

                while (iterator.hasNext()) {
                    LevelChunkSection chunksection1 = (LevelChunkSection) iterator.next();

                    chunksection1.release();
                }

            }, executor);
        }
    }

    private ChunkAccess doFill(Blender blender, StructureFeatureManager structureAccessor, ChunkAccess chunk, int i, int j) {
        NoiseGeneratorSettings generatorsettingbase = (NoiseGeneratorSettings) this.settings.value();
        NoiseChunk noisechunk = chunk.getOrCreateNoiseChunk(this.router, () -> {
            return new Beardifier(structureAccessor, chunk);
        }, generatorsettingbase, this.globalFluidPicker, blender);
        Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmap1 = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkcoordintpair = chunk.getPos();
        int k = chunkcoordintpair.getMinBlockX();
        int l = chunkcoordintpair.getMinBlockZ();
        Aquifer aquifer = noisechunk.aquifer();

        noisechunk.initializeForFirstCellX();
        BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
        NoiseSettings noisesettings = generatorsettingbase.noiseSettings();
        int i1 = noisesettings.getCellWidth();
        int j1 = noisesettings.getCellHeight();
        int k1 = 16 / i1;
        int l1 = 16 / i1;

        for (int i2 = 0; i2 < k1; ++i2) {
            noisechunk.advanceCellX(i2);

            for (int j2 = 0; j2 < l1; ++j2) {
                LevelChunkSection chunksection = chunk.getSection(chunk.getSectionsCount() - 1);

                for (int k2 = j - 1; k2 >= 0; --k2) {
                    noisechunk.selectCellYZ(k2, j2);

                    for (int l2 = j1 - 1; l2 >= 0; --l2) {
                        int i3 = (i + k2) * j1 + l2;
                        int j3 = i3 & 15;
                        int k3 = chunk.getSectionIndex(i3);

                        if (chunk.getSectionIndex(chunksection.bottomBlockY()) != k3) {
                            chunksection = chunk.getSection(k3);
                        }

                        double d0 = (double) l2 / (double) j1;

                        noisechunk.updateForY(i3, d0);

                        for (int l3 = 0; l3 < i1; ++l3) {
                            int i4 = k + i2 * i1 + l3;
                            int j4 = i4 & 15;
                            double d1 = (double) l3 / (double) i1;

                            noisechunk.updateForX(i4, d1);

                            for (int k4 = 0; k4 < i1; ++k4) {
                                int l4 = l + j2 * i1 + k4;
                                int i5 = l4 & 15;
                                double d2 = (double) k4 / (double) i1;

                                noisechunk.updateForZ(l4, d2);
                                BlockState iblockdata = noisechunk.getInterpolatedState();

                                if (iblockdata == null) {
                                    iblockdata = this.defaultBlock;
                                }

                                iblockdata = this.debugPreliminarySurfaceLevel(noisechunk, i4, i3, l4, iblockdata);
                                if (iblockdata != NoiseBasedChunkGenerator.AIR && !SharedConstants.debugVoidTerrain(chunk.getPos())) {
                                    if (iblockdata.getLightEmission() != 0 && chunk instanceof ProtoChunk) {
                                        blockposition_mutableblockposition.set(i4, i3, l4);
                                        ((ProtoChunk) chunk).addLight(blockposition_mutableblockposition);
                                    }

                                    chunksection.setBlockState(j4, j3, i5, iblockdata, false);
                                    heightmap.update(j4, i3, i5, iblockdata);
                                    heightmap1.update(j4, i3, i5, iblockdata);
                                    if (aquifer.shouldScheduleFluidUpdate() && !iblockdata.getFluidState().isEmpty()) {
                                        blockposition_mutableblockposition.set(i4, i3, l4);
                                        chunk.markPosForPostprocessing(blockposition_mutableblockposition);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            noisechunk.swapSlices();
        }

        noisechunk.stopInterpolation();
        return chunk;
    }

    private BlockState debugPreliminarySurfaceLevel(NoiseChunk chunkNoiseSampler, int x, int y, int z, BlockState state) {
        return state;
    }

    @Override
    public int getGenDepth() {
        return ((NoiseGeneratorSettings) this.settings.value()).noiseSettings().height();
    }

    @Override
    public int getSeaLevel() {
        return ((NoiseGeneratorSettings) this.settings.value()).seaLevel();
    }

    @Override
    public int getMinY() {
        return ((NoiseGeneratorSettings) this.settings.value()).noiseSettings().minY();
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        if (!((NoiseGeneratorSettings) this.settings.value()).disableMobGeneration()) {
            ChunkPos chunkcoordintpair = region.getCenter();
            Holder<Biome> holder = region.getBiome(chunkcoordintpair.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
            WorldgenRandom seededrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.seedUniquifier()));

            seededrandom.setDecorationSeed(region.getSeed(), chunkcoordintpair.getMinBlockX(), chunkcoordintpair.getMinBlockZ());
            NaturalSpawner.spawnMobsForChunkGeneration(region, holder, chunkcoordintpair, seededrandom);
        }
    }

    /** @deprecated */
    @Deprecated
    public Optional<BlockState> topMaterial(CarvingContext context, Function<BlockPos, Holder<Biome>> posToBiome, ChunkAccess chunk, NoiseChunk chunkNoiseSampler, BlockPos pos, boolean hasFluid) {
        return this.surfaceSystem.topMaterial(((NoiseGeneratorSettings) this.settings.value()).surfaceRule(), context, posToBiome, chunk, chunkNoiseSampler, pos, hasFluid);
    }
}
