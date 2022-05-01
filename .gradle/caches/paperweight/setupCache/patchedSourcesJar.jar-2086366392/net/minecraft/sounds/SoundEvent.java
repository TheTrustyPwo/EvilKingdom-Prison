package net.minecraft.sounds;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

public class SoundEvent {
    public static final Codec<SoundEvent> CODEC = ResourceLocation.CODEC.xmap(SoundEvent::new, (soundEvent) -> {
        return soundEvent.location;
    });
    private final ResourceLocation location;

    public SoundEvent(ResourceLocation id) {
        this.location = id;
    }

    public ResourceLocation getLocation() {
        return this.location;
    }
}
