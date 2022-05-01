package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.material.Material;

public class IcebergFeature extends Feature<BlockStateConfiguration> {
    public IcebergFeature(Codec<BlockStateConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockStateConfiguration> context) {
        BlockPos blockPos = context.origin();
        WorldGenLevel worldGenLevel = context.level();
        blockPos = new BlockPos(blockPos.getX(), context.chunkGenerator().getSeaLevel(), blockPos.getZ());
        Random random = context.random();
        boolean bl = random.nextDouble() > 0.7D;
        BlockState blockState = (context.config()).state;
        double d = random.nextDouble() * 2.0D * Math.PI;
        int i = 11 - random.nextInt(5);
        int j = 3 + random.nextInt(3);
        boolean bl2 = random.nextDouble() > 0.7D;
        int k = 11;
        int l = bl2 ? random.nextInt(6) + 6 : random.nextInt(15) + 3;
        if (!bl2 && random.nextDouble() > 0.9D) {
            l += random.nextInt(19) + 7;
        }

        int m = Math.min(l + random.nextInt(11), 18);
        int n = Math.min(l + random.nextInt(7) - random.nextInt(5), 11);
        int o = bl2 ? i : 11;

        for(int p = -o; p < o; ++p) {
            for(int q = -o; q < o; ++q) {
                for(int r = 0; r < l; ++r) {
                    int s = bl2 ? this.heightDependentRadiusEllipse(r, l, n) : this.heightDependentRadiusRound(random, r, l, n);
                    if (bl2 || p < s) {
                        this.generateIcebergBlock(worldGenLevel, random, blockPos, l, p, r, q, s, o, bl2, j, d, bl, blockState);
                    }
                }
            }
        }

        this.smooth(worldGenLevel, blockPos, n, l, bl2, i);

        for(int t = -o; t < o; ++t) {
            for(int u = -o; u < o; ++u) {
                for(int v = -1; v > -m; --v) {
                    int w = bl2 ? Mth.ceil((float)o * (1.0F - (float)Math.pow((double)v, 2.0D) / ((float)m * 8.0F))) : o;
                    int x = this.heightDependentRadiusSteep(random, -v, m, n);
                    if (t < x) {
                        this.generateIcebergBlock(worldGenLevel, random, blockPos, m, t, v, u, x, w, bl2, j, d, bl, blockState);
                    }
                }
            }
        }

        boolean bl3 = bl2 ? random.nextDouble() > 0.1D : random.nextDouble() > 0.7D;
        if (bl3) {
            this.generateCutOut(random, worldGenLevel, n, l, blockPos, bl2, i, d, j);
        }

        return true;
    }

    private void generateCutOut(Random random, LevelAccessor world, int i, int j, BlockPos pos, boolean bl, int k, double d, int l) {
        int m = random.nextBoolean() ? -1 : 1;
        int n = random.nextBoolean() ? -1 : 1;
        int o = random.nextInt(Math.max(i / 2 - 2, 1));
        if (random.nextBoolean()) {
            o = i / 2 + 1 - random.nextInt(Math.max(i - i / 2 - 1, 1));
        }

        int p = random.nextInt(Math.max(i / 2 - 2, 1));
        if (random.nextBoolean()) {
            p = i / 2 + 1 - random.nextInt(Math.max(i - i / 2 - 1, 1));
        }

        if (bl) {
            o = p = random.nextInt(Math.max(k - 5, 1));
        }

        BlockPos blockPos = new BlockPos(m * o, 0, n * p);
        double e = bl ? d + (Math.PI / 2D) : random.nextDouble() * 2.0D * Math.PI;

        for(int q = 0; q < j - 3; ++q) {
            int r = this.heightDependentRadiusRound(random, q, j, i);
            this.carve(r, q, pos, world, false, e, blockPos, k, l);
        }

        for(int s = -1; s > -j + random.nextInt(5); --s) {
            int t = this.heightDependentRadiusSteep(random, -s, j, i);
            this.carve(t, s, pos, world, true, e, blockPos, k, l);
        }

    }

    private void carve(int i, int y, BlockPos pos, LevelAccessor world, boolean placeWater, double d, BlockPos blockPos, int j, int k) {
        int l = i + 1 + j / 3;
        int m = Math.min(i - 3, 3) + k / 2 - 1;

        for(int n = -l; n < l; ++n) {
            for(int o = -l; o < l; ++o) {
                double e = this.signedDistanceEllipse(n, o, blockPos, l, m, d);
                if (e < 0.0D) {
                    BlockPos blockPos2 = pos.offset(n, y, o);
                    BlockState blockState = world.getBlockState(blockPos2);
                    if (isIcebergState(blockState) || blockState.is(Blocks.SNOW_BLOCK)) {
                        if (placeWater) {
                            this.setBlock(world, blockPos2, Blocks.WATER.defaultBlockState());
                        } else {
                            this.setBlock(world, blockPos2, Blocks.AIR.defaultBlockState());
                            this.removeFloatingSnowLayer(world, blockPos2);
                        }
                    }
                }
            }
        }

    }

    private void removeFloatingSnowLayer(LevelAccessor world, BlockPos pos) {
        if (world.getBlockState(pos.above()).is(Blocks.SNOW)) {
            this.setBlock(world, pos.above(), Blocks.AIR.defaultBlockState());
        }

    }

    private void generateIcebergBlock(LevelAccessor world, Random random, BlockPos pos, int height, int offsetX, int offsetY, int offsetZ, int i, int j, boolean bl, int k, double randomSine, boolean placeSnow, BlockState state) {
        double d = bl ? this.signedDistanceEllipse(offsetX, offsetZ, BlockPos.ZERO, j, this.getEllipseC(offsetY, height, k), randomSine) : this.signedDistanceCircle(offsetX, offsetZ, BlockPos.ZERO, i, random);
        if (d < 0.0D) {
            BlockPos blockPos = pos.offset(offsetX, offsetY, offsetZ);
            double e = bl ? -0.5D : (double)(-6 - random.nextInt(3));
            if (d > e && random.nextDouble() > 0.9D) {
                return;
            }

            this.setIcebergBlock(blockPos, world, random, height - offsetY, height, bl, placeSnow, state);
        }

    }

    private void setIcebergBlock(BlockPos pos, LevelAccessor world, Random random, int heightRemaining, int height, boolean lessSnow, boolean placeSnow, BlockState state) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.getMaterial() == Material.AIR || blockState.is(Blocks.SNOW_BLOCK) || blockState.is(Blocks.ICE) || blockState.is(Blocks.WATER)) {
            boolean bl = !lessSnow || random.nextDouble() > 0.05D;
            int i = lessSnow ? 3 : 2;
            if (placeSnow && !blockState.is(Blocks.WATER) && (double)heightRemaining <= (double)random.nextInt(Math.max(1, height / i)) + (double)height * 0.6D && bl) {
                this.setBlock(world, pos, Blocks.SNOW_BLOCK.defaultBlockState());
            } else {
                this.setBlock(world, pos, state);
            }
        }

    }

    private int getEllipseC(int y, int height, int value) {
        int i = value;
        if (y > 0 && height - y <= 3) {
            i = value - (4 - (height - y));
        }

        return i;
    }

    private double signedDistanceCircle(int x, int z, BlockPos pos, int i, Random random) {
        float f = 10.0F * Mth.clamp(random.nextFloat(), 0.2F, 0.8F) / (float)i;
        return (double)f + Math.pow((double)(x - pos.getX()), 2.0D) + Math.pow((double)(z - pos.getZ()), 2.0D) - Math.pow((double)i, 2.0D);
    }

    private double signedDistanceEllipse(int x, int z, BlockPos pos, int divisor1, int divisor2, double randomSine) {
        return Math.pow(((double)(x - pos.getX()) * Math.cos(randomSine) - (double)(z - pos.getZ()) * Math.sin(randomSine)) / (double)divisor1, 2.0D) + Math.pow(((double)(x - pos.getX()) * Math.sin(randomSine) + (double)(z - pos.getZ()) * Math.cos(randomSine)) / (double)divisor2, 2.0D) - 1.0D;
    }

    private int heightDependentRadiusRound(Random random, int y, int height, int factor) {
        float f = 3.5F - random.nextFloat();
        float g = (1.0F - (float)Math.pow((double)y, 2.0D) / ((float)height * f)) * (float)factor;
        if (height > 15 + random.nextInt(5)) {
            int i = y < 3 + random.nextInt(6) ? y / 2 : y;
            g = (1.0F - (float)i / ((float)height * f * 0.4F)) * (float)factor;
        }

        return Mth.ceil(g / 2.0F);
    }

    private int heightDependentRadiusEllipse(int y, int height, int factor) {
        float f = 1.0F;
        float g = (1.0F - (float)Math.pow((double)y, 2.0D) / ((float)height * 1.0F)) * (float)factor;
        return Mth.ceil(g / 2.0F);
    }

    private int heightDependentRadiusSteep(Random random, int y, int height, int factor) {
        float f = 1.0F + random.nextFloat() / 2.0F;
        float g = (1.0F - (float)y / ((float)height * f)) * (float)factor;
        return Mth.ceil(g / 2.0F);
    }

    private static boolean isIcebergState(BlockState state) {
        return state.is(Blocks.PACKED_ICE) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.BLUE_ICE);
    }

    private boolean belowIsAir(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos.below()).getMaterial() == Material.AIR;
    }

    private void smooth(LevelAccessor world, BlockPos pos, int i, int height, boolean bl, int j) {
        int k = bl ? j : i / 2;

        for(int l = -k; l <= k; ++l) {
            for(int m = -k; m <= k; ++m) {
                for(int n = 0; n <= height; ++n) {
                    BlockPos blockPos = pos.offset(l, n, m);
                    BlockState blockState = world.getBlockState(blockPos);
                    if (isIcebergState(blockState) || blockState.is(Blocks.SNOW)) {
                        if (this.belowIsAir(world, blockPos)) {
                            this.setBlock(world, blockPos, Blocks.AIR.defaultBlockState());
                            this.setBlock(world, blockPos.above(), Blocks.AIR.defaultBlockState());
                        } else if (isIcebergState(blockState)) {
                            BlockState[] blockStates = new BlockState[]{world.getBlockState(blockPos.west()), world.getBlockState(blockPos.east()), world.getBlockState(blockPos.north()), world.getBlockState(blockPos.south())};
                            int o = 0;

                            for(BlockState blockState2 : blockStates) {
                                if (!isIcebergState(blockState2)) {
                                    ++o;
                                }
                            }

                            if (o >= 3) {
                                this.setBlock(world, blockPos, Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }

    }
}
