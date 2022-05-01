package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;

public class CheckerboardColumnBiomeSource extends BiomeSource {
    public static final Codec<CheckerboardColumnBiomeSource> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Biome.LIST_CODEC.fieldOf("biomes").forGetter((checkerboardColumnBiomeSource) -> {
            return checkerboardColumnBiomeSource.allowedBiomes;
        }), Codec.intRange(0, 62).fieldOf("scale").orElse(2).forGetter((checkerboardColumnBiomeSource) -> {
            return checkerboardColumnBiomeSource.size;
        })).apply(instance, CheckerboardColumnBiomeSource::new);
    });
    private final HolderSet<Biome> allowedBiomes;
    private final int bitShift;
    private final int size;

    public CheckerboardColumnBiomeSource(HolderSet<Biome> holderSet, int size) {
        super(holderSet.stream());
        this.allowedBiomes = holderSet;
        this.bitShift = size + 2;
        this.size = size;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public BiomeSource withSeed(long seed) {
        return this;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        return this.allowedBiomes.get(Math.floorMod((x >> this.bitShift) + (z >> this.bitShift), this.allowedBiomes.size()));
    }
}
