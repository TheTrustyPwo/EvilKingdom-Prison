package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.DripstoneClusterConfiguration;

public class DripstoneClusterFeature extends Feature<DripstoneClusterConfiguration> {
    public DripstoneClusterFeature(Codec<DripstoneClusterConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<DripstoneClusterConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        DripstoneClusterConfiguration dripstoneClusterConfiguration = context.config();
        Random random = context.random();
        if (!DripstoneUtils.isEmptyOrWater(worldGenLevel, blockPos)) {
            return false;
        } else {
            int i = dripstoneClusterConfiguration.height.sample(random);
            float f = dripstoneClusterConfiguration.wetness.sample(random);
            float g = dripstoneClusterConfiguration.density.sample(random);
            int j = dripstoneClusterConfiguration.radius.sample(random);
            int k = dripstoneClusterConfiguration.radius.sample(random);

            for(int l = -j; l <= j; ++l) {
                for(int m = -k; m <= k; ++m) {
                    double d = this.getChanceOfStalagmiteOrStalactite(j, k, l, m, dripstoneClusterConfiguration);
                    BlockPos blockPos2 = blockPos.offset(l, 0, m);
                    this.placeColumn(worldGenLevel, random, blockPos2, l, m, f, d, i, g, dripstoneClusterConfiguration);
                }
            }

            return true;
        }
    }

    private void placeColumn(WorldGenLevel world, Random random, BlockPos pos, int localX, int localZ, float wetness, double dripstoneChance, int height, float density, DripstoneClusterConfiguration config) {
        Optional<Column> optional = Column.scan(world, pos, config.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isNeitherEmptyNorWater);
        if (optional.isPresent()) {
            OptionalInt optionalInt = optional.get().getCeiling();
            OptionalInt optionalInt2 = optional.get().getFloor();
            if (optionalInt.isPresent() || optionalInt2.isPresent()) {
                boolean bl = random.nextFloat() < wetness;
                Column column;
                if (bl && optionalInt2.isPresent() && this.canPlacePool(world, pos.atY(optionalInt2.getAsInt()))) {
                    int i = optionalInt2.getAsInt();
                    column = optional.get().withFloor(OptionalInt.of(i - 1));
                    world.setBlock(pos.atY(i), Blocks.WATER.defaultBlockState(), 2);
                } else {
                    column = optional.get();
                }

                OptionalInt optionalInt3 = column.getFloor();
                boolean bl2 = random.nextDouble() < dripstoneChance;
                int m;
                if (optionalInt.isPresent() && bl2 && !this.isLava(world, pos.atY(optionalInt.getAsInt()))) {
                    int j = config.dripstoneBlockLayerThickness.sample(random);
                    this.replaceBlocksWithDripstoneBlocks(world, pos.atY(optionalInt.getAsInt()), j, Direction.UP);
                    int k;
                    if (optionalInt3.isPresent()) {
                        k = Math.min(height, optionalInt.getAsInt() - optionalInt3.getAsInt());
                    } else {
                        k = height;
                    }

                    m = this.getDripstoneHeight(random, localX, localZ, density, k, config);
                } else {
                    m = 0;
                }

                boolean bl3 = random.nextDouble() < dripstoneChance;
                int p;
                if (optionalInt3.isPresent() && bl3 && !this.isLava(world, pos.atY(optionalInt3.getAsInt()))) {
                    int o = config.dripstoneBlockLayerThickness.sample(random);
                    this.replaceBlocksWithDripstoneBlocks(world, pos.atY(optionalInt3.getAsInt()), o, Direction.DOWN);
                    if (optionalInt.isPresent()) {
                        p = Math.max(0, m + Mth.randomBetweenInclusive(random, -config.maxStalagmiteStalactiteHeightDiff, config.maxStalagmiteStalactiteHeightDiff));
                    } else {
                        p = this.getDripstoneHeight(random, localX, localZ, density, height, config);
                    }
                } else {
                    p = 0;
                }

                int z;
                int y;
                if (optionalInt.isPresent() && optionalInt3.isPresent() && optionalInt.getAsInt() - m <= optionalInt3.getAsInt() + p) {
                    int s = optionalInt3.getAsInt();
                    int t = optionalInt.getAsInt();
                    int u = Math.max(t - m, s + 1);
                    int v = Math.min(s + p, t - 1);
                    int w = Mth.randomBetweenInclusive(random, u, v + 1);
                    int x = w - 1;
                    y = t - w;
                    z = x - s;
                } else {
                    y = m;
                    z = p;
                }

                boolean bl4 = random.nextBoolean() && y > 0 && z > 0 && column.getHeight().isPresent() && y + z == column.getHeight().getAsInt();
                if (optionalInt.isPresent()) {
                    DripstoneUtils.growPointedDripstone(world, pos.atY(optionalInt.getAsInt() - 1), Direction.DOWN, y, bl4);
                }

                if (optionalInt3.isPresent()) {
                    DripstoneUtils.growPointedDripstone(world, pos.atY(optionalInt3.getAsInt() + 1), Direction.UP, z, bl4);
                }

            }
        }
    }

    private boolean isLava(LevelReader world, BlockPos pos) {
        return world.getBlockState(pos).is(Blocks.LAVA);
    }

    private int getDripstoneHeight(Random random, int localX, int localZ, float density, int height, DripstoneClusterConfiguration config) {
        if (random.nextFloat() > density) {
            return 0;
        } else {
            int i = Math.abs(localX) + Math.abs(localZ);
            float f = (float)Mth.clampedMap((double)i, 0.0D, (double)config.maxDistanceFromCenterAffectingHeightBias, (double)height / 2.0D, 0.0D);
            return (int)randomBetweenBiased(random, 0.0F, (float)height, f, (float)config.heightDeviation);
        }
    }

    private boolean canPlacePool(WorldGenLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.is(Blocks.WATER) && !blockState.is(Blocks.DRIPSTONE_BLOCK) && !blockState.is(Blocks.POINTED_DRIPSTONE)) {
            if (world.getBlockState(pos.above()).getFluidState().is(FluidTags.WATER)) {
                return false;
            } else {
                for(Direction direction : Direction.Plane.HORIZONTAL) {
                    if (!this.canBeAdjacentToWater(world, pos.relative(direction))) {
                        return false;
                    }
                }

                return this.canBeAdjacentToWater(world, pos.below());
            }
        } else {
            return false;
        }
    }

    private boolean canBeAdjacentToWater(LevelAccessor world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.is(BlockTags.BASE_STONE_OVERWORLD) || blockState.getFluidState().is(FluidTags.WATER);
    }

    private void replaceBlocksWithDripstoneBlocks(WorldGenLevel world, BlockPos pos, int height, Direction direction) {
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

        for(int i = 0; i < height; ++i) {
            if (!DripstoneUtils.placeDripstoneBlockIfPossible(world, mutableBlockPos)) {
                return;
            }

            mutableBlockPos.move(direction);
        }

    }

    private double getChanceOfStalagmiteOrStalactite(int radiusX, int radiusZ, int localX, int localZ, DripstoneClusterConfiguration config) {
        int i = radiusX - Math.abs(localX);
        int j = radiusZ - Math.abs(localZ);
        int k = Math.min(i, j);
        return (double)Mth.clampedMap((float)k, 0.0F, (float)config.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn, config.chanceOfDripstoneColumnAtMaxDistanceFromCenter, 1.0F);
    }

    private static float randomBetweenBiased(Random random, float min, float max, float mean, float deviation) {
        return ClampedNormalFloat.sample(random, mean, deviation, min, max);
    }
}
