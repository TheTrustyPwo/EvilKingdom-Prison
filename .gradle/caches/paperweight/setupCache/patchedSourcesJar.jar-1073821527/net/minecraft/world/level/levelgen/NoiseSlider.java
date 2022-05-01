package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

public record NoiseSlider(double target, int size, int offset) {
    public static final Codec<NoiseSlider> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.DOUBLE.fieldOf("target").forGetter((noiseSlider) -> {
            return noiseSlider.target;
        }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("size").forGetter((noiseSlider) -> {
            return noiseSlider.size;
        }), Codec.INT.fieldOf("offset").forGetter((noiseSlider) -> {
            return noiseSlider.offset;
        })).apply(instance, NoiseSlider::new);
    });

    public double applySlide(double d, double e) {
        if (this.size <= 0) {
            return d;
        } else {
            double f = (e - (double)this.offset) / (double)this.size;
            return Mth.clampedLerp(this.target, d, f);
        }
    }
}
