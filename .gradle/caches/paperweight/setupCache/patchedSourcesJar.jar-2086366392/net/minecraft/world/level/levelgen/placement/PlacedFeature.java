package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.apache.commons.lang3.mutable.MutableBoolean;

public record PlacedFeature(Holder<ConfiguredFeature<?, ?>> feature, List<PlacementModifier> placement) {
    public static final Codec<PlacedFeature> DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(ConfiguredFeature.CODEC.fieldOf("feature").forGetter((placedFeature) -> {
            return placedFeature.feature;
        }), PlacementModifier.CODEC.listOf().fieldOf("placement").forGetter((placedFeature) -> {
            return placedFeature.placement;
        })).apply(instance, PlacedFeature::new);
    });
    public static final Codec<Holder<PlacedFeature>> CODEC = RegistryFileCodec.create(Registry.PLACED_FEATURE_REGISTRY, DIRECT_CODEC);
    public static final Codec<HolderSet<PlacedFeature>> LIST_CODEC = RegistryCodecs.homogeneousList(Registry.PLACED_FEATURE_REGISTRY, DIRECT_CODEC);
    public static final Codec<List<HolderSet<PlacedFeature>>> LIST_OF_LISTS_CODEC = RegistryCodecs.homogeneousList(Registry.PLACED_FEATURE_REGISTRY, DIRECT_CODEC, true).listOf();

    public boolean place(WorldGenLevel world, ChunkGenerator generator, Random random, BlockPos pos) {
        return this.placeWithContext(new PlacementContext(world, generator, Optional.empty()), random, pos);
    }

    public boolean placeWithBiomeCheck(WorldGenLevel world, ChunkGenerator generator, Random random, BlockPos pos) {
        return this.placeWithContext(new PlacementContext(world, generator, Optional.of(this)), random, pos);
    }

    private boolean placeWithContext(PlacementContext context, Random random, BlockPos pos) {
        Stream<BlockPos> stream = Stream.of(pos);

        for(PlacementModifier placementModifier : this.placement) {
            stream = stream.flatMap((posx) -> {
                return placementModifier.getPositions(context, random, posx);
            });
        }

        ConfiguredFeature<?, ?> configuredFeature = this.feature.value();
        MutableBoolean mutableBoolean = new MutableBoolean();
        stream.forEach((blockPos) -> {
            if (configuredFeature.place(context.getLevel(), context.generator(), random, blockPos)) {
                mutableBoolean.setTrue();
            }

        });
        return mutableBoolean.isTrue();
    }

    public Stream<ConfiguredFeature<?, ?>> getFeatures() {
        return this.feature.value().getFeatures();
    }

    @Override
    public String toString() {
        return "Placed " + this.feature;
    }

    static record test(int a) {
    }
}
