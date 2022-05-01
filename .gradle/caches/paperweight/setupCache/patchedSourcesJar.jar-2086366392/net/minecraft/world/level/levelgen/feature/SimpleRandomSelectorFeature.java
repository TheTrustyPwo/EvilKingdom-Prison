package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SimpleRandomSelectorFeature extends Feature<SimpleRandomFeatureConfiguration> {
    public SimpleRandomSelectorFeature(Codec<SimpleRandomFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SimpleRandomFeatureConfiguration> context) {
        Random random = context.random();
        SimpleRandomFeatureConfiguration simpleRandomFeatureConfiguration = context.config();
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        int i = random.nextInt(simpleRandomFeatureConfiguration.features.size());
        PlacedFeature placedFeature = simpleRandomFeatureConfiguration.features.get(i).value();
        return placedFeature.place(worldGenLevel, chunkGenerator, random, blockPos);
    }
}
