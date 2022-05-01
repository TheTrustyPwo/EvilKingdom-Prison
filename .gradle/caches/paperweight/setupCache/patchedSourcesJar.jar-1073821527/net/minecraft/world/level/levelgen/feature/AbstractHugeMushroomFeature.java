package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public abstract class AbstractHugeMushroomFeature extends Feature<HugeMushroomFeatureConfiguration> {
    public AbstractHugeMushroomFeature(Codec<HugeMushroomFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    protected void placeTrunk(LevelAccessor world, Random random, BlockPos pos, HugeMushroomFeatureConfiguration config, int height, BlockPos.MutableBlockPos mutablePos) {
        for(int i = 0; i < height; ++i) {
            mutablePos.set(pos).move(Direction.UP, i);
            if (!world.getBlockState(mutablePos).isSolidRender(world, mutablePos)) {
                this.setBlock(world, mutablePos, config.stemProvider.getState(random, pos));
            }
        }

    }

    protected int getTreeHeight(Random random) {
        int i = random.nextInt(3) + 4;
        if (random.nextInt(12) == 0) {
            i *= 2;
        }

        return i;
    }

    protected boolean isValidPosition(LevelAccessor world, BlockPos pos, int height, BlockPos.MutableBlockPos mutablePos, HugeMushroomFeatureConfiguration config) {
        int i = pos.getY();
        if (i >= world.getMinBuildHeight() + 1 && i + height + 1 < world.getMaxBuildHeight()) {
            BlockState blockState = world.getBlockState(pos.below());
            if (!isDirt(blockState) && !blockState.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
                return false;
            } else {
                for(int j = 0; j <= height; ++j) {
                    int k = this.getTreeRadiusForHeight(-1, -1, config.foliageRadius, j);

                    for(int l = -k; l <= k; ++l) {
                        for(int m = -k; m <= k; ++m) {
                            BlockState blockState2 = world.getBlockState(mutablePos.setWithOffset(pos, l, j, m));
                            if (!blockState2.isAir() && !blockState2.is(BlockTags.LEAVES)) {
                                return false;
                            }
                        }
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<HugeMushroomFeatureConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        Random random = context.random();
        HugeMushroomFeatureConfiguration hugeMushroomFeatureConfiguration = context.config();
        int i = this.getTreeHeight(random);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        if (!this.isValidPosition(worldGenLevel, blockPos, i, mutableBlockPos, hugeMushroomFeatureConfiguration)) {
            return false;
        } else {
            this.makeCap(worldGenLevel, random, blockPos, i, mutableBlockPos, hugeMushroomFeatureConfiguration);
            this.placeTrunk(worldGenLevel, random, blockPos, hugeMushroomFeatureConfiguration, i, mutableBlockPos);
            return true;
        }
    }

    protected abstract int getTreeRadiusForHeight(int i, int j, int capSize, int y);

    protected abstract void makeCap(LevelAccessor world, Random random, BlockPos start, int y, BlockPos.MutableBlockPos mutable, HugeMushroomFeatureConfiguration config);
}
