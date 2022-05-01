package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class HugeMushroomFeatureConfiguration implements FeatureConfiguration {
    public static final Codec<HugeMushroomFeatureConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockStateProvider.CODEC.fieldOf("cap_provider").forGetter((hugeMushroomFeatureConfiguration) -> {
            return hugeMushroomFeatureConfiguration.capProvider;
        }), BlockStateProvider.CODEC.fieldOf("stem_provider").forGetter((hugeMushroomFeatureConfiguration) -> {
            return hugeMushroomFeatureConfiguration.stemProvider;
        }), Codec.INT.fieldOf("foliage_radius").orElse(2).forGetter((hugeMushroomFeatureConfiguration) -> {
            return hugeMushroomFeatureConfiguration.foliageRadius;
        })).apply(instance, HugeMushroomFeatureConfiguration::new);
    });
    public final BlockStateProvider capProvider;
    public final BlockStateProvider stemProvider;
    public final int foliageRadius;

    public HugeMushroomFeatureConfiguration(BlockStateProvider capProvider, BlockStateProvider stemProvider, int foliageRadius) {
        this.capProvider = capProvider;
        this.stemProvider = stemProvider;
        this.foliageRadius = foliageRadius;
    }
}
