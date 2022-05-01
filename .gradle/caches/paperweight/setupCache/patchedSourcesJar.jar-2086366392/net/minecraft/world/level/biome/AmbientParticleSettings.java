package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

public class AmbientParticleSettings {
    public static final Codec<AmbientParticleSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(ParticleTypes.CODEC.fieldOf("options").forGetter((ambientParticleSettings) -> {
            return ambientParticleSettings.options;
        }), Codec.FLOAT.fieldOf("probability").forGetter((ambientParticleSettings) -> {
            return ambientParticleSettings.probability;
        })).apply(instance, AmbientParticleSettings::new);
    });
    private final ParticleOptions options;
    private final float probability;

    public AmbientParticleSettings(ParticleOptions particle, float probability) {
        this.options = particle;
        this.probability = probability;
    }

    public ParticleOptions getOptions() {
        return this.options;
    }

    public boolean canSpawn(Random random) {
        return random.nextFloat() <= this.probability;
    }
}
