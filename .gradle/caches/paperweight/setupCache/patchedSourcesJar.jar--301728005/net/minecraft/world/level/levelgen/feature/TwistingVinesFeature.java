package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TwistingVinesConfig;

public class TwistingVinesFeature extends Feature<TwistingVinesConfig> {
    public TwistingVinesFeature(Codec<TwistingVinesConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<TwistingVinesConfig> context) {
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        if (isInvalidPlacementLocation(worldGenLevel, blockPos)) {
            return false;
        } else {
            Random random = context.random();
            TwistingVinesConfig twistingVinesConfig = context.config();
            int i = twistingVinesConfig.spreadWidth();
            int j = twistingVinesConfig.spreadHeight();
            int k = twistingVinesConfig.maxHeight();
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for(int l = 0; l < i * i; ++l) {
                mutableBlockPos.set(blockPos).move(Mth.nextInt(random, -i, i), Mth.nextInt(random, -j, j), Mth.nextInt(random, -i, i));
                if (findFirstAirBlockAboveGround(worldGenLevel, mutableBlockPos) && !isInvalidPlacementLocation(worldGenLevel, mutableBlockPos)) {
                    int m = Mth.nextInt(random, 1, k);
                    if (random.nextInt(6) == 0) {
                        m *= 2;
                    }

                    if (random.nextInt(5) == 0) {
                        m = 1;
                    }

                    int n = 17;
                    int o = 25;
                    placeWeepingVinesColumn(worldGenLevel, random, mutableBlockPos, m, 17, 25);
                }
            }

            return true;
        }
    }

    private static boolean findFirstAirBlockAboveGround(LevelAccessor world, BlockPos.MutableBlockPos pos) {
        do {
            pos.move(0, -1, 0);
            if (world.isOutsideBuildHeight(pos)) {
                return false;
            }
        } while(world.getBlockState(pos).isAir());

        pos.move(0, 1, 0);
        return true;
    }

    public static void placeWeepingVinesColumn(LevelAccessor world, Random random, BlockPos.MutableBlockPos pos, int maxLength, int minAge, int maxAge) {
        for(int i = 1; i <= maxLength; ++i) {
            if (world.isEmptyBlock(pos)) {
                if (i == maxLength || !world.isEmptyBlock(pos.above())) {
                    world.setBlock(pos, Blocks.TWISTING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Integer.valueOf(Mth.nextInt(random, minAge, maxAge))), 2);
                    break;
                }

                world.setBlock(pos, Blocks.TWISTING_VINES_PLANT.defaultBlockState(), 2);
            }

            pos.move(Direction.UP);
        }

    }

    private static boolean isInvalidPlacementLocation(LevelAccessor world, BlockPos pos) {
        if (!world.isEmptyBlock(pos)) {
            return true;
        } else {
            BlockState blockState = world.getBlockState(pos.below());
            return !blockState.is(Blocks.NETHERRACK) && !blockState.is(Blocks.WARPED_NYLIUM) && !blockState.is(Blocks.WARPED_WART_BLOCK);
        }
    }
}
