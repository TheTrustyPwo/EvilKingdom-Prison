package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.GlowLichenConfiguration;

public class GlowLichenFeature extends Feature<GlowLichenConfiguration> {
    public GlowLichenFeature(Codec<GlowLichenConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GlowLichenConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        Random random = context.random();
        GlowLichenConfiguration glowLichenConfiguration = context.config();
        if (!isAirOrWater(worldGenLevel.getBlockState(blockPos))) {
            return false;
        } else {
            List<Direction> list = getShuffledDirections(glowLichenConfiguration, random);
            if (placeGlowLichenIfPossible(worldGenLevel, blockPos, worldGenLevel.getBlockState(blockPos), glowLichenConfiguration, random, list)) {
                return true;
            } else {
                BlockPos.MutableBlockPos mutableBlockPos = blockPos.mutable();

                for(Direction direction : list) {
                    mutableBlockPos.set(blockPos);
                    List<Direction> list2 = getShuffledDirectionsExcept(glowLichenConfiguration, random, direction.getOpposite());

                    for(int i = 0; i < glowLichenConfiguration.searchRange; ++i) {
                        mutableBlockPos.setWithOffset(blockPos, direction);
                        BlockState blockState = worldGenLevel.getBlockState(mutableBlockPos);
                        if (!isAirOrWater(blockState) && !blockState.is(Blocks.GLOW_LICHEN)) {
                            break;
                        }

                        if (placeGlowLichenIfPossible(worldGenLevel, mutableBlockPos, blockState, glowLichenConfiguration, random, list2)) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }

    public static boolean placeGlowLichenIfPossible(WorldGenLevel world, BlockPos pos, BlockState state, GlowLichenConfiguration config, Random random, List<Direction> directions) {
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

        for(Direction direction : directions) {
            BlockState blockState = world.getBlockState(mutableBlockPos.setWithOffset(pos, direction));
            if (blockState.is(config.canBePlacedOn)) {
                GlowLichenBlock glowLichenBlock = (GlowLichenBlock)Blocks.GLOW_LICHEN;
                BlockState blockState2 = glowLichenBlock.getStateForPlacement(state, world, pos, direction);
                if (blockState2 == null) {
                    return false;
                }

                world.setBlock(pos, blockState2, 3);
                world.getChunk(pos).markPosForPostprocessing(pos);
                if (random.nextFloat() < config.chanceOfSpreading) {
                    glowLichenBlock.spreadFromFaceTowardRandomDirection(blockState2, world, pos, direction, random, true);
                }

                return true;
            }
        }

        return false;
    }

    public static List<Direction> getShuffledDirections(GlowLichenConfiguration config, Random random) {
        List<Direction> list = Lists.newArrayList(config.validDirections);
        Collections.shuffle(list, random);
        return list;
    }

    public static List<Direction> getShuffledDirectionsExcept(GlowLichenConfiguration config, Random random, Direction excluded) {
        List<Direction> list = config.validDirections.stream().filter((direction) -> {
            return direction != excluded;
        }).collect(Collectors.toList());
        Collections.shuffle(list, random);
        return list;
    }

    private static boolean isAirOrWater(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }
}
