package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class DesertWellFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockStatePredicate IS_SAND = BlockStatePredicate.forBlock(Blocks.SAND);
    private final BlockState sandSlab = Blocks.SANDSTONE_SLAB.defaultBlockState();
    private final BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
    private final BlockState water = Blocks.WATER.defaultBlockState();

    public DesertWellFeature(Codec<NoneFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();

        for(blockPos = blockPos.above(); worldGenLevel.isEmptyBlock(blockPos) && blockPos.getY() > worldGenLevel.getMinBuildHeight() + 2; blockPos = blockPos.below()) {
        }

        if (!IS_SAND.test(worldGenLevel.getBlockState(blockPos))) {
            return false;
        } else {
            for(int i = -2; i <= 2; ++i) {
                for(int j = -2; j <= 2; ++j) {
                    if (worldGenLevel.isEmptyBlock(blockPos.offset(i, -1, j)) && worldGenLevel.isEmptyBlock(blockPos.offset(i, -2, j))) {
                        return false;
                    }
                }
            }

            for(int k = -1; k <= 0; ++k) {
                for(int l = -2; l <= 2; ++l) {
                    for(int m = -2; m <= 2; ++m) {
                        worldGenLevel.setBlock(blockPos.offset(l, k, m), this.sandstone, 2);
                    }
                }
            }

            worldGenLevel.setBlock(blockPos, this.water, 2);

            for(Direction direction : Direction.Plane.HORIZONTAL) {
                worldGenLevel.setBlock(blockPos.relative(direction), this.water, 2);
            }

            for(int n = -2; n <= 2; ++n) {
                for(int o = -2; o <= 2; ++o) {
                    if (n == -2 || n == 2 || o == -2 || o == 2) {
                        worldGenLevel.setBlock(blockPos.offset(n, 1, o), this.sandstone, 2);
                    }
                }
            }

            worldGenLevel.setBlock(blockPos.offset(2, 1, 0), this.sandSlab, 2);
            worldGenLevel.setBlock(blockPos.offset(-2, 1, 0), this.sandSlab, 2);
            worldGenLevel.setBlock(blockPos.offset(0, 1, 2), this.sandSlab, 2);
            worldGenLevel.setBlock(blockPos.offset(0, 1, -2), this.sandSlab, 2);

            for(int p = -1; p <= 1; ++p) {
                for(int q = -1; q <= 1; ++q) {
                    if (p == 0 && q == 0) {
                        worldGenLevel.setBlock(blockPos.offset(p, 4, q), this.sandstone, 2);
                    } else {
                        worldGenLevel.setBlock(blockPos.offset(p, 4, q), this.sandSlab, 2);
                    }
                }
            }

            for(int r = 1; r <= 3; ++r) {
                worldGenLevel.setBlock(blockPos.offset(-1, r, -1), this.sandstone, 2);
                worldGenLevel.setBlock(blockPos.offset(-1, r, 1), this.sandstone, 2);
                worldGenLevel.setBlock(blockPos.offset(1, r, -1), this.sandstone, 2);
                worldGenLevel.setBlock(blockPos.offset(1, r, 1), this.sandstone, 2);
            }

            return true;
        }
    }
}
