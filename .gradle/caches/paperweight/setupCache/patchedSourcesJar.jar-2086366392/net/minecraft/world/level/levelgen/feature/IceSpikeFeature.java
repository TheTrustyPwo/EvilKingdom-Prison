package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class IceSpikeFeature extends Feature<NoneFeatureConfiguration> {
    public IceSpikeFeature(Codec<NoneFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos blockPos = context.origin();
        Random random = context.random();

        WorldGenLevel worldGenLevel;
        for(worldGenLevel = context.level(); worldGenLevel.isEmptyBlock(blockPos) && blockPos.getY() > worldGenLevel.getMinBuildHeight() + 2; blockPos = blockPos.below()) {
        }

        if (!worldGenLevel.getBlockState(blockPos).is(Blocks.SNOW_BLOCK)) {
            return false;
        } else {
            blockPos = blockPos.above(random.nextInt(4));
            int i = random.nextInt(4) + 7;
            int j = i / 4 + random.nextInt(2);
            if (j > 1 && random.nextInt(60) == 0) {
                blockPos = blockPos.above(10 + random.nextInt(30));
            }

            for(int k = 0; k < i; ++k) {
                float f = (1.0F - (float)k / (float)i) * (float)j;
                int l = Mth.ceil(f);

                for(int m = -l; m <= l; ++m) {
                    float g = (float)Mth.abs(m) - 0.25F;

                    for(int n = -l; n <= l; ++n) {
                        float h = (float)Mth.abs(n) - 0.25F;
                        if ((m == 0 && n == 0 || !(g * g + h * h > f * f)) && (m != -l && m != l && n != -l && n != l || !(random.nextFloat() > 0.75F))) {
                            BlockState blockState = worldGenLevel.getBlockState(blockPos.offset(m, k, n));
                            if (blockState.isAir() || isDirt(blockState) || blockState.is(Blocks.SNOW_BLOCK) || blockState.is(Blocks.ICE)) {
                                this.setBlock(worldGenLevel, blockPos.offset(m, k, n), Blocks.PACKED_ICE.defaultBlockState());
                            }

                            if (k != 0 && l > 1) {
                                blockState = worldGenLevel.getBlockState(blockPos.offset(m, -k, n));
                                if (blockState.isAir() || isDirt(blockState) || blockState.is(Blocks.SNOW_BLOCK) || blockState.is(Blocks.ICE)) {
                                    this.setBlock(worldGenLevel, blockPos.offset(m, -k, n), Blocks.PACKED_ICE.defaultBlockState());
                                }
                            }
                        }
                    }
                }
            }

            int o = j - 1;
            if (o < 0) {
                o = 0;
            } else if (o > 1) {
                o = 1;
            }

            for(int p = -o; p <= o; ++p) {
                for(int q = -o; q <= o; ++q) {
                    BlockPos blockPos2 = blockPos.offset(p, -1, q);
                    int r = 50;
                    if (Math.abs(p) == 1 && Math.abs(q) == 1) {
                        r = random.nextInt(5);
                    }

                    while(blockPos2.getY() > 50) {
                        BlockState blockState2 = worldGenLevel.getBlockState(blockPos2);
                        if (!blockState2.isAir() && !isDirt(blockState2) && !blockState2.is(Blocks.SNOW_BLOCK) && !blockState2.is(Blocks.ICE) && !blockState2.is(Blocks.PACKED_ICE)) {
                            break;
                        }

                        this.setBlock(worldGenLevel, blockPos2, Blocks.PACKED_ICE.defaultBlockState());
                        blockPos2 = blockPos2.below();
                        --r;
                        if (r <= 0) {
                            blockPos2 = blockPos2.below(random.nextInt(5) + 1);
                            r = random.nextInt(5);
                        }
                    }
                }
            }

            return true;
        }
    }
}
