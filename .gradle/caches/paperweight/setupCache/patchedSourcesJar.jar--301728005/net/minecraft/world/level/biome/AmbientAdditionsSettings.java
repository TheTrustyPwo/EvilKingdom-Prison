package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.sounds.SoundEvent;

public class AmbientAdditionsSettings {
    public static final Codec<AmbientAdditionsSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(SoundEvent.CODEC.fieldOf("sound").forGetter((ambientAdditionsSettings) -> {
            return ambientAdditionsSettings.soundEvent;
        }), Codec.DOUBLE.fieldOf("tick_chance").forGetter((ambientAdditionsSettings) -> {
            return ambientAdditionsSettings.tickChance;
        })).apply(instance, AmbientAdditionsSettings::new);
    });
    private final SoundEvent soundEvent;
    private final double tickChance;

    public AmbientAdditionsSettings(SoundEvent sound, double chance) {
        this.soundEvent = sound;
        this.tickChance = chance;
    }

    public SoundEvent getSoundEvent() {
        return this.soundEvent;
    }

    public double getTickChance() {
        return this.tickChance;
    }
}
