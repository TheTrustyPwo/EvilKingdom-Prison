package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

public class NoiseThresholdCountPlacement extends RepeatingPlacement {
    public static final Codec<NoiseThresholdCountPlacement> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.DOUBLE.fieldOf("noise_level").forGetter((noiseThresholdCountPlacement) -> {
            return noiseThresholdCountPlacement.noiseLevel;
        }), Codec.INT.fieldOf("below_noise").forGetter((noiseThresholdCountPlacement) -> {
            return noiseThresholdCountPlacement.belowNoise;
        }), Codec.INT.fieldOf("above_noise").forGetter((noiseThresholdCountPlacement) -> {
            return noiseThresholdCountPlacement.aboveNoise;
        })).apply(instance, NoiseThresholdCountPlacement::new);
    });
    private final double noiseLevel;
    private final int belowNoise;
    private final int aboveNoise;

    private NoiseThresholdCountPlacement(double noiseLevel, int belowNoise, int aboveNoise) {
        this.noiseLevel = noiseLevel;
        this.belowNoise = belowNoise;
        this.aboveNoise = aboveNoise;
    }

    public static NoiseThresholdCountPlacement of(double noiseLevel, int belowNoise, int aboveNoise) {
        return new NoiseThresholdCountPlacement(noiseLevel, belowNoise, aboveNoise);
    }

    @Override
    protected int count(Random random, BlockPos pos) {
        double d = Biome.BIOME_INFO_NOISE.getValue((double)pos.getX() / 200.0D, (double)pos.getZ() / 200.0D, false);
        return d < this.noiseLevel ? this.belowNoise : this.aboveNoise;
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.NOISE_THRESHOLD_COUNT;
    }
}
