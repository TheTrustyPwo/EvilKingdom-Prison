package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class GeodeLayerSettings {
    private static final Codec<Double> LAYER_RANGE = Codec.doubleRange(0.01D, 50.0D);
    public static final Codec<GeodeLayerSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(LAYER_RANGE.fieldOf("filling").orElse(1.7D).forGetter((config) -> {
            return config.filling;
        }), LAYER_RANGE.fieldOf("inner_layer").orElse(2.2D).forGetter((config) -> {
            return config.innerLayer;
        }), LAYER_RANGE.fieldOf("middle_layer").orElse(3.2D).forGetter((config) -> {
            return config.middleLayer;
        }), LAYER_RANGE.fieldOf("outer_layer").orElse(4.2D).forGetter((config) -> {
            return config.outerLayer;
        })).apply(instance, GeodeLayerSettings::new);
    });
    public final double filling;
    public final double innerLayer;
    public final double middleLayer;
    public final double outerLayer;

    public GeodeLayerSettings(double filling, double innerLayer, double middleLayer, double outerLayer) {
        this.filling = filling;
        this.innerLayer = innerLayer;
        this.middleLayer = middleLayer;
        this.outerLayer = outerLayer;
    }
}
