package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.VisibleForDebug;

public class MultiNoiseBiomeSource extends BiomeSource {
    public static final MapCodec<MultiNoiseBiomeSource> DIRECT_CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(ExtraCodecs.<Pair<Climate.ParameterPoint, T>>nonEmptyList(RecordCodecBuilder.create((instancex) -> {
            return instancex.group(Climate.ParameterPoint.CODEC.fieldOf("parameters").forGetter(Pair::getFirst), Biome.CODEC.fieldOf("biome").forGetter(Pair::getSecond)).apply(instancex, Pair::of);
        }).listOf()).xmap(Climate.ParameterList::new, Climate.ParameterList::values).fieldOf("biomes").forGetter((multiNoiseBiomeSource) -> {
            return multiNoiseBiomeSource.parameters;
        })).apply(instance, MultiNoiseBiomeSource::new);
    });
    public static final Codec<MultiNoiseBiomeSource> CODEC = Codec.mapEither(MultiNoiseBiomeSource.PresetInstance.CODEC, DIRECT_CODEC).xmap((either) -> {
        return either.map(MultiNoiseBiomeSource.PresetInstance::biomeSource, Function.identity());
    }, (multiNoiseBiomeSource) -> {
        return multiNoiseBiomeSource.preset().map(Either::left).orElseGet(() -> {
            return Either.right(multiNoiseBiomeSource);
        });
    }).codec();
    private final Climate.ParameterList<Holder<Biome>> parameters;
    private final Optional<MultiNoiseBiomeSource.PresetInstance> preset;

    private MultiNoiseBiomeSource(Climate.ParameterList<Holder<Biome>> entries) {
        this(entries, Optional.empty());
    }

    MultiNoiseBiomeSource(Climate.ParameterList<Holder<Biome>> biomeEntries, Optional<MultiNoiseBiomeSource.PresetInstance> instance) {
        super(biomeEntries.values().stream().map(Pair::getSecond));
        this.preset = instance;
        this.parameters = biomeEntries;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public BiomeSource withSeed(long seed) {
        return this;
    }

    private Optional<MultiNoiseBiomeSource.PresetInstance> preset() {
        return this.preset;
    }

    public boolean stable(MultiNoiseBiomeSource.Preset instance) {
        return this.preset.isPresent() && Objects.equals(this.preset.get().preset(), instance);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        return this.getNoiseBiome(noise.sample(x, y, z));
    }

    @VisibleForDebug
    public Holder<Biome> getNoiseBiome(Climate.TargetPoint point) {
        return this.parameters.findValue(point);
    }

    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, Climate.Sampler noiseSampler) {
        int i = QuartPos.fromBlock(pos.getX());
        int j = QuartPos.fromBlock(pos.getY());
        int k = QuartPos.fromBlock(pos.getZ());
        Climate.TargetPoint targetPoint = noiseSampler.sample(i, j, k);
        float f = Climate.unquantizeCoord(targetPoint.continentalness());
        float g = Climate.unquantizeCoord(targetPoint.erosion());
        float h = Climate.unquantizeCoord(targetPoint.temperature());
        float l = Climate.unquantizeCoord(targetPoint.humidity());
        float m = Climate.unquantizeCoord(targetPoint.weirdness());
        double d = (double)TerrainShaper.peaksAndValleys(m);
        OverworldBiomeBuilder overworldBiomeBuilder = new OverworldBiomeBuilder();
        info.add("Biome builder PV: " + OverworldBiomeBuilder.getDebugStringForPeaksAndValleys(d) + " C: " + overworldBiomeBuilder.getDebugStringForContinentalness((double)f) + " E: " + overworldBiomeBuilder.getDebugStringForErosion((double)g) + " T: " + overworldBiomeBuilder.getDebugStringForTemperature((double)h) + " H: " + overworldBiomeBuilder.getDebugStringForHumidity((double)l));
    }

    public static class Preset {
        static final Map<ResourceLocation, MultiNoiseBiomeSource.Preset> BY_NAME = Maps.newHashMap();
        public static final MultiNoiseBiomeSource.Preset NETHER = new MultiNoiseBiomeSource.Preset(new ResourceLocation("nether"), (registry) -> {
            return new Climate.ParameterList<>(ImmutableList.of(Pair.of(Climate.parameters(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), registry.getOrCreateHolder(Biomes.NETHER_WASTES)), Pair.of(Climate.parameters(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), registry.getOrCreateHolder(Biomes.SOUL_SAND_VALLEY)), Pair.of(Climate.parameters(0.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), registry.getOrCreateHolder(Biomes.CRIMSON_FOREST)), Pair.of(Climate.parameters(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.375F), registry.getOrCreateHolder(Biomes.WARPED_FOREST)), Pair.of(Climate.parameters(-0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.175F), registry.getOrCreateHolder(Biomes.BASALT_DELTAS))));
        });
        public static final MultiNoiseBiomeSource.Preset OVERWORLD = new MultiNoiseBiomeSource.Preset(new ResourceLocation("overworld"), (registry) -> {
            Builder<Pair<Climate.ParameterPoint, Holder<Biome>>> builder = ImmutableList.builder();
            (new OverworldBiomeBuilder()).addBiomes((pair) -> {
                builder.add(pair.mapSecond(registry::getOrCreateHolder));
            });
            return new Climate.ParameterList<>(builder.build());
        });
        final ResourceLocation name;
        private final Function<Registry<Biome>, Climate.ParameterList<Holder<Biome>>> parameterSource;

        public Preset(ResourceLocation id, Function<Registry<Biome>, Climate.ParameterList<Holder<Biome>>> biomeSourceFunction) {
            this.name = id;
            this.parameterSource = biomeSourceFunction;
            BY_NAME.put(id, this);
        }

        MultiNoiseBiomeSource biomeSource(MultiNoiseBiomeSource.PresetInstance instance, boolean useInstance) {
            Climate.ParameterList<Holder<Biome>> parameterList = this.parameterSource.apply(instance.biomes());
            return new MultiNoiseBiomeSource(parameterList, useInstance ? Optional.of(instance) : Optional.empty());
        }

        public MultiNoiseBiomeSource biomeSource(Registry<Biome> biomeRegistry, boolean useInstance) {
            return this.biomeSource(new MultiNoiseBiomeSource.PresetInstance(this, biomeRegistry), useInstance);
        }

        public MultiNoiseBiomeSource biomeSource(Registry<Biome> biomeRegistry) {
            return this.biomeSource(biomeRegistry, true);
        }
    }

    static record PresetInstance(MultiNoiseBiomeSource.Preset preset, Registry<Biome> biomes) {
        public static final MapCodec<MultiNoiseBiomeSource.PresetInstance> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
            return instance.group(ResourceLocation.CODEC.flatXmap((id) -> {
                return Optional.ofNullable(MultiNoiseBiomeSource.Preset.BY_NAME.get(id)).map(DataResult::success).orElseGet(() -> {
                    return DataResult.error("Unknown preset: " + id);
                });
            }, (preset) -> {
                return DataResult.success(preset.name);
            }).fieldOf("preset").stable().forGetter(MultiNoiseBiomeSource.PresetInstance::preset), RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter(MultiNoiseBiomeSource.PresetInstance::biomes)).apply(instance, instance.stable(MultiNoiseBiomeSource.PresetInstance::new));
        });

        public MultiNoiseBiomeSource biomeSource() {
            return this.preset.biomeSource(this, true);
        }
    }
}
