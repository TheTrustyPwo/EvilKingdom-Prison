package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.MineshaftFeature;

public class MineshaftConfiguration implements FeatureConfiguration {
    public static final Codec<MineshaftConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter((config) -> {
            return config.probability;
        }), MineshaftFeature.Type.CODEC.fieldOf("type").forGetter((config) -> {
            return config.type;
        })).apply(instance, MineshaftConfiguration::new);
    });
    public final float probability;
    public final MineshaftFeature.Type type;

    public MineshaftConfiguration(float probability, MineshaftFeature.Type type) {
        this.probability = probability;
        this.type = type;
    }
}
