package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;

public class ShipwreckConfiguration implements FeatureConfiguration {
    public static final Codec<ShipwreckConfiguration> CODEC = Codec.BOOL.fieldOf("is_beached").orElse(false).xmap(ShipwreckConfiguration::new, (config) -> {
        return config.isBeached;
    }).codec();
    public final boolean isBeached;

    public ShipwreckConfiguration(boolean isBeached) {
        this.isBeached = isBeached;
    }
}
