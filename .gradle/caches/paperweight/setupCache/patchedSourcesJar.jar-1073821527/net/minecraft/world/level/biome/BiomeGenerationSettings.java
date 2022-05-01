package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;

public class BiomeGenerationSettings {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BiomeGenerationSettings EMPTY = new BiomeGenerationSettings(ImmutableMap.of(), ImmutableList.of());
    public static final MapCodec<BiomeGenerationSettings> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(Codec.simpleMap(GenerationStep.Carving.CODEC, ConfiguredWorldCarver.LIST_CODEC.promotePartial(Util.prefix("Carver: ", LOGGER::error)), StringRepresentable.keys(GenerationStep.Carving.values())).fieldOf("carvers").forGetter((biomeGenerationSettings) -> {
            return biomeGenerationSettings.carvers;
        }), PlacedFeature.LIST_OF_LISTS_CODEC.promotePartial(Util.prefix("Features: ", LOGGER::error)).fieldOf("features").forGetter((biomeGenerationSettings) -> {
            return biomeGenerationSettings.features;
        })).apply(instance, BiomeGenerationSettings::new);
    });
    private final Map<GenerationStep.Carving, HolderSet<ConfiguredWorldCarver<?>>> carvers;
    private final List<HolderSet<PlacedFeature>> features;
    private final Supplier<List<ConfiguredFeature<?, ?>>> flowerFeatures;
    private final Supplier<Set<PlacedFeature>> featureSet;

    BiomeGenerationSettings(Map<GenerationStep.Carving, HolderSet<ConfiguredWorldCarver<?>>> carvers, List<HolderSet<PlacedFeature>> features) {
        this.carvers = carvers;
        this.features = features;
        this.flowerFeatures = Suppliers.memoize(() -> {
            return features.stream().flatMap(HolderSet::stream).map(Holder::value).flatMap(PlacedFeature::getFeatures).filter((configuredFeature) -> {
                return configuredFeature.feature() == Feature.FLOWER;
            }).collect(ImmutableList.toImmutableList());
        });
        this.featureSet = Suppliers.memoize(() -> {
            return features.stream().flatMap(HolderSet::stream).map(Holder::value).collect(Collectors.toSet());
        });
    }

    public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers(GenerationStep.Carving carverStep) {
        return Objects.requireNonNullElseGet(this.carvers.get(carverStep), List::of);
    }

    public List<ConfiguredFeature<?, ?>> getFlowerFeatures() {
        return this.flowerFeatures.get();
    }

    public List<HolderSet<PlacedFeature>> features() {
        return this.features;
    }

    public boolean hasFeature(PlacedFeature feature) {
        return this.featureSet.get().contains(feature);
    }

    public static class Builder {
        private final Map<GenerationStep.Carving, List<Holder<ConfiguredWorldCarver<?>>>> carvers = Maps.newLinkedHashMap();
        private final List<List<Holder<PlacedFeature>>> features = Lists.newArrayList();

        public BiomeGenerationSettings.Builder addFeature(GenerationStep.Decoration featureStep, Holder<PlacedFeature> feature) {
            return this.addFeature(featureStep.ordinal(), feature);
        }

        public BiomeGenerationSettings.Builder addFeature(int stepIndex, Holder<PlacedFeature> featureEntry) {
            this.addFeatureStepsUpTo(stepIndex);
            this.features.get(stepIndex).add(featureEntry);
            return this;
        }

        public BiomeGenerationSettings.Builder addCarver(GenerationStep.Carving carverStep, Holder<? extends ConfiguredWorldCarver<?>> carver) {
            this.carvers.computeIfAbsent(carverStep, (carving) -> {
                return Lists.newArrayList();
            }).add(Holder.hackyErase(carver));
            return this;
        }

        private void addFeatureStepsUpTo(int stepIndex) {
            while(this.features.size() <= stepIndex) {
                this.features.add(Lists.newArrayList());
            }

        }

        public BiomeGenerationSettings build() {
            return new BiomeGenerationSettings(this.carvers.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (entry) -> {
                return HolderSet.direct(entry.getValue());
            })), this.features.stream().map(HolderSet::direct).collect(ImmutableList.toImmutableList()));
        }
    }
}
