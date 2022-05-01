package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

public class LayerConfiguration implements FeatureConfiguration {
    public static final Codec<LayerConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.intRange(0, DimensionType.Y_SIZE).fieldOf("height").forGetter((config) -> {
            return config.height;
        }), BlockState.CODEC.fieldOf("state").forGetter((config) -> {
            return config.state;
        })).apply(instance, LayerConfiguration::new);
    });
    public final int height;
    public final BlockState state;

    public LayerConfiguration(int height, BlockState state) {
        this.height = height;
        this.state = state;
    }
}
