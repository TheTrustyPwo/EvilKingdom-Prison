package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public abstract class CoralFeature extends Feature<NoneFeatureConfiguration> {
    public CoralFeature(Codec<NoneFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        Random random = context.random();
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        Optional<Block> optional = Registry.BLOCK.getTag(BlockTags.CORAL_BLOCKS).flatMap((blocks) -> {
            return blocks.getRandomElement(random);
        }).map(Holder::value);
        return optional.isEmpty() ? false : this.placeFeature(worldGenLevel, random, blockPos, optional.get().defaultBlockState());
    }

    protected abstract boolean placeFeature(LevelAccessor world, Random random, BlockPos pos, BlockState state);

    protected boolean placeCoralBlock(LevelAccessor world, Random random, BlockPos pos, BlockState state) {
        BlockPos blockPos = pos.above();
        BlockState blockState = world.getBlockState(pos);
        if ((blockState.is(Blocks.WATER) || blockState.is(BlockTags.CORALS)) && world.getBlockState(blockPos).is(Blocks.WATER)) {
            world.setBlock(pos, state, 3);
            if (random.nextFloat() < 0.25F) {
                Registry.BLOCK.getTag(BlockTags.CORALS).flatMap((blocks) -> {
                    return blocks.getRandomElement(random);
                }).map(Holder::value).ifPresent((block) -> {
                    world.setBlock(blockPos, block.defaultBlockState(), 2);
                });
            } else if (random.nextFloat() < 0.05F) {
                world.setBlock(blockPos, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, Integer.valueOf(random.nextInt(4) + 1)), 2);
            }

            for(Direction direction : Direction.Plane.HORIZONTAL) {
                if (random.nextFloat() < 0.2F) {
                    BlockPos blockPos2 = pos.relative(direction);
                    if (world.getBlockState(blockPos2).is(Blocks.WATER)) {
                        Registry.BLOCK.getTag(BlockTags.WALL_CORALS).flatMap((blocks) -> {
                            return blocks.getRandomElement(random);
                        }).map(Holder::value).ifPresent((block) -> {
                            BlockState blockState = block.defaultBlockState();
                            if (blockState.hasProperty(BaseCoralWallFanBlock.FACING)) {
                                blockState = blockState.setValue(BaseCoralWallFanBlock.FACING, direction);
                            }

                            world.setBlock(blockPos2, blockState, 2);
                        });
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
