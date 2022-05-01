package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RandomBooleanFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class RandomBooleanSelectorFeature extends Feature<RandomBooleanFeatureConfiguration> {
    public RandomBooleanSelectorFeature(Codec<RandomBooleanFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RandomBooleanFeatureConfiguration> context) {
        Random random = context.random();
        RandomBooleanFeatureConfiguration randomBooleanFeatureConfiguration = context.config();
        WorldGenLevel worldGenLevel = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        BlockPos blockPos = context.origin();
        boolean bl = random.nextBoolean();
        return ((PlacedFeature)(bl ? randomBooleanFeatureConfiguration.featureTrue : randomBooleanFeatureConfiguration.featureFalse).value()).place(worldGenLevel, chunkGenerator, random, blockPos);
    }
}
