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
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class WeepingVinesFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] DIRECTIONS = Direction.values();

    public WeepingVinesFeature(Codec<NoneFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        Random random = context.random();
        if (!worldGenLevel.isEmptyBlock(blockPos)) {
            return false;
        } else {
            BlockState blockState = worldGenLevel.getBlockState(blockPos.above());
            if (!blockState.is(Blocks.NETHERRACK) && !blockState.is(Blocks.NETHER_WART_BLOCK)) {
                return false;
            } else {
                this.placeRoofNetherWart(worldGenLevel, random, blockPos);
                this.placeRoofWeepingVines(worldGenLevel, random, blockPos);
                return true;
            }
        }
    }

    private void placeRoofNetherWart(LevelAccessor world, Random random, BlockPos pos) {
        world.setBlock(pos, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();

        for(int i = 0; i < 200; ++i) {
            mutableBlockPos.setWithOffset(pos, random.nextInt(6) - random.nextInt(6), random.nextInt(2) - random.nextInt(5), random.nextInt(6) - random.nextInt(6));
            if (world.isEmptyBlock(mutableBlockPos)) {
                int j = 0;

                for(Direction direction : DIRECTIONS) {
                    BlockState blockState = world.getBlockState(mutableBlockPos2.setWithOffset(mutableBlockPos, direction));
                    if (blockState.is(Blocks.NETHERRACK) || blockState.is(Blocks.NETHER_WART_BLOCK)) {
                        ++j;
                    }

                    if (j > 1) {
                        break;
                    }
                }

                if (j == 1) {
                    world.setBlock(mutableBlockPos, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
                }
            }
        }

    }

    private void placeRoofWeepingVines(LevelAccessor world, Random random, BlockPos pos) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(int i = 0; i < 100; ++i) {
            mutableBlockPos.setWithOffset(pos, random.nextInt(8) - random.nextInt(8), random.nextInt(2) - random.nextInt(7), random.nextInt(8) - random.nextInt(8));
            if (world.isEmptyBlock(mutableBlockPos)) {
                BlockState blockState = world.getBlockState(mutableBlockPos.above());
                if (blockState.is(Blocks.NETHERRACK) || blockState.is(Blocks.NETHER_WART_BLOCK)) {
                    int j = Mth.nextInt(random, 1, 8);
                    if (random.nextInt(6) == 0) {
                        j *= 2;
                    }

                    if (random.nextInt(5) == 0) {
                        j = 1;
                    }

                    int k = 17;
                    int l = 25;
                    placeWeepingVinesColumn(world, random, mutableBlockPos, j, 17, 25);
                }
            }
        }

    }

    public static void placeWeepingVinesColumn(LevelAccessor world, Random random, BlockPos.MutableBlockPos pos, int length, int minAge, int maxAge) {
        for(int i = 0; i <= length; ++i) {
            if (world.isEmptyBlock(pos)) {
                if (i == length || !world.isEmptyBlock(pos.below())) {
                    world.setBlock(pos, Blocks.WEEPING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Integer.valueOf(Mth.nextInt(random, minAge, maxAge))), 2);
                    break;
                }

                world.setBlock(pos, Blocks.WEEPING_VINES_PLANT.defaultBlockState(), 2);
            }

            pos.move(Direction.DOWN);
        }

    }
}
