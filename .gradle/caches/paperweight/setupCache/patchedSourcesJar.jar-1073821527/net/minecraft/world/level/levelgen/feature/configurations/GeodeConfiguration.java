package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;

public class GeodeConfiguration implements FeatureConfiguration {
    public static final Codec<Double> CHANCE_RANGE = Codec.doubleRange(0.0D, 1.0D);
    public static final Codec<GeodeConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(GeodeBlockSettings.CODEC.fieldOf("blocks").forGetter((config) -> {
            return config.geodeBlockSettings;
        }), GeodeLayerSettings.CODEC.fieldOf("layers").forGetter((config) -> {
            return config.geodeLayerSettings;
        }), GeodeCrackSettings.CODEC.fieldOf("crack").forGetter((config) -> {
            return config.geodeCrackSettings;
        }), CHANCE_RANGE.fieldOf("use_potential_placements_chance").orElse(0.35D).forGetter((config) -> {
            return config.usePotentialPlacementsChance;
        }), CHANCE_RANGE.fieldOf("use_alternate_layer0_chance").orElse(0.0D).forGetter((config) -> {
            return config.useAlternateLayer0Chance;
        }), Codec.BOOL.fieldOf("placements_require_layer0_alternate").orElse(true).forGetter((config) -> {
            return config.placementsRequireLayer0Alternate;
        }), IntProvider.codec(1, 20).fieldOf("outer_wall_distance").orElse(UniformInt.of(4, 5)).forGetter((config) -> {
            return config.outerWallDistance;
        }), IntProvider.codec(1, 20).fieldOf("distribution_points").orElse(UniformInt.of(3, 4)).forGetter((config) -> {
            return config.distributionPoints;
        }), IntProvider.codec(0, 10).fieldOf("point_offset").orElse(UniformInt.of(1, 2)).forGetter((config) -> {
            return config.pointOffset;
        }), Codec.INT.fieldOf("min_gen_offset").orElse(-16).forGetter((config) -> {
            return config.minGenOffset;
        }), Codec.INT.fieldOf("max_gen_offset").orElse(16).forGetter((config) -> {
            return config.maxGenOffset;
        }), CHANCE_RANGE.fieldOf("noise_multiplier").orElse(0.05D).forGetter((config) -> {
            return config.noiseMultiplier;
        }), Codec.INT.fieldOf("invalid_blocks_threshold").forGetter((config) -> {
            return config.invalidBlocksThreshold;
        })).apply(instance, GeodeConfiguration::new);
    });
    public final GeodeBlockSettings geodeBlockSettings;
    public final GeodeLayerSettings geodeLayerSettings;
    public final GeodeCrackSettings geodeCrackSettings;
    public final double usePotentialPlacementsChance;
    public final double useAlternateLayer0Chance;
    public final boolean placementsRequireLayer0Alternate;
    public final IntProvider outerWallDistance;
    public final IntProvider distributionPoints;
    public final IntProvider pointOffset;
    public final int minGenOffset;
    public final int maxGenOffset;
    public final double noiseMultiplier;
    public final int invalidBlocksThreshold;

    public GeodeConfiguration(GeodeBlockSettings layerConfig, GeodeLayerSettings layerThicknessConfig, GeodeCrackSettings crackConfig, double usePotentialPlacementsChance, double useAlternateLayer0Chance, boolean placementsRequireLayer0Alternate, IntProvider outerWallDistance, IntProvider distributionPoints, IntProvider pointOffset, int maxDistributionPoints, int minPointOffset, double noiseMultiplier, int maxGenOffset) {
        this.geodeBlockSettings = layerConfig;
        this.geodeLayerSettings = layerThicknessConfig;
        this.geodeCrackSettings = crackConfig;
        this.usePotentialPlacementsChance = usePotentialPlacementsChance;
        this.useAlternateLayer0Chance = useAlternateLayer0Chance;
        this.placementsRequireLayer0Alternate = placementsRequireLayer0Alternate;
        this.outerWallDistance = outerWallDistance;
        this.distributionPoints = distributionPoints;
        this.pointOffset = pointOffset;
        this.minGenOffset = maxDistributionPoints;
        this.maxGenOffset = minPointOffset;
        this.noiseMultiplier = noiseMultiplier;
        this.invalidBlocksThreshold = maxGenOffset;
    }
}
