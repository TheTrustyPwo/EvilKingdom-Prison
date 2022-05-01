package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;

public class NoiseChunk implements DensityFunction.ContextProvider, DensityFunction.FunctionContext {
    private final NoiseSettings noiseSettings;
    final int cellCountXZ;
    final int cellCountY;
    final int cellNoiseMinY;
    private final int firstCellX;
    private final int firstCellZ;
    final int firstNoiseX;
    final int firstNoiseZ;
    final List<NoiseChunk.NoiseInterpolator> interpolators;
    final List<NoiseChunk.CacheAllInCell> cellCaches;
    private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();
    private final Long2IntMap preliminarySurfaceLevel = new Long2IntOpenHashMap();
    private final Aquifer aquifer;
    private final DensityFunction initialDensityNoJaggedness;
    private final NoiseChunk.BlockStateFiller blockStateRule;
    private final Blender blender;
    private final NoiseChunk.FlatCache blendAlpha;
    private final NoiseChunk.FlatCache blendOffset;
    private final DensityFunctions.BeardifierOrMarker beardifier;
    private long lastBlendingDataPos = ChunkPos.INVALID_CHUNK_POS;
    private Blender.BlendingOutput lastBlendingOutput = new Blender.BlendingOutput(1.0D, 0.0D);
    final int noiseSizeXZ;
    final int cellWidth;
    final int cellHeight;
    boolean interpolating;
    boolean fillingCell;
    private int cellStartBlockX;
    int cellStartBlockY;
    private int cellStartBlockZ;
    int inCellX;
    int inCellY;
    int inCellZ;
    long interpolationCounter;
    long arrayInterpolationCounter;
    int arrayIndex;
    private final DensityFunction.ContextProvider sliceFillingContextProvider = new DensityFunction.ContextProvider() {
        @Override
        public DensityFunction.FunctionContext forIndex(int i) {
            NoiseChunk.this.cellStartBlockY = (i + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
            ++NoiseChunk.this.interpolationCounter;
            NoiseChunk.this.inCellY = 0;
            NoiseChunk.this.arrayIndex = i;
            return NoiseChunk.this;
        }

        @Override
        public void fillAllDirectly(double[] ds, DensityFunction densityFunction) {
            for(int i = 0; i < NoiseChunk.this.cellCountY + 1; ++i) {
                NoiseChunk.this.cellStartBlockY = (i + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
                ++NoiseChunk.this.interpolationCounter;
                NoiseChunk.this.inCellY = 0;
                NoiseChunk.this.arrayIndex = i;
                ds[i] = densityFunction.compute(NoiseChunk.this);
            }

        }
    };

    public static NoiseChunk forChunk(ChunkAccess chunk, NoiseRouter noiseRouter, Supplier<DensityFunctions.BeardifierOrMarker> noiseTypeSupplier, NoiseGeneratorSettings chunkGeneratorSettings, Aquifer.FluidPicker fluidLevelSampler, Blender blender) {
        ChunkPos chunkPos = chunk.getPos();
        NoiseSettings noiseSettings = chunkGeneratorSettings.noiseSettings();
        int i = Math.max(noiseSettings.minY(), chunk.getMinBuildHeight());
        int j = Math.min(noiseSettings.minY() + noiseSettings.height(), chunk.getMaxBuildHeight());
        int k = Mth.intFloorDiv(i, noiseSettings.getCellHeight());
        int l = Mth.intFloorDiv(j - i, noiseSettings.getCellHeight());
        return new NoiseChunk(16 / noiseSettings.getCellWidth(), l, k, noiseRouter, chunkPos.getMinBlockX(), chunkPos.getMinBlockZ(), noiseTypeSupplier.get(), chunkGeneratorSettings, fluidLevelSampler, blender);
    }

    public static NoiseChunk forColumn(int x, int z, int minimumY, int height, NoiseRouter noiseRouter, NoiseGeneratorSettings chunkGeneratorSettings, Aquifer.FluidPicker fluidLevelSampler) {
        return new NoiseChunk(1, height, minimumY, noiseRouter, x, z, DensityFunctions.BeardifierMarker.INSTANCE, chunkGeneratorSettings, fluidLevelSampler, Blender.empty());
    }

    private NoiseChunk(int horizontalSize, int height, int minimumY, NoiseRouter noiseRouter, int x, int z, DensityFunctions.BeardifierOrMarker noiseType, NoiseGeneratorSettings chunkGeneratorSettings, Aquifer.FluidPicker fluidLevelSampler, Blender blender) {
        this.noiseSettings = chunkGeneratorSettings.noiseSettings();
        this.cellCountXZ = horizontalSize;
        this.cellCountY = height;
        this.cellNoiseMinY = minimumY;
        this.cellWidth = this.noiseSettings.getCellWidth();
        this.cellHeight = this.noiseSettings.getCellHeight();
        this.firstCellX = Math.floorDiv(x, this.cellWidth);
        this.firstCellZ = Math.floorDiv(z, this.cellWidth);
        this.interpolators = Lists.newArrayList();
        this.cellCaches = Lists.newArrayList();
        this.firstNoiseX = QuartPos.fromBlock(x);
        this.firstNoiseZ = QuartPos.fromBlock(z);
        this.noiseSizeXZ = QuartPos.fromBlock(horizontalSize * this.cellWidth);
        this.blender = blender;
        this.beardifier = noiseType;
        this.blendAlpha = new NoiseChunk.FlatCache(new NoiseChunk.BlendAlpha(), false);
        this.blendOffset = new NoiseChunk.FlatCache(new NoiseChunk.BlendOffset(), false);

        for(int i = 0; i <= this.noiseSizeXZ; ++i) {
            int j = this.firstNoiseX + i;
            int k = QuartPos.toBlock(j);

            for(int l = 0; l <= this.noiseSizeXZ; ++l) {
                int m = this.firstNoiseZ + l;
                int n = QuartPos.toBlock(m);
                Blender.BlendingOutput blendingOutput = blender.blendOffsetAndFactor(k, n);
                this.blendAlpha.values[i][l] = blendingOutput.alpha();
                this.blendOffset.values[i][l] = blendingOutput.blendingOffset();
            }
        }

        if (!chunkGeneratorSettings.isAquifersEnabled()) {
            this.aquifer = Aquifer.createDisabled(fluidLevelSampler);
        } else {
            int o = SectionPos.blockToSectionCoord(x);
            int p = SectionPos.blockToSectionCoord(z);
            this.aquifer = Aquifer.create(this, new ChunkPos(o, p), noiseRouter.barrierNoise(), noiseRouter.fluidLevelFloodednessNoise(), noiseRouter.fluidLevelSpreadNoise(), noiseRouter.lavaNoise(), noiseRouter.aquiferPositionalRandomFactory(), minimumY * this.cellHeight, height * this.cellHeight, fluidLevelSampler);
        }

        Builder<NoiseChunk.BlockStateFiller> builder = ImmutableList.builder();
        DensityFunction densityFunction = DensityFunctions.cacheAllInCell(DensityFunctions.add(noiseRouter.finalDensity(), DensityFunctions.BeardifierMarker.INSTANCE)).mapAll(this::wrap);
        builder.add((functionContext) -> {
            return this.aquifer.computeSubstance(functionContext, densityFunction.compute(functionContext));
        });
        if (chunkGeneratorSettings.oreVeinsEnabled()) {
            builder.add(OreVeinifier.create(noiseRouter.veinToggle().mapAll(this::wrap), noiseRouter.veinRidged().mapAll(this::wrap), noiseRouter.veinGap().mapAll(this::wrap), noiseRouter.oreVeinsPositionalRandomFactory()));
        }

        this.blockStateRule = new MaterialRuleList(builder.build());
        this.initialDensityNoJaggedness = noiseRouter.initialDensityWithoutJaggedness().mapAll(this::wrap);
    }

    protected Climate.Sampler cachedClimateSampler(NoiseRouter noiseRouter) {
        return new Climate.Sampler(noiseRouter.temperature().mapAll(this::wrap), noiseRouter.humidity().mapAll(this::wrap), noiseRouter.continents().mapAll(this::wrap), noiseRouter.erosion().mapAll(this::wrap), noiseRouter.depth().mapAll(this::wrap), noiseRouter.ridges().mapAll(this::wrap), noiseRouter.spawnTarget());
    }

    @Nullable
    protected BlockState getInterpolatedState() {
        return this.blockStateRule.calculate(this);
    }

    @Override
    public int blockX() {
        return this.cellStartBlockX + this.inCellX;
    }

    @Override
    public int blockY() {
        return this.cellStartBlockY + this.inCellY;
    }

    @Override
    public int blockZ() {
        return this.cellStartBlockZ + this.inCellZ;
    }

    public int preliminarySurfaceLevel(int i, int j) {
        return this.preliminarySurfaceLevel.computeIfAbsent(ChunkPos.asLong(QuartPos.fromBlock(i), QuartPos.fromBlock(j)), this::computePreliminarySurfaceLevel);
    }

    private int computePreliminarySurfaceLevel(long l) {
        int i = ChunkPos.getX(l);
        int j = ChunkPos.getZ(l);
        return (int)NoiseRouterData.computePreliminarySurfaceLevelScanning(this.noiseSettings, this.initialDensityNoJaggedness, QuartPos.toBlock(i), QuartPos.toBlock(j));
    }

    @Override
    public Blender getBlender() {
        return this.blender;
    }

    private void fillSlice(boolean bl, int i) {
        this.cellStartBlockX = i * this.cellWidth;
        this.inCellX = 0;

        for(int j = 0; j < this.cellCountXZ + 1; ++j) {
            int k = this.firstCellZ + j;
            this.cellStartBlockZ = k * this.cellWidth;
            this.inCellZ = 0;
            ++this.arrayInterpolationCounter;

            for(NoiseChunk.NoiseInterpolator noiseInterpolator : this.interpolators) {
                double[] ds = (bl ? noiseInterpolator.slice0 : noiseInterpolator.slice1)[j];
                noiseInterpolator.fillArray(ds, this.sliceFillingContextProvider);
            }
        }

        ++this.arrayInterpolationCounter;
    }

    public void initializeForFirstCellX() {
        if (this.interpolating) {
            throw new IllegalStateException("Staring interpolation twice");
        } else {
            this.interpolating = true;
            this.interpolationCounter = 0L;
            this.fillSlice(true, this.firstCellX);
        }
    }

    public void advanceCellX(int x) {
        this.fillSlice(false, this.firstCellX + x + 1);
        this.cellStartBlockX = (this.firstCellX + x) * this.cellWidth;
    }

    @Override
    public NoiseChunk forIndex(int i) {
        int j = Math.floorMod(i, this.cellWidth);
        int k = Math.floorDiv(i, this.cellWidth);
        int l = Math.floorMod(k, this.cellWidth);
        int m = this.cellHeight - 1 - Math.floorDiv(k, this.cellWidth);
        this.inCellX = l;
        this.inCellY = m;
        this.inCellZ = j;
        this.arrayIndex = i;
        return this;
    }

    @Override
    public void fillAllDirectly(double[] ds, DensityFunction densityFunction) {
        this.arrayIndex = 0;

        for(int i = this.cellHeight - 1; i >= 0; --i) {
            this.inCellY = i;

            for(int j = 0; j < this.cellWidth; ++j) {
                this.inCellX = j;

                for(int k = 0; k < this.cellWidth; ++k) {
                    this.inCellZ = k;
                    ds[this.arrayIndex++] = densityFunction.compute(this);
                }
            }
        }

    }

    public void selectCellYZ(int noiseY, int noiseZ) {
        this.interpolators.forEach((interpolator) -> {
            interpolator.selectCellYZ(noiseY, noiseZ);
        });
        this.fillingCell = true;
        this.cellStartBlockY = (noiseY + this.cellNoiseMinY) * this.cellHeight;
        this.cellStartBlockZ = (this.firstCellZ + noiseZ) * this.cellWidth;
        ++this.arrayInterpolationCounter;

        for(NoiseChunk.CacheAllInCell cacheAllInCell : this.cellCaches) {
            cacheAllInCell.noiseFiller.fillArray(cacheAllInCell.values, this);
        }

        ++this.arrayInterpolationCounter;
        this.fillingCell = false;
    }

    public void updateForY(int i, double d) {
        this.inCellY = i - this.cellStartBlockY;
        this.interpolators.forEach((interpolator) -> {
            interpolator.updateForY(d);
        });
    }

    public void updateForX(int i, double d) {
        this.inCellX = i - this.cellStartBlockX;
        this.interpolators.forEach((interpolator) -> {
            interpolator.updateForX(d);
        });
    }

    public void updateForZ(int i, double d) {
        this.inCellZ = i - this.cellStartBlockZ;
        ++this.interpolationCounter;
        this.interpolators.forEach((interpolator) -> {
            interpolator.updateForZ(d);
        });
    }

    public void stopInterpolation() {
        if (!this.interpolating) {
            throw new IllegalStateException("Staring interpolation twice");
        } else {
            this.interpolating = false;
        }
    }

    public void swapSlices() {
        this.interpolators.forEach(NoiseChunk.NoiseInterpolator::swapSlices);
    }

    public Aquifer aquifer() {
        return this.aquifer;
    }

    Blender.BlendingOutput getOrComputeBlendingOutput(int i, int j) {
        long l = ChunkPos.asLong(i, j);
        if (this.lastBlendingDataPos == l) {
            return this.lastBlendingOutput;
        } else {
            this.lastBlendingDataPos = l;
            Blender.BlendingOutput blendingOutput = this.blender.blendOffsetAndFactor(i, j);
            this.lastBlendingOutput = blendingOutput;
            return blendingOutput;
        }
    }

    protected DensityFunction wrap(DensityFunction densityFunction) {
        return this.wrapped.computeIfAbsent(densityFunction, this::wrapNew);
    }

    private DensityFunction wrapNew(DensityFunction densityFunction) {
        if (densityFunction instanceof DensityFunctions.Marker) {
            DensityFunctions.Marker marker = (DensityFunctions.Marker)densityFunction;
            Object var10000;
            switch(marker.type()) {
            case Interpolated:
                var10000 = new NoiseChunk.NoiseInterpolator(marker.wrapped());
                break;
            case FlatCache:
                var10000 = new NoiseChunk.FlatCache(marker.wrapped(), true);
                break;
            case Cache2D:
                var10000 = new NoiseChunk.Cache2D(marker.wrapped());
                break;
            case CacheOnce:
                var10000 = new NoiseChunk.CacheOnce(marker.wrapped());
                break;
            case CacheAllInCell:
                var10000 = new NoiseChunk.CacheAllInCell(marker.wrapped());
                break;
            default:
                throw new IncompatibleClassChangeError();
            }

            return (DensityFunction)var10000;
        } else {
            if (this.blender != Blender.empty()) {
                if (densityFunction == DensityFunctions.BlendAlpha.INSTANCE) {
                    return this.blendAlpha;
                }

                if (densityFunction == DensityFunctions.BlendOffset.INSTANCE) {
                    return this.blendOffset;
                }
            }

            if (densityFunction == DensityFunctions.BeardifierMarker.INSTANCE) {
                return this.beardifier;
            } else if (densityFunction instanceof DensityFunctions.HolderHolder) {
                DensityFunctions.HolderHolder holderHolder = (DensityFunctions.HolderHolder)densityFunction;
                return holderHolder.function().value();
            } else {
                return densityFunction;
            }
        }
    }

    class BlendAlpha implements NoiseChunk.NoiseChunkDensityFunction {
        @Override
        public DensityFunction wrapped() {
            return DensityFunctions.BlendAlpha.INSTANCE;
        }

        @Override
        public double compute(DensityFunction.FunctionContext pos) {
            return NoiseChunk.this.getOrComputeBlendingOutput(pos.blockX(), pos.blockZ()).alpha();
        }

        @Override
        public void fillArray(double[] ds, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(ds, this);
        }

        @Override
        public double minValue() {
            return 0.0D;
        }

        @Override
        public double maxValue() {
            return 1.0D;
        }

        @Override
        public Codec<? extends DensityFunction> codec() {
            return DensityFunctions.BlendAlpha.CODEC;
        }
    }

    class BlendOffset implements NoiseChunk.NoiseChunkDensityFunction {
        @Override
        public DensityFunction wrapped() {
            return DensityFunctions.BlendOffset.INSTANCE;
        }

        @Override
        public double compute(DensityFunction.FunctionContext pos) {
            return NoiseChunk.this.getOrComputeBlendingOutput(pos.blockX(), pos.blockZ()).blendingOffset();
        }

        @Override
        public void fillArray(double[] ds, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(ds, this);
        }

        @Override
        public double minValue() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public Codec<? extends DensityFunction> codec() {
            return DensityFunctions.BlendOffset.CODEC;
        }
    }

    @FunctionalInterface
    public interface BlockStateFiller {
        @Nullable
        BlockState calculate(DensityFunction.FunctionContext pos);
    }

    static class Cache2D implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
        private final DensityFunction function;
        private long lastPos2D = ChunkPos.INVALID_CHUNK_POS;
        private double lastValue;

        Cache2D(DensityFunction densityFunction) {
            this.function = densityFunction;
        }

        @Override
        public double compute(DensityFunction.FunctionContext pos) {
            int i = pos.blockX();
            int j = pos.blockZ();
            long l = ChunkPos.asLong(i, j);
            if (this.lastPos2D == l) {
                return this.lastValue;
            } else {
                this.lastPos2D = l;
                double d = this.function.compute(pos);
                this.lastValue = d;
                return d;
            }
        }

        @Override
        public void fillArray(double[] ds, DensityFunction.ContextProvider contextProvider) {
            this.function.fillArray(ds, contextProvider);
        }

        @Override
        public DensityFunction wrapped() {
            return this.function;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.Cache2D;
        }
    }

    class CacheAllInCell implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
        final DensityFunction noiseFiller;
        final double[] values;

        CacheAllInCell(DensityFunction densityFunction) {
            this.noiseFiller = densityFunction;
            this.values = new double[NoiseChunk.this.cellWidth * NoiseChunk.this.cellWidth * NoiseChunk.this.cellHeight];
            NoiseChunk.this.cellCaches.add(this);
        }

        @Override
        public double compute(DensityFunction.FunctionContext pos) {
            if (pos != NoiseChunk.this) {
                return this.noiseFiller.compute(pos);
            } else if (!NoiseChunk.this.interpolating) {
                throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
            } else {
                int i = NoiseChunk.this.inCellX;
                int j = NoiseChunk.this.inCellY;
                int k = NoiseChunk.this.inCellZ;
                return i >= 0 && j >= 0 && k >= 0 && i < NoiseChunk.this.cellWidth && j < NoiseChunk.this.cellHeight && k < NoiseChunk.this.cellWidth ? this.values[((NoiseChunk.this.cellHeight - 1 - j) * NoiseChunk.this.cellWidth + i) * NoiseChunk.this.cellWidth + k] : this.noiseFiller.compute(pos);
            }
        }

        @Override
        public void fillArray(double[] ds, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(ds, this);
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.CacheAllInCell;
        }
    }

    class CacheOnce implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
        private final DensityFunction function;
        private long lastCounter;
        private long lastArrayCounter;
        private double lastValue;
        @Nullable
        private double[] lastArray;

        CacheOnce(DensityFunction densityFunction) {
            this.function = densityFunction;
        }

        @Override
        public double compute(DensityFunction.FunctionContext pos) {
            if (pos != NoiseChunk.this) {
                return this.function.compute(pos);
            } else if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
                return this.lastArray[NoiseChunk.this.arrayIndex];
            } else if (this.lastCounter == NoiseChunk.this.interpolationCounter) {
                return this.lastValue;
            } else {
                this.lastCounter = NoiseChunk.this.interpolationCounter;
                double d = this.function.compute(pos);
                this.lastValue = d;
                return d;
            }
        }

        @Override
        public void fillArray(double[] ds, DensityFunction.ContextProvider contextProvider) {
            if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
                System.arraycopy(this.lastArray, 0, ds, 0, ds.length);
            } else {
                this.wrapped().fillArray(ds, contextProvider);
                if (this.lastArray != null && this.lastArray.length == ds.length) {
                    System.arraycopy(ds, 0, this.lastArray, 0, ds.length);
                } else {
                    this.lastArray = (double[])ds.clone();
                }

                this.lastArrayCounter = NoiseChunk.this.arrayInterpolationCounter;
            }
        }

        @Override
        public DensityFunction wrapped() {
            return this.function;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.CacheOnce;
        }
    }

    class FlatCache implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
        private final DensityFunction noiseFiller;
        final double[][] values;

        FlatCache(DensityFunction densityFunction, boolean bl) {
            this.noiseFiller = densityFunction;
            this.values = new double[NoiseChunk.this.noiseSizeXZ + 1][NoiseChunk.this.noiseSizeXZ + 1];
            if (bl) {
                for(int i = 0; i <= NoiseChunk.this.noiseSizeXZ; ++i) {
                    int j = NoiseChunk.this.firstNoiseX + i;
                    int k = QuartPos.toBlock(j);

                    for(int l = 0; l <= NoiseChunk.this.noiseSizeXZ; ++l) {
                        int m = NoiseChunk.this.firstNoiseZ + l;
                        int n = QuartPos.toBlock(m);
                        this.values[i][l] = densityFunction.compute(new DensityFunction.SinglePointContext(k, 0, n));
                    }
                }
            }

        }

        @Override
        public double compute(DensityFunction.FunctionContext pos) {
            int i = QuartPos.fromBlock(pos.blockX());
            int j = QuartPos.fromBlock(pos.blockZ());
            int k = i - NoiseChunk.this.firstNoiseX;
            int l = j - NoiseChunk.this.firstNoiseZ;
            int m = this.values.length;
            return k >= 0 && l >= 0 && k < m && l < m ? this.values[k][l] : this.noiseFiller.compute(pos);
        }

        @Override
        public void fillArray(double[] ds, DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(ds, this);
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.FlatCache;
        }
    }

    interface NoiseChunkDensityFunction extends DensityFunction {
        DensityFunction wrapped();

        @Override
        default DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return this.wrapped().mapAll(visitor);
        }

        @Override
        default double minValue() {
            return this.wrapped().minValue();
        }

        @Override
        default double maxValue() {
            return this.wrapped().maxValue();
        }
    }

    public class NoiseInterpolator implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
        double[][] slice0;
        double[][] slice1;
        private final DensityFunction noiseFiller;
        private double noise000;
        private double noise001;
        private double noise100;
        private double noise101;
        private double noise010;
        private double noise011;
        private double noise110;
        private double noise111;
        private double valueXZ00;
        private double valueXZ10;
        private double valueXZ01;
        private double valueXZ11;
        private double valueZ0;
        private double valueZ1;
        private double value;

        NoiseInterpolator(DensityFunction columnSampler) {
            this.noiseFiller = columnSampler;
            this.slice0 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
            this.slice1 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
            NoiseChunk.this.interpolators.add(this);
        }

        private double[][] allocateSlice(int sizeZ, int sizeX) {
            int i = sizeX + 1;
            int j = sizeZ + 1;
            double[][] ds = new double[i][j];

            for(int k = 0; k < i; ++k) {
                ds[k] = new double[j];
            }

            return ds;
        }

        void selectCellYZ(int noiseY, int noiseZ) {
            this.noise000 = this.slice0[noiseZ][noiseY];
            this.noise001 = this.slice0[noiseZ + 1][noiseY];
            this.noise100 = this.slice1[noiseZ][noiseY];
            this.noise101 = this.slice1[noiseZ + 1][noiseY];
            this.noise010 = this.slice0[noiseZ][noiseY + 1];
            this.noise011 = this.slice0[noiseZ + 1][noiseY + 1];
            this.noise110 = this.slice1[noiseZ][noiseY + 1];
            this.noise111 = this.slice1[noiseZ + 1][noiseY + 1];
        }

        void updateForY(double deltaY) {
            this.valueXZ00 = Mth.lerp(deltaY, this.noise000, this.noise010);
            this.valueXZ10 = Mth.lerp(deltaY, this.noise100, this.noise110);
            this.valueXZ01 = Mth.lerp(deltaY, this.noise001, this.noise011);
            this.valueXZ11 = Mth.lerp(deltaY, this.noise101, this.noise111);
        }

        void updateForX(double deltaX) {
            this.valueZ0 = Mth.lerp(deltaX, this.valueXZ00, this.valueXZ10);
            this.valueZ1 = Mth.lerp(deltaX, this.valueXZ01, this.valueXZ11);
        }

        void updateForZ(double deltaZ) {
            this.value = Mth.lerp(deltaZ, this.valueZ0, this.valueZ1);
        }

        @Override
        public double compute(DensityFunction.FunctionContext pos) {
            if (pos != NoiseChunk.this) {
                return this.noiseFiller.compute(pos);
            } else if (!NoiseChunk.this.interpolating) {
                throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
            } else {
                return NoiseChunk.this.fillingCell ? Mth.lerp3((double)NoiseChunk.this.inCellX / (double)NoiseChunk.this.cellWidth, (double)NoiseChunk.this.inCellY / (double)NoiseChunk.this.cellHeight, (double)NoiseChunk.this.inCellZ / (double)NoiseChunk.this.cellWidth, this.noise000, this.noise100, this.noise010, this.noise110, this.noise001, this.noise101, this.noise011, this.noise111) : this.value;
            }
        }

        @Override
        public void fillArray(double[] ds, DensityFunction.ContextProvider contextProvider) {
            if (NoiseChunk.this.fillingCell) {
                contextProvider.fillAllDirectly(ds, this);
            } else {
                this.wrapped().fillArray(ds, contextProvider);
            }
        }

        @Override
        public DensityFunction wrapped() {
            return this.noiseFiller;
        }

        private void swapSlices() {
            double[][] ds = this.slice0;
            this.slice0 = this.slice1;
            this.slice1 = ds;
        }

        @Override
        public DensityFunctions.Marker.Type type() {
            return DensityFunctions.Marker.Type.Interpolated;
        }
    }
}
