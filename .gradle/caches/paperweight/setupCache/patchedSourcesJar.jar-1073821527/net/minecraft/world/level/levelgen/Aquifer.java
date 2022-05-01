package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.lang3.mutable.MutableDouble;

public interface Aquifer {
    static Aquifer create(NoiseChunk chunkNoiseSampler, ChunkPos chunkPos, DensityFunction densityFunction, DensityFunction densityFunction2, DensityFunction densityFunction3, DensityFunction densityFunction4, PositionalRandomFactory randomDeriver, int minY, int height, Aquifer.FluidPicker fluidLevelSampler) {
        return new Aquifer.NoiseBasedAquifer(chunkNoiseSampler, chunkPos, densityFunction, densityFunction2, densityFunction3, densityFunction4, randomDeriver, minY, height, fluidLevelSampler);
    }

    static Aquifer createDisabled(Aquifer.FluidPicker fluidLevelSampler) {
        return new Aquifer() {
            @Nullable
            @Override
            public BlockState computeSubstance(DensityFunction.FunctionContext functionContext, double d) {
                return d > 0.0D ? null : fluidLevelSampler.computeFluid(functionContext.blockX(), functionContext.blockY(), functionContext.blockZ()).at(functionContext.blockY());
            }

            @Override
            public boolean shouldScheduleFluidUpdate() {
                return false;
            }
        };
    }

    @Nullable
    BlockState computeSubstance(DensityFunction.FunctionContext functionContext, double d);

    boolean shouldScheduleFluidUpdate();

    public interface FluidPicker {
        Aquifer.FluidStatus computeFluid(int x, int y, int z);
    }

    public static final class FluidStatus {
        final int fluidLevel;
        final BlockState fluidType;

        public FluidStatus(int y, BlockState state) {
            this.fluidLevel = y;
            this.fluidType = state;
        }

        public BlockState at(int y) {
            return y < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
        }
    }

    public static class NoiseBasedAquifer implements Aquifer {
        private static final int X_RANGE = 10;
        private static final int Y_RANGE = 9;
        private static final int Z_RANGE = 10;
        private static final int X_SEPARATION = 6;
        private static final int Y_SEPARATION = 3;
        private static final int Z_SEPARATION = 6;
        private static final int X_SPACING = 16;
        private static final int Y_SPACING = 12;
        private static final int Z_SPACING = 16;
        private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
        private static final double FLOWING_UPDATE_SIMULARITY = similarity(Mth.square(10), Mth.square(12));
        private final NoiseChunk noiseChunk;
        private final DensityFunction barrierNoise;
        private final DensityFunction fluidLevelFloodednessNoise;
        private final DensityFunction fluidLevelSpreadNoise;
        private final DensityFunction lavaNoise;
        private final PositionalRandomFactory positionalRandomFactory;
        private final Aquifer.FluidStatus[] aquiferCache;
        private final long[] aquiferLocationCache;
        private final Aquifer.FluidPicker globalFluidPicker;
        private boolean shouldScheduleFluidUpdate;
        private final int minGridX;
        private final int minGridY;
        private final int minGridZ;
        private final int gridSizeX;
        private final int gridSizeZ;
        private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{{-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}};

        NoiseBasedAquifer(NoiseChunk chunkNoiseSampler, ChunkPos chunkPos, DensityFunction densityFunction, DensityFunction densityFunction2, DensityFunction densityFunction3, DensityFunction densityFunction4, PositionalRandomFactory randomDeriver, int minY, int height, Aquifer.FluidPicker fluidLevelSampler) {
            this.noiseChunk = chunkNoiseSampler;
            this.barrierNoise = densityFunction;
            this.fluidLevelFloodednessNoise = densityFunction2;
            this.fluidLevelSpreadNoise = densityFunction3;
            this.lavaNoise = densityFunction4;
            this.positionalRandomFactory = randomDeriver;
            this.minGridX = this.gridX(chunkPos.getMinBlockX()) - 1;
            this.globalFluidPicker = fluidLevelSampler;
            int i = this.gridX(chunkPos.getMaxBlockX()) + 1;
            this.gridSizeX = i - this.minGridX + 1;
            this.minGridY = this.gridY(minY) - 1;
            int j = this.gridY(minY + height) + 1;
            int k = j - this.minGridY + 1;
            this.minGridZ = this.gridZ(chunkPos.getMinBlockZ()) - 1;
            int l = this.gridZ(chunkPos.getMaxBlockZ()) + 1;
            this.gridSizeZ = l - this.minGridZ + 1;
            int m = this.gridSizeX * k * this.gridSizeZ;
            this.aquiferCache = new Aquifer.FluidStatus[m];
            this.aquiferLocationCache = new long[m];
            Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
        }

        private int getIndex(int x, int y, int z) {
            int i = x - this.minGridX;
            int j = y - this.minGridY;
            int k = z - this.minGridZ;
            return (j * this.gridSizeZ + k) * this.gridSizeX + i;
        }

        @Nullable
        @Override
        public BlockState computeSubstance(DensityFunction.FunctionContext functionContext, double d) {
            int i = functionContext.blockX();
            int j = functionContext.blockY();
            int k = functionContext.blockZ();
            if (d > 0.0D) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            } else {
                Aquifer.FluidStatus fluidStatus = this.globalFluidPicker.computeFluid(i, j, k);
                if (fluidStatus.at(j).is(Blocks.LAVA)) {
                    this.shouldScheduleFluidUpdate = false;
                    return Blocks.LAVA.defaultBlockState();
                } else {
                    int l = Math.floorDiv(i - 5, 16);
                    int m = Math.floorDiv(j + 1, 12);
                    int n = Math.floorDiv(k - 5, 16);
                    int o = Integer.MAX_VALUE;
                    int p = Integer.MAX_VALUE;
                    int q = Integer.MAX_VALUE;
                    long r = 0L;
                    long s = 0L;
                    long t = 0L;

                    for(int u = 0; u <= 1; ++u) {
                        for(int v = -1; v <= 1; ++v) {
                            for(int w = 0; w <= 1; ++w) {
                                int x = l + u;
                                int y = m + v;
                                int z = n + w;
                                int aa = this.getIndex(x, y, z);
                                long ab = this.aquiferLocationCache[aa];
                                long ac;
                                if (ab != Long.MAX_VALUE) {
                                    ac = ab;
                                } else {
                                    RandomSource randomSource = this.positionalRandomFactory.at(x, y, z);
                                    ac = BlockPos.asLong(x * 16 + randomSource.nextInt(10), y * 12 + randomSource.nextInt(9), z * 16 + randomSource.nextInt(10));
                                    this.aquiferLocationCache[aa] = ac;
                                }

                                int ae = BlockPos.getX(ac) - i;
                                int af = BlockPos.getY(ac) - j;
                                int ag = BlockPos.getZ(ac) - k;
                                int ah = ae * ae + af * af + ag * ag;
                                if (o >= ah) {
                                    t = s;
                                    s = r;
                                    r = ac;
                                    q = p;
                                    p = o;
                                    o = ah;
                                } else if (p >= ah) {
                                    t = s;
                                    s = ac;
                                    q = p;
                                    p = ah;
                                } else if (q >= ah) {
                                    t = ac;
                                    q = ah;
                                }
                            }
                        }
                    }

                    Aquifer.FluidStatus fluidStatus2 = this.getAquiferStatus(r);
                    double e = similarity(o, p);
                    BlockState blockState = fluidStatus2.at(j);
                    if (e <= 0.0D) {
                        this.shouldScheduleFluidUpdate = e >= FLOWING_UPDATE_SIMULARITY;
                        return blockState;
                    } else if (blockState.is(Blocks.WATER) && this.globalFluidPicker.computeFluid(i, j - 1, k).at(j - 1).is(Blocks.LAVA)) {
                        this.shouldScheduleFluidUpdate = true;
                        return blockState;
                    } else {
                        MutableDouble mutableDouble = new MutableDouble(Double.NaN);
                        Aquifer.FluidStatus fluidStatus3 = this.getAquiferStatus(s);
                        double f = e * this.calculatePressure(functionContext, mutableDouble, fluidStatus2, fluidStatus3);
                        if (d + f > 0.0D) {
                            this.shouldScheduleFluidUpdate = false;
                            return null;
                        } else {
                            Aquifer.FluidStatus fluidStatus4 = this.getAquiferStatus(t);
                            double g = similarity(o, q);
                            if (g > 0.0D) {
                                double h = e * g * this.calculatePressure(functionContext, mutableDouble, fluidStatus2, fluidStatus4);
                                if (d + h > 0.0D) {
                                    this.shouldScheduleFluidUpdate = false;
                                    return null;
                                }
                            }

                            double ai = similarity(p, q);
                            if (ai > 0.0D) {
                                double aj = e * ai * this.calculatePressure(functionContext, mutableDouble, fluidStatus3, fluidStatus4);
                                if (d + aj > 0.0D) {
                                    this.shouldScheduleFluidUpdate = false;
                                    return null;
                                }
                            }

                            this.shouldScheduleFluidUpdate = true;
                            return blockState;
                        }
                    }
                }
            }
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return this.shouldScheduleFluidUpdate;
        }

        private static double similarity(int i, int a) {
            double d = 25.0D;
            return 1.0D - (double)Math.abs(a - i) / 25.0D;
        }

        private double calculatePressure(DensityFunction.FunctionContext functionContext, MutableDouble mutableDouble, Aquifer.FluidStatus fluidStatus, Aquifer.FluidStatus fluidStatus2) {
            int i = functionContext.blockY();
            BlockState blockState = fluidStatus.at(i);
            BlockState blockState2 = fluidStatus2.at(i);
            if ((!blockState.is(Blocks.LAVA) || !blockState2.is(Blocks.WATER)) && (!blockState.is(Blocks.WATER) || !blockState2.is(Blocks.LAVA))) {
                int j = Math.abs(fluidStatus.fluidLevel - fluidStatus2.fluidLevel);
                if (j == 0) {
                    return 0.0D;
                } else {
                    double d = 0.5D * (double)(fluidStatus.fluidLevel + fluidStatus2.fluidLevel);
                    double e = (double)i + 0.5D - d;
                    double f = (double)j / 2.0D;
                    double g = 0.0D;
                    double h = 2.5D;
                    double k = 1.5D;
                    double l = 3.0D;
                    double m = 10.0D;
                    double n = 3.0D;
                    double o = f - Math.abs(e);
                    double q;
                    if (e > 0.0D) {
                        double p = 0.0D + o;
                        if (p > 0.0D) {
                            q = p / 1.5D;
                        } else {
                            q = p / 2.5D;
                        }
                    } else {
                        double s = 3.0D + o;
                        if (s > 0.0D) {
                            q = s / 3.0D;
                        } else {
                            q = s / 10.0D;
                        }
                    }

                    double v = 2.0D;
                    double z;
                    if (!(q < -2.0D) && !(q > 2.0D)) {
                        double x = mutableDouble.getValue();
                        if (Double.isNaN(x)) {
                            double y = this.barrierNoise.compute(functionContext);
                            mutableDouble.setValue(y);
                            z = y;
                        } else {
                            z = x;
                        }
                    } else {
                        z = 0.0D;
                    }

                    return 2.0D * (z + q);
                }
            } else {
                return 2.0D;
            }
        }

        private int gridX(int x) {
            return Math.floorDiv(x, 16);
        }

        private int gridY(int y) {
            return Math.floorDiv(y, 12);
        }

        private int gridZ(int z) {
            return Math.floorDiv(z, 16);
        }

        private Aquifer.FluidStatus getAquiferStatus(long pos) {
            int i = BlockPos.getX(pos);
            int j = BlockPos.getY(pos);
            int k = BlockPos.getZ(pos);
            int l = this.gridX(i);
            int m = this.gridY(j);
            int n = this.gridZ(k);
            int o = this.getIndex(l, m, n);
            Aquifer.FluidStatus fluidStatus = this.aquiferCache[o];
            if (fluidStatus != null) {
                return fluidStatus;
            } else {
                Aquifer.FluidStatus fluidStatus2 = this.computeFluid(i, j, k);
                this.aquiferCache[o] = fluidStatus2;
                return fluidStatus2;
            }
        }

        private Aquifer.FluidStatus computeFluid(int i, int j, int k) {
            Aquifer.FluidStatus fluidStatus = this.globalFluidPicker.computeFluid(i, j, k);
            int l = Integer.MAX_VALUE;
            int m = j + 12;
            int n = j - 12;
            boolean bl = false;

            for(int[] is : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
                int o = i + SectionPos.sectionToBlockCoord(is[0]);
                int p = k + SectionPos.sectionToBlockCoord(is[1]);
                int q = this.noiseChunk.preliminarySurfaceLevel(o, p);
                int r = q + 8;
                boolean bl2 = is[0] == 0 && is[1] == 0;
                if (bl2 && n > r) {
                    return fluidStatus;
                }

                boolean bl3 = m > r;
                if (bl3 || bl2) {
                    Aquifer.FluidStatus fluidStatus2 = this.globalFluidPicker.computeFluid(o, r, p);
                    if (!fluidStatus2.at(r).isAir()) {
                        if (bl2) {
                            bl = true;
                        }

                        if (bl3) {
                            return fluidStatus2;
                        }
                    }
                }

                l = Math.min(l, q);
            }

            int s = l + 8 - j;
            int t = 64;
            double d = bl ? Mth.clampedMap((double)s, 0.0D, 64.0D, 1.0D, 0.0D) : 0.0D;
            double e = Mth.clamp(this.fluidLevelFloodednessNoise.compute(new DensityFunction.SinglePointContext(i, j, k)), -1.0D, 1.0D);
            double f = Mth.map(d, 1.0D, 0.0D, -0.3D, 0.8D);
            if (e > f) {
                return fluidStatus;
            } else {
                double g = Mth.map(d, 1.0D, 0.0D, -0.8D, 0.4D);
                if (e <= g) {
                    return new Aquifer.FluidStatus(DimensionType.WAY_BELOW_MIN_Y, fluidStatus.fluidType);
                } else {
                    int u = 16;
                    int v = 40;
                    int w = Math.floorDiv(i, 16);
                    int x = Math.floorDiv(j, 40);
                    int y = Math.floorDiv(k, 16);
                    int z = x * 40 + 20;
                    int aa = 10;
                    double h = this.fluidLevelSpreadNoise.compute(new DensityFunction.SinglePointContext(w, x, y)) * 10.0D;
                    int ab = Mth.quantize(h, 3);
                    int ac = z + ab;
                    int ad = Math.min(l, ac);
                    if (ac <= -10) {
                        int ae = 64;
                        int af = 40;
                        int ag = Math.floorDiv(i, 64);
                        int ah = Math.floorDiv(j, 40);
                        int ai = Math.floorDiv(k, 64);
                        double aj = this.lavaNoise.compute(new DensityFunction.SinglePointContext(ag, ah, ai));
                        if (Math.abs(aj) > 0.3D) {
                            return new Aquifer.FluidStatus(ad, Blocks.LAVA.defaultBlockState());
                        }
                    }

                    return new Aquifer.FluidStatus(ad, fluidStatus.fluidType);
                }
            }
        }
    }
}
