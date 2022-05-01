package net.minecraft.world.level.levelgen.feature;

import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class FeaturePlaceContext<FC extends FeatureConfiguration> {
    private final Optional<ConfiguredFeature<?, ?>> topFeature;
    private final WorldGenLevel level;
    private final ChunkGenerator chunkGenerator;
    private final Random random;
    private final BlockPos origin;
    private final FC config;

    public FeaturePlaceContext(Optional<ConfiguredFeature<?, ?>> feature, WorldGenLevel world, ChunkGenerator generator, Random random, BlockPos origin, FC config) {
        this.topFeature = feature;
        this.level = world;
        this.chunkGenerator = generator;
        this.random = random;
        this.origin = origin;
        this.config = config;
    }

    public Optional<ConfiguredFeature<?, ?>> topFeature() {
        return this.topFeature;
    }

    public WorldGenLevel level() {
        return this.level;
    }

    public ChunkGenerator chunkGenerator() {
        return this.chunkGenerator;
    }

    public Random random() {
        return this.random;
    }

    public BlockPos origin() {
        return this.origin;
    }

    public FC config() {
        return this.config;
    }
}
