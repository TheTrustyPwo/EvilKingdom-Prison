package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralTreeFeature extends CoralFeature {
    public CoralTreeFeature(Codec<NoneFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected boolean placeFeature(LevelAccessor world, Random random, BlockPos pos, BlockState state) {
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
        int i = random.nextInt(3) + 1;

        for(int j = 0; j < i; ++j) {
            if (!this.placeCoralBlock(world, random, mutableBlockPos, state)) {
                return true;
            }

            mutableBlockPos.move(Direction.UP);
        }

        BlockPos blockPos = mutableBlockPos.immutable();
        int k = random.nextInt(3) + 2;
        List<Direction> list = Lists.newArrayList(Direction.Plane.HORIZONTAL);
        Collections.shuffle(list, random);

        for(Direction direction : list.subList(0, k)) {
            mutableBlockPos.set(blockPos);
            mutableBlockPos.move(direction);
            int l = random.nextInt(5) + 2;
            int m = 0;

            for(int n = 0; n < l && this.placeCoralBlock(world, random, mutableBlockPos, state); ++n) {
                ++m;
                mutableBlockPos.move(Direction.UP);
                if (n == 0 || m >= 2 && random.nextFloat() < 0.25F) {
                    mutableBlockPos.move(direction);
                    m = 0;
                }
            }
        }

        return true;
    }
}
