package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ProbabilityFeatureConfiguration implements FeatureConfiguration {
    public static final Codec<ProbabilityFeatureConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter((probabilityFeatureConfiguration) -> {
            return probabilityFeatureConfiguration.probability;
        })).apply(instance, ProbabilityFeatureConfiguration::new);
    });
    public final float probability;

    public ProbabilityFeatureConfiguration(float probability) {
        this.probability = probability;
    }
}
