package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.ColumnFeatureConfiguration;

public class BasaltColumnsFeature extends Feature<ColumnFeatureConfiguration> {
    private static final ImmutableList<Block> CANNOT_PLACE_ON = ImmutableList.of(Blocks.LAVA, Blocks.BEDROCK, Blocks.MAGMA_BLOCK, Blocks.SOUL_SAND, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_WART, Blocks.CHEST, Blocks.SPAWNER);
    private static final int CLUSTERED_REACH = 5;
    private static final int CLUSTERED_SIZE = 50;
    private static final int UNCLUSTERED_REACH = 8;
    private static final int UNCLUSTERED_SIZE = 15;

    public BasaltColumnsFeature(Codec<ColumnFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<ColumnFeatureConfiguration> context) {
        int i = context.chunkGenerator().getSeaLevel();
        BlockPos blockPos = context.origin();
        WorldGenLevel worldGenLevel = context.level();
        Random random = context.random();
        ColumnFeatureConfiguration columnFeatureConfiguration = context.config();
        if (!canPlaceAt(worldGenLevel, i, blockPos.mutable())) {
            return false;
        } else {
            int j = columnFeatureConfiguration.height().sample(random);
            boolean bl = random.nextFloat() < 0.9F;
            int k = Math.min(j, bl ? 5 : 8);
            int l = bl ? 50 : 15;
            boolean bl2 = false;

            for(BlockPos blockPos2 : BlockPos.randomBetweenClosed(random, l, blockPos.getX() - k, blockPos.getY(), blockPos.getZ() - k, blockPos.getX() + k, blockPos.getY(), blockPos.getZ() + k)) {
                int m = j - blockPos2.distManhattan(blockPos);
                if (m >= 0) {
                    bl2 |= this.placeColumn(worldGenLevel, i, blockPos2, m, columnFeatureConfiguration.reach().sample(random));
                }
            }

            return bl2;
        }
    }

    private boolean placeColumn(LevelAccessor world, int seaLevel, BlockPos pos, int height, int reach) {
        boolean bl = false;

        for(BlockPos blockPos : BlockPos.betweenClosed(pos.getX() - reach, pos.getY(), pos.getZ() - reach, pos.getX() + reach, pos.getY(), pos.getZ() + reach)) {
            int i = blockPos.distManhattan(pos);
            BlockPos blockPos2 = isAirOrLavaOcean(world, seaLevel, blockPos) ? findSurface(world, seaLevel, blockPos.mutable(), i) : findAir(world, blockPos.mutable(), i);
            if (blockPos2 != null) {
                int j = height - i / 2;

                for(BlockPos.MutableBlockPos mutableBlockPos = blockPos2.mutable(); j >= 0; --j) {
                    if (isAirOrLavaOcean(world, seaLevel, mutableBlockPos)) {
                        this.setBlock(world, mutableBlockPos, Blocks.BASALT.defaultBlockState());
                        mutableBlockPos.move(Direction.UP);
                        bl = true;
                    } else {
                        if (!world.getBlockState(mutableBlockPos).is(Blocks.BASALT)) {
                            break;
                        }

                        mutableBlockPos.move(Direction.UP);
                    }
                }
            }
        }

        return bl;
    }

    @Nullable
    private static BlockPos findSurface(LevelAccessor world, int seaLevel, BlockPos.MutableBlockPos mutablePos, int distance) {
        while(mutablePos.getY() > world.getMinBuildHeight() + 1 && distance > 0) {
            --distance;
            if (canPlaceAt(world, seaLevel, mutablePos)) {
                return mutablePos;
            }

            mutablePos.move(Direction.DOWN);
        }

        return null;
    }

    private static boolean canPlaceAt(LevelAccessor world, int seaLevel, BlockPos.MutableBlockPos mutablePos) {
        if (!isAirOrLavaOcean(world, seaLevel, mutablePos)) {
            return false;
        } else {
            BlockState blockState = world.getBlockState(mutablePos.move(Direction.DOWN));
            mutablePos.move(Direction.UP);
            return !blockState.isAir() && !CANNOT_PLACE_ON.contains(blockState.getBlock());
        }
    }

    @Nullable
    private static BlockPos findAir(LevelAccessor world, BlockPos.MutableBlockPos mutablePos, int distance) {
        while(mutablePos.getY() < world.getMaxBuildHeight() && distance > 0) {
            --distance;
            BlockState blockState = world.getBlockState(mutablePos);
            if (CANNOT_PLACE_ON.contains(blockState.getBlock())) {
                return null;
            }

            if (blockState.isAir()) {
                return mutablePos;
            }

            mutablePos.move(Direction.UP);
        }

        return null;
    }

    private static boolean isAirOrLavaOcean(LevelAccessor world, int seaLevel, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.isAir() || blockState.is(Blocks.LAVA) && pos.getY() <= seaLevel;
    }
}
