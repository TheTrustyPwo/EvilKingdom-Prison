package net.minecraft.world.level.levelgen.blending;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.FluidState;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;

public class Blender {
    private static final Blender EMPTY = new Blender(new Long2ObjectOpenHashMap(), new Long2ObjectOpenHashMap()) {
        @Override
        public Blender.BlendingOutput blendOffsetAndFactor(int i, int j) {
            return new Blender.BlendingOutput(1.0D, 0.0D);
        }

        @Override
        public double blendDensity(DensityFunction.FunctionContext functionContext, double d) {
            return d;
        }

        @Override
        public BiomeResolver getBiomeResolver(BiomeResolver biomeSupplier) {
            return biomeSupplier;
        }
    };
    private static final NormalNoise SHIFT_NOISE = NormalNoise.create(new XoroshiroRandomSource(42L), BuiltinRegistries.NOISE.getOrThrow(Noises.SHIFT));
    private static final int HEIGHT_BLENDING_RANGE_CELLS = QuartPos.fromSection(7) - 1;
    private static final int HEIGHT_BLENDING_RANGE_CHUNKS = QuartPos.toSection(HEIGHT_BLENDING_RANGE_CELLS + 3);
    private static final int DENSITY_BLENDING_RANGE_CELLS = 2;
    private static final int DENSITY_BLENDING_RANGE_CHUNKS = QuartPos.toSection(5);
    private static final double OLD_CHUNK_Y_RADIUS = (double)BlendingData.AREA_WITH_OLD_GENERATION.getHeight() / 2.0D;
    private static final double OLD_CHUNK_CENTER_Y = (double)BlendingData.AREA_WITH_OLD_GENERATION.getMinBuildHeight() + OLD_CHUNK_Y_RADIUS;
    private static final double OLD_CHUNK_XZ_RADIUS = 8.0D;
    private final Long2ObjectOpenHashMap<BlendingData> blendingData;
    private final Long2ObjectOpenHashMap<BlendingData> blendingDataForDensityBlending;

    public static Blender empty() {
        return EMPTY;
    }

    public static Blender of(@Nullable WorldGenRegion chunkRegion) {
        if (chunkRegion == null) {
            return EMPTY;
        } else {
            Long2ObjectOpenHashMap<BlendingData> long2ObjectOpenHashMap = new Long2ObjectOpenHashMap<>();
            Long2ObjectOpenHashMap<BlendingData> long2ObjectOpenHashMap2 = new Long2ObjectOpenHashMap<>();
            ChunkPos chunkPos = chunkRegion.getCenter();

            for(int i = -HEIGHT_BLENDING_RANGE_CHUNKS; i <= HEIGHT_BLENDING_RANGE_CHUNKS; ++i) {
                for(int j = -HEIGHT_BLENDING_RANGE_CHUNKS; j <= HEIGHT_BLENDING_RANGE_CHUNKS; ++j) {
                    int k = chunkPos.x + i;
                    int l = chunkPos.z + j;
                    BlendingData blendingData = BlendingData.getOrUpdateBlendingData(chunkRegion, k, l);
                    if (blendingData != null) {
                        long2ObjectOpenHashMap.put(ChunkPos.asLong(k, l), blendingData);
                        if (i >= -DENSITY_BLENDING_RANGE_CHUNKS && i <= DENSITY_BLENDING_RANGE_CHUNKS && j >= -DENSITY_BLENDING_RANGE_CHUNKS && j <= DENSITY_BLENDING_RANGE_CHUNKS) {
                            long2ObjectOpenHashMap2.put(ChunkPos.asLong(k, l), blendingData);
                        }
                    }
                }
            }

            return long2ObjectOpenHashMap.isEmpty() && long2ObjectOpenHashMap2.isEmpty() ? EMPTY : new Blender(long2ObjectOpenHashMap, long2ObjectOpenHashMap2);
        }
    }

    Blender(Long2ObjectOpenHashMap<BlendingData> long2ObjectOpenHashMap, Long2ObjectOpenHashMap<BlendingData> long2ObjectOpenHashMap2) {
        this.blendingData = long2ObjectOpenHashMap;
        this.blendingDataForDensityBlending = long2ObjectOpenHashMap2;
    }

    public Blender.BlendingOutput blendOffsetAndFactor(int i, int j) {
        int k = QuartPos.fromBlock(i);
        int l = QuartPos.fromBlock(j);
        double d = this.getBlendingDataValue(k, 0, l, BlendingData::getHeight);
        if (d != Double.MAX_VALUE) {
            return new Blender.BlendingOutput(0.0D, heightToOffset(d));
        } else {
            MutableDouble mutableDouble = new MutableDouble(0.0D);
            MutableDouble mutableDouble2 = new MutableDouble(0.0D);
            MutableDouble mutableDouble3 = new MutableDouble(Double.POSITIVE_INFINITY);
            this.blendingData.forEach((long_, blendingData) -> {
                blendingData.iterateHeights(QuartPos.fromSection(ChunkPos.getX(long_)), QuartPos.fromSection(ChunkPos.getZ(long_)), (kx, lx, d) -> {
                    double e = Mth.length((double)(k - kx), (double)(l - lx));
                    if (!(e > (double)HEIGHT_BLENDING_RANGE_CELLS)) {
                        if (e < mutableDouble3.doubleValue()) {
                            mutableDouble3.setValue(e);
                        }

                        double f = 1.0D / (e * e * e * e);
                        mutableDouble2.add(d * f);
                        mutableDouble.add(f);
                    }
                });
            });
            if (mutableDouble3.doubleValue() == Double.POSITIVE_INFINITY) {
                return new Blender.BlendingOutput(1.0D, 0.0D);
            } else {
                double e = mutableDouble2.doubleValue() / mutableDouble.doubleValue();
                double f = Mth.clamp(mutableDouble3.doubleValue() / (double)(HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0D, 1.0D);
                f = 3.0D * f * f - 2.0D * f * f * f;
                return new Blender.BlendingOutput(f, heightToOffset(e));
            }
        }
    }

    private static double heightToOffset(double d) {
        double e = 1.0D;
        double f = d + 0.5D;
        double g = Mth.positiveModulo(f, 8.0D);
        return 1.0D * (32.0D * (f - 128.0D) - 3.0D * (f - 120.0D) * g + 3.0D * g * g) / (128.0D * (32.0D - 3.0D * g));
    }

    public double blendDensity(DensityFunction.FunctionContext functionContext, double d) {
        int i = QuartPos.fromBlock(functionContext.blockX());
        int j = functionContext.blockY() / 8;
        int k = QuartPos.fromBlock(functionContext.blockZ());
        double e = this.getBlendingDataValue(i, j, k, BlendingData::getDensity);
        if (e != Double.MAX_VALUE) {
            return e;
        } else {
            MutableDouble mutableDouble = new MutableDouble(0.0D);
            MutableDouble mutableDouble2 = new MutableDouble(0.0D);
            MutableDouble mutableDouble3 = new MutableDouble(Double.POSITIVE_INFINITY);
            this.blendingDataForDensityBlending.forEach((long_, blendingData) -> {
                blendingData.iterateDensities(QuartPos.fromSection(ChunkPos.getX(long_)), QuartPos.fromSection(ChunkPos.getZ(long_)), j - 1, j + 1, (l, m, n, d) -> {
                    double e = Mth.length((double)(i - l), (double)((j - m) * 2), (double)(k - n));
                    if (!(e > 2.0D)) {
                        if (e < mutableDouble3.doubleValue()) {
                            mutableDouble3.setValue(e);
                        }

                        double f = 1.0D / (e * e * e * e);
                        mutableDouble2.add(d * f);
                        mutableDouble.add(f);
                    }
                });
            });
            if (mutableDouble3.doubleValue() == Double.POSITIVE_INFINITY) {
                return d;
            } else {
                double f = mutableDouble2.doubleValue() / mutableDouble.doubleValue();
                double g = Mth.clamp(mutableDouble3.doubleValue() / 3.0D, 0.0D, 1.0D);
                return Mth.lerp(g, f, d);
            }
        }
    }

    private double getBlendingDataValue(int i, int j, int k, Blender.CellValueGetter cellValueGetter) {
        int l = QuartPos.toSection(i);
        int m = QuartPos.toSection(k);
        boolean bl = (i & 3) == 0;
        boolean bl2 = (k & 3) == 0;
        double d = this.getBlendingDataValue(cellValueGetter, l, m, i, j, k);
        if (d == Double.MAX_VALUE) {
            if (bl && bl2) {
                d = this.getBlendingDataValue(cellValueGetter, l - 1, m - 1, i, j, k);
            }

            if (d == Double.MAX_VALUE) {
                if (bl) {
                    d = this.getBlendingDataValue(cellValueGetter, l - 1, m, i, j, k);
                }

                if (d == Double.MAX_VALUE && bl2) {
                    d = this.getBlendingDataValue(cellValueGetter, l, m - 1, i, j, k);
                }
            }
        }

        return d;
    }

    private double getBlendingDataValue(Blender.CellValueGetter cellValueGetter, int i, int j, int k, int l, int m) {
        BlendingData blendingData = this.blendingData.get(ChunkPos.asLong(i, j));
        return blendingData != null ? cellValueGetter.get(blendingData, k - QuartPos.fromSection(i), l, m - QuartPos.fromSection(j)) : Double.MAX_VALUE;
    }

    public BiomeResolver getBiomeResolver(BiomeResolver biomeSupplier) {
        return (x, y, z, noise) -> {
            Holder<Biome> holder = this.blendBiome(x, z);
            return holder == null ? biomeSupplier.getNoiseBiome(x, y, z, noise) : holder;
        };
    }

    @Nullable
    private Holder<Biome> blendBiome(int x, int y) {
        double d = (double)x + SHIFT_NOISE.getValue((double)x, 0.0D, (double)y) * 12.0D;
        double e = (double)y + SHIFT_NOISE.getValue((double)y, (double)x, 0.0D) * 12.0D;
        MutableDouble mutableDouble = new MutableDouble(Double.POSITIVE_INFINITY);
        MutableObject<Holder<Biome>> mutableObject = new MutableObject<>();
        this.blendingData.forEach((long_, blendingData) -> {
            blendingData.iterateBiomes(QuartPos.fromSection(ChunkPos.getX(long_)), QuartPos.fromSection(ChunkPos.getZ(long_)), (i, j, holder) -> {
                double f = Mth.length(d - (double)i, e - (double)j);
                if (!(f > (double)HEIGHT_BLENDING_RANGE_CELLS)) {
                    if (f < mutableDouble.doubleValue()) {
                        mutableObject.setValue(holder);
                        mutableDouble.setValue(f);
                    }

                }
            });
        });
        if (mutableDouble.doubleValue() == Double.POSITIVE_INFINITY) {
            return null;
        } else {
            double f = Mth.clamp(mutableDouble.doubleValue() / (double)(HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0D, 1.0D);
            return f > 0.5D ? null : mutableObject.getValue();
        }
    }

    public static void generateBorderTicks(WorldGenRegion chunkRegion, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        boolean bl = chunk.isOldNoiseGeneration();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPos blockPos = new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());
        int i = BlendingData.AREA_WITH_OLD_GENERATION.getMinBuildHeight();
        int j = BlendingData.AREA_WITH_OLD_GENERATION.getMaxBuildHeight() - 1;
        if (bl) {
            for(int k = 0; k < 16; ++k) {
                for(int l = 0; l < 16; ++l) {
                    generateBorderTick(chunk, mutableBlockPos.setWithOffset(blockPos, k, i - 1, l));
                    generateBorderTick(chunk, mutableBlockPos.setWithOffset(blockPos, k, i, l));
                    generateBorderTick(chunk, mutableBlockPos.setWithOffset(blockPos, k, j, l));
                    generateBorderTick(chunk, mutableBlockPos.setWithOffset(blockPos, k, j + 1, l));
                }
            }
        }

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            if (chunkRegion.getChunk(chunkPos.x + direction.getStepX(), chunkPos.z + direction.getStepZ()).isOldNoiseGeneration() != bl) {
                int m = direction == Direction.EAST ? 15 : 0;
                int n = direction == Direction.WEST ? 0 : 15;
                int o = direction == Direction.SOUTH ? 15 : 0;
                int p = direction == Direction.NORTH ? 0 : 15;

                for(int q = m; q <= n; ++q) {
                    for(int r = o; r <= p; ++r) {
                        int s = Math.min(j, chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, q, r)) + 1;

                        for(int t = i; t < s; ++t) {
                            generateBorderTick(chunk, mutableBlockPos.setWithOffset(blockPos, q, t, r));
                        }
                    }
                }
            }
        }

    }

    private static void generateBorderTick(ChunkAccess chunk, BlockPos pos) {
        BlockState blockState = chunk.getBlockState(pos);
        if (blockState.is(BlockTags.LEAVES)) {
            chunk.markPosForPostprocessing(pos);
        }

        FluidState fluidState = chunk.getFluidState(pos);
        if (!fluidState.isEmpty()) {
            chunk.markPosForPostprocessing(pos);
        }

    }

    public static void addAroundOldChunksCarvingMaskFilter(WorldGenLevel worldGenLevel, ProtoChunk protoChunk) {
        ChunkPos chunkPos = protoChunk.getPos();
        Blender.DistanceGetter distanceGetter = makeOldChunkDistanceGetter(protoChunk.isOldNoiseGeneration(), BlendingData.sideByGenerationAge(worldGenLevel, chunkPos.x, chunkPos.z, true));
        if (distanceGetter != null) {
            CarvingMask.Mask mask = (i, j, k) -> {
                double d = (double)i + 0.5D + SHIFT_NOISE.getValue((double)i, (double)j, (double)k) * 4.0D;
                double e = (double)j + 0.5D + SHIFT_NOISE.getValue((double)j, (double)k, (double)i) * 4.0D;
                double f = (double)k + 0.5D + SHIFT_NOISE.getValue((double)k, (double)i, (double)j) * 4.0D;
                return distanceGetter.getDistance(d, e, f) < 4.0D;
            };
            Stream.of(GenerationStep.Carving.values()).map(protoChunk::getOrCreateCarvingMask).forEach((carvingMask) -> {
                carvingMask.setAdditionalMask(mask);
            });
        }
    }

    @Nullable
    public static Blender.DistanceGetter makeOldChunkDistanceGetter(boolean bl, Set<Direction8> set) {
        if (!bl && set.isEmpty()) {
            return null;
        } else {
            List<Blender.DistanceGetter> list = Lists.newArrayList();
            if (bl) {
                list.add(makeOffsetOldChunkDistanceGetter((Direction8)null));
            }

            set.forEach((direction8) -> {
                list.add(makeOffsetOldChunkDistanceGetter(direction8));
            });
            return (d, e, f) -> {
                double g = Double.POSITIVE_INFINITY;

                for(Blender.DistanceGetter distanceGetter : list) {
                    double h = distanceGetter.getDistance(d, e, f);
                    if (h < g) {
                        g = h;
                    }
                }

                return g;
            };
        }
    }

    private static Blender.DistanceGetter makeOffsetOldChunkDistanceGetter(@Nullable Direction8 direction8) {
        double d = 0.0D;
        double e = 0.0D;
        if (direction8 != null) {
            for(Direction direction : direction8.getDirections()) {
                d += (double)(direction.getStepX() * 16);
                e += (double)(direction.getStepZ() * 16);
            }
        }

        double f = d;
        double g = e;
        return (f, gx, h) -> {
            return distanceToCube(f - 8.0D - f, gx - OLD_CHUNK_CENTER_Y, h - 8.0D - g, 8.0D, OLD_CHUNK_Y_RADIUS, 8.0D);
        };
    }

    private static double distanceToCube(double d, double e, double f, double g, double h, double i) {
        double j = Math.abs(d) - g;
        double k = Math.abs(e) - h;
        double l = Math.abs(f) - i;
        return Mth.length(Math.max(0.0D, j), Math.max(0.0D, k), Math.max(0.0D, l));
    }

    public static record BlendingOutput(double alpha, double blendingOffset) {
    }

    interface CellValueGetter {
        double get(BlendingData blendingData, int i, int j, int k);
    }

    public interface DistanceGetter {
        double getDistance(double d, double e, double f);
    }
}
