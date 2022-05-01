package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class CanyonCarverConfiguration extends CarverConfiguration {
    public static final Codec<CanyonCarverConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(CarverConfiguration.CODEC.forGetter((canyonCarverConfiguration) -> {
            return canyonCarverConfiguration;
        }), FloatProvider.CODEC.fieldOf("vertical_rotation").forGetter((config) -> {
            return config.verticalRotation;
        }), CanyonCarverConfiguration.CanyonShapeConfiguration.CODEC.fieldOf("shape").forGetter((config) -> {
            return config.shape;
        })).apply(instance, CanyonCarverConfiguration::new);
    });
    public final FloatProvider verticalRotation;
    public final CanyonCarverConfiguration.CanyonShapeConfiguration shape;

    public CanyonCarverConfiguration(float probability, HeightProvider y, FloatProvider yScale, VerticalAnchor lavaLevel, CarverDebugSettings debugConfig, FloatProvider verticalRotation, CanyonCarverConfiguration.CanyonShapeConfiguration shape) {
        super(probability, y, yScale, lavaLevel, debugConfig);
        this.verticalRotation = verticalRotation;
        this.shape = shape;
    }

    public CanyonCarverConfiguration(CarverConfiguration config, FloatProvider verticalRotation, CanyonCarverConfiguration.CanyonShapeConfiguration shape) {
        this(config.probability, config.y, config.yScale, config.lavaLevel, config.debugSettings, verticalRotation, shape);
    }

    public static class CanyonShapeConfiguration {
        public static final Codec<CanyonCarverConfiguration.CanyonShapeConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(FloatProvider.CODEC.fieldOf("distance_factor").forGetter((shape) -> {
                return shape.distanceFactor;
            }), FloatProvider.CODEC.fieldOf("thickness").forGetter((shape) -> {
                return shape.thickness;
            }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("width_smoothness").forGetter((shape) -> {
                return shape.widthSmoothness;
            }), FloatProvider.CODEC.fieldOf("horizontal_radius_factor").forGetter((shape) -> {
                return shape.horizontalRadiusFactor;
            }), Codec.FLOAT.fieldOf("vertical_radius_default_factor").forGetter((shape) -> {
                return shape.verticalRadiusDefaultFactor;
            }), Codec.FLOAT.fieldOf("vertical_radius_center_factor").forGetter((shape) -> {
                return shape.verticalRadiusCenterFactor;
            })).apply(instance, CanyonCarverConfiguration.CanyonShapeConfiguration::new);
        });
        public final FloatProvider distanceFactor;
        public final FloatProvider thickness;
        public final int widthSmoothness;
        public final FloatProvider horizontalRadiusFactor;
        public final float verticalRadiusDefaultFactor;
        public final float verticalRadiusCenterFactor;

        public CanyonShapeConfiguration(FloatProvider distanceFactor, FloatProvider thickness, int widthSmoothness, FloatProvider horizontalRadiusFactor, float verticalRadiusDefaultFactor, float verticalRadiusCenterFactor) {
            this.widthSmoothness = widthSmoothness;
            this.horizontalRadiusFactor = horizontalRadiusFactor;
            this.verticalRadiusDefaultFactor = verticalRadiusDefaultFactor;
            this.verticalRadiusCenterFactor = verticalRadiusCenterFactor;
            this.distanceFactor = distanceFactor;
            this.thickness = thickness;
        }
    }
}
