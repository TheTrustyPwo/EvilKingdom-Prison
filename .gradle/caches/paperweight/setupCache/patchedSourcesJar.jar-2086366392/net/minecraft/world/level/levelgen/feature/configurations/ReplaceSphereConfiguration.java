package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;

public class ReplaceSphereConfiguration implements FeatureConfiguration {
    public static final Codec<ReplaceSphereConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockState.CODEC.fieldOf("target").forGetter((config) -> {
            return config.targetState;
        }), BlockState.CODEC.fieldOf("state").forGetter((config) -> {
            return config.replaceState;
        }), IntProvider.codec(0, 12).fieldOf("radius").forGetter((config) -> {
            return config.radius;
        })).apply(instance, ReplaceSphereConfiguration::new);
    });
    public final BlockState targetState;
    public final BlockState replaceState;
    private final IntProvider radius;

    public ReplaceSphereConfiguration(BlockState target, BlockState state, IntProvider radius) {
        this.targetState = target;
        this.replaceState = state;
        this.radius = radius;
    }

    public IntProvider radius() {
        return this.radius;
    }
}
