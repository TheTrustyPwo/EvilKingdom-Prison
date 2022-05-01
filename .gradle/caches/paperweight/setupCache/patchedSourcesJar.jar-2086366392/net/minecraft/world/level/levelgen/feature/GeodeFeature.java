package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.FluidState;

public class GeodeFeature extends Feature<GeodeConfiguration> {
    private static final Direction[] DIRECTIONS = Direction.values();

    public GeodeFeature(Codec<GeodeConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GeodeConfiguration> context) {
        GeodeConfiguration geodeConfiguration = context.config();
        Random random = context.random();
        BlockPos blockPos = context.origin();
        WorldGenLevel worldGenLevel = context.level();
        int i = geodeConfiguration.minGenOffset;
        int j = geodeConfiguration.maxGenOffset;
        List<Pair<BlockPos, Integer>> list = Lists.newLinkedList();
        int k = geodeConfiguration.distributionPoints.sample(random);
        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(worldGenLevel.getSeed()));
        NormalNoise normalNoise = NormalNoise.create(worldgenRandom, -4, 1.0D);
        List<BlockPos> list2 = Lists.newLinkedList();
        double d = (double)k / (double)geodeConfiguration.outerWallDistance.getMaxValue();
        GeodeLayerSettings geodeLayerSettings = geodeConfiguration.geodeLayerSettings;
        GeodeBlockSettings geodeBlockSettings = geodeConfiguration.geodeBlockSettings;
        GeodeCrackSettings geodeCrackSettings = geodeConfiguration.geodeCrackSettings;
        double e = 1.0D / Math.sqrt(geodeLayerSettings.filling);
        double f = 1.0D / Math.sqrt(geodeLayerSettings.innerLayer + d);
        double g = 1.0D / Math.sqrt(geodeLayerSettings.middleLayer + d);
        double h = 1.0D / Math.sqrt(geodeLayerSettings.outerLayer + d);
        double l = 1.0D / Math.sqrt(geodeCrackSettings.baseCrackSize + random.nextDouble() / 2.0D + (k > 3 ? d : 0.0D));
        boolean bl = (double)random.nextFloat() < geodeCrackSettings.generateCrackChance;
        int m = 0;

        for(int n = 0; n < k; ++n) {
            int o = geodeConfiguration.outerWallDistance.sample(random);
            int p = geodeConfiguration.outerWallDistance.sample(random);
            int q = geodeConfiguration.outerWallDistance.sample(random);
            BlockPos blockPos2 = blockPos.offset(o, p, q);
            BlockState blockState = worldGenLevel.getBlockState(blockPos2);
            if (blockState.isAir() || blockState.is(BlockTags.GEODE_INVALID_BLOCKS)) {
                ++m;
                if (m > geodeConfiguration.invalidBlocksThreshold) {
                    return false;
                }
            }

            list.add(Pair.of(blockPos2, geodeConfiguration.pointOffset.sample(random)));
        }

        if (bl) {
            int r = random.nextInt(4);
            int s = k * 2 + 1;
            if (r == 0) {
                list2.add(blockPos.offset(s, 7, 0));
                list2.add(blockPos.offset(s, 5, 0));
                list2.add(blockPos.offset(s, 1, 0));
            } else if (r == 1) {
                list2.add(blockPos.offset(0, 7, s));
                list2.add(blockPos.offset(0, 5, s));
                list2.add(blockPos.offset(0, 1, s));
            } else if (r == 2) {
                list2.add(blockPos.offset(s, 7, s));
                list2.add(blockPos.offset(s, 5, s));
                list2.add(blockPos.offset(s, 1, s));
            } else {
                list2.add(blockPos.offset(0, 7, 0));
                list2.add(blockPos.offset(0, 5, 0));
                list2.add(blockPos.offset(0, 1, 0));
            }
        }

        List<BlockPos> list3 = Lists.newArrayList();
        Predicate<BlockState> predicate = isReplaceable(geodeConfiguration.geodeBlockSettings.cannotReplace);

        for(BlockPos blockPos3 : BlockPos.betweenClosed(blockPos.offset(i, i, i), blockPos.offset(j, j, j))) {
            double t = normalNoise.getValue((double)blockPos3.getX(), (double)blockPos3.getY(), (double)blockPos3.getZ()) * geodeConfiguration.noiseMultiplier;
            double u = 0.0D;
            double v = 0.0D;

            for(Pair<BlockPos, Integer> pair : list) {
                u += Mth.fastInvSqrt(blockPos3.distSqr(pair.getFirst()) + (double)pair.getSecond().intValue()) + t;
            }

            for(BlockPos blockPos4 : list2) {
                v += Mth.fastInvSqrt(blockPos3.distSqr(blockPos4) + (double)geodeCrackSettings.crackPointOffset) + t;
            }

            if (!(u < h)) {
                if (bl && v >= l && u < e) {
                    this.safeSetBlock(worldGenLevel, blockPos3, Blocks.AIR.defaultBlockState(), predicate);

                    for(Direction direction : DIRECTIONS) {
                        BlockPos blockPos5 = blockPos3.relative(direction);
                        FluidState fluidState = worldGenLevel.getFluidState(blockPos5);
                        if (!fluidState.isEmpty()) {
                            worldGenLevel.scheduleTick(blockPos5, fluidState.getType(), 0);
                        }
                    }
                } else if (u >= e) {
                    this.safeSetBlock(worldGenLevel, blockPos3, geodeBlockSettings.fillingProvider.getState(random, blockPos3), predicate);
                } else if (u >= f) {
                    boolean bl2 = (double)random.nextFloat() < geodeConfiguration.useAlternateLayer0Chance;
                    if (bl2) {
                        this.safeSetBlock(worldGenLevel, blockPos3, geodeBlockSettings.alternateInnerLayerProvider.getState(random, blockPos3), predicate);
                    } else {
                        this.safeSetBlock(worldGenLevel, blockPos3, geodeBlockSettings.innerLayerProvider.getState(random, blockPos3), predicate);
                    }

                    if ((!geodeConfiguration.placementsRequireLayer0Alternate || bl2) && (double)random.nextFloat() < geodeConfiguration.usePotentialPlacementsChance) {
                        list3.add(blockPos3.immutable());
                    }
                } else if (u >= g) {
                    this.safeSetBlock(worldGenLevel, blockPos3, geodeBlockSettings.middleLayerProvider.getState(random, blockPos3), predicate);
                } else if (u >= h) {
                    this.safeSetBlock(worldGenLevel, blockPos3, geodeBlockSettings.outerLayerProvider.getState(random, blockPos3), predicate);
                }
            }
        }

        List<BlockState> list4 = geodeBlockSettings.innerPlacements;

        for(BlockPos blockPos6 : list3) {
            BlockState blockState2 = Util.getRandom(list4, random);

            for(Direction direction2 : DIRECTIONS) {
                if (blockState2.hasProperty(BlockStateProperties.FACING)) {
                    blockState2 = blockState2.setValue(BlockStateProperties.FACING, direction2);
                }

                BlockPos blockPos7 = blockPos6.relative(direction2);
                BlockState blockState3 = worldGenLevel.getBlockState(blockPos7);
                if (blockState2.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    blockState2 = blockState2.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(blockState3.getFluidState().isSource()));
                }

                if (BuddingAmethystBlock.canClusterGrowAtState(blockState3)) {
                    this.safeSetBlock(worldGenLevel, blockPos7, blockState2, predicate);
                    break;
                }
            }
        }

        return true;
    }
}
