package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;

public class BlockColumnFeature extends Feature<BlockColumnConfiguration> {
    public BlockColumnFeature(Codec<BlockColumnConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockColumnConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        BlockColumnConfiguration blockColumnConfiguration = context.config();
        Random random = context.random();
        int i = blockColumnConfiguration.layers().size();
        int[] is = new int[i];
        int j = 0;

        for(int k = 0; k < i; ++k) {
            is[k] = blockColumnConfiguration.layers().get(k).height().sample(random);
            j += is[k];
        }

        if (j == 0) {
            return false;
        } else {
            BlockPos.MutableBlockPos mutableBlockPos = context.origin().mutable();
            BlockPos.MutableBlockPos mutableBlockPos2 = mutableBlockPos.mutable().move(blockColumnConfiguration.direction());

            for(int l = 0; l < j; ++l) {
                if (!blockColumnConfiguration.allowedPlacement().test(worldGenLevel, mutableBlockPos2)) {
                    truncate(is, j, l, blockColumnConfiguration.prioritizeTip());
                    break;
                }

                mutableBlockPos2.move(blockColumnConfiguration.direction());
            }

            for(int m = 0; m < i; ++m) {
                int n = is[m];
                if (n != 0) {
                    BlockColumnConfiguration.Layer layer = blockColumnConfiguration.layers().get(m);

                    for(int o = 0; o < n; ++o) {
                        worldGenLevel.setBlock(mutableBlockPos, layer.state().getState(random, mutableBlockPos), 2);
                        mutableBlockPos.move(blockColumnConfiguration.direction());
                    }
                }
            }

            return true;
        }
    }

    private static void truncate(int[] is, int i, int j, boolean bl) {
        int k = i - j;
        int l = bl ? 1 : -1;
        int m = bl ? 0 : is.length - 1;
        int n = bl ? is.length : -1;

        for(int o = m; o != n && k > 0; o += l) {
            int p = is[o];
            int q = Math.min(p, k);
            k -= q;
            is[o] -= q;
        }

    }
}
