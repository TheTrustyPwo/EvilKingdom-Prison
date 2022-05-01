package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class WorldGenSettings {
    public static final Codec<WorldGenSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.LONG.fieldOf("seed").stable().forGetter(WorldGenSettings::seed), Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(WorldGenSettings::generateFeatures), Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(WorldGenSettings::generateBonusChest), RegistryCodecs.dataPackAwareCodec(Registry.LEVEL_STEM_REGISTRY, Lifecycle.stable(), LevelStem.CODEC).xmap(LevelStem::sortMap, Function.identity()).fieldOf("dimensions").forGetter(WorldGenSettings::dimensions), Codec.STRING.optionalFieldOf("legacy_custom_options").stable().forGetter((worldGenSettings) -> {
            return worldGenSettings.legacyCustomOptions;
        })).apply(instance, instance.stable(WorldGenSettings::new));
    }).comapFlatMap(WorldGenSettings::guardExperimental, Function.identity());
    private static final Logger LOGGER = LogUtils.getLogger();
    private final long seed;
    private final boolean generateFeatures;
    private final boolean generateBonusChest;
    private final Registry<LevelStem> dimensions;
    private final Optional<String> legacyCustomOptions;

    private DataResult<WorldGenSettings> guardExperimental() {
        LevelStem levelStem = this.dimensions.get(LevelStem.OVERWORLD);
        if (levelStem == null) {
            return DataResult.error("Overworld settings missing");
        } else {
            return this.stable() ? DataResult.success(this, Lifecycle.stable()) : DataResult.success(this);
        }
    }

    private boolean stable() {
        return LevelStem.stable(this.seed, this.dimensions);
    }

    public WorldGenSettings(long seed, boolean generateStructures, boolean bonusChest, Registry<LevelStem> options) {
        this(seed, generateStructures, bonusChest, options, Optional.empty());
        LevelStem levelStem = options.get(LevelStem.OVERWORLD);
        if (levelStem == null) {
            throw new IllegalStateException("Overworld settings missing");
        }
    }

    private WorldGenSettings(long seed, boolean generateStructures, boolean bonusChest, Registry<LevelStem> options, Optional<String> legacyCustomOptions) {
        this.seed = seed;
        this.generateFeatures = generateStructures;
        this.generateBonusChest = bonusChest;
        this.dimensions = options;
        this.legacyCustomOptions = legacyCustomOptions;
    }

    public static WorldGenSettings demoSettings(RegistryAccess registryManager) {
        int i = "North Carolina".hashCode();
        return new WorldGenSettings((long)i, true, true, withOverworld(registryManager.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), DimensionType.defaultDimensions(registryManager, (long)i), makeDefaultOverworld(registryManager, (long)i)));
    }

    public static WorldGenSettings makeDefault(RegistryAccess registryManager) {
        long l = (new Random()).nextLong();
        return new WorldGenSettings(l, true, false, withOverworld(registryManager.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), DimensionType.defaultDimensions(registryManager, l), makeDefaultOverworld(registryManager, l)));
    }

    public static NoiseBasedChunkGenerator makeDefaultOverworld(RegistryAccess registryManager, long seed) {
        return makeDefaultOverworld(registryManager, seed, true);
    }

    public static NoiseBasedChunkGenerator makeDefaultOverworld(RegistryAccess registryManager, long seed, boolean bl) {
        return makeOverworld(registryManager, seed, NoiseGeneratorSettings.OVERWORLD, bl);
    }

    public static NoiseBasedChunkGenerator makeOverworld(RegistryAccess registryManager, long seed, ResourceKey<NoiseGeneratorSettings> settings) {
        return makeOverworld(registryManager, seed, settings, true);
    }

    public static NoiseBasedChunkGenerator makeOverworld(RegistryAccess registryManager, long seed, ResourceKey<NoiseGeneratorSettings> settings, boolean bl) {
        Registry<Biome> registry = registryManager.registryOrThrow(Registry.BIOME_REGISTRY);
        Registry<StructureSet> registry2 = registryManager.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        Registry<NoiseGeneratorSettings> registry3 = registryManager.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        Registry<NormalNoise.NoiseParameters> registry4 = registryManager.registryOrThrow(Registry.NOISE_REGISTRY);
        return new NoiseBasedChunkGenerator(registry2, registry4, MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(registry, bl), seed, registry3.getOrCreateHolder(settings));
    }

    public long seed() {
        return this.seed;
    }

    public boolean generateFeatures() {
        return this.generateFeatures;
    }

    public boolean generateBonusChest() {
        return this.generateBonusChest;
    }

    public static Registry<LevelStem> withOverworld(Registry<DimensionType> dimensionTypeRegistry, Registry<LevelStem> options, ChunkGenerator overworldGenerator) {
        LevelStem levelStem = options.get(LevelStem.OVERWORLD);
        Holder<DimensionType> holder = levelStem == null ? dimensionTypeRegistry.getOrCreateHolder(DimensionType.OVERWORLD_LOCATION) : levelStem.typeHolder();
        return withOverworld(options, holder, overworldGenerator);
    }

    public static Registry<LevelStem> withOverworld(Registry<LevelStem> options, Holder<DimensionType> dimensionType, ChunkGenerator overworldGenerator) {
        WritableRegistry<LevelStem> writableRegistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);
        writableRegistry.register(LevelStem.OVERWORLD, new LevelStem(dimensionType, overworldGenerator), Lifecycle.stable());

        for(Entry<ResourceKey<LevelStem>, LevelStem> entry : options.entrySet()) {
            ResourceKey<LevelStem> resourceKey = entry.getKey();
            if (resourceKey != LevelStem.OVERWORLD) {
                writableRegistry.register(resourceKey, entry.getValue(), options.lifecycle(entry.getValue()));
            }
        }

        return writableRegistry;
    }

    public Registry<LevelStem> dimensions() {
        return this.dimensions;
    }

    public ChunkGenerator overworld() {
        LevelStem levelStem = this.dimensions.get(LevelStem.OVERWORLD);
        if (levelStem == null) {
            throw new IllegalStateException("Overworld settings missing");
        } else {
            return levelStem.generator();
        }
    }

    public ImmutableSet<ResourceKey<Level>> levels() {
        return this.dimensions().entrySet().stream().map(Entry::getKey).map(WorldGenSettings::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
    }

    public static ResourceKey<Level> levelStemToLevel(ResourceKey<LevelStem> dimensionOptionsKey) {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, dimensionOptionsKey.location());
    }

    public static ResourceKey<LevelStem> levelToLevelStem(ResourceKey<Level> worldKey) {
        return ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, worldKey.location());
    }

    public boolean isDebug() {
        return this.overworld() instanceof DebugLevelSource;
    }

    public boolean isFlatWorld() {
        return this.overworld() instanceof FlatLevelSource;
    }

    public boolean isOldCustomizedWorld() {
        return this.legacyCustomOptions.isPresent();
    }

    public WorldGenSettings withBonusChest() {
        return new WorldGenSettings(this.seed, this.generateFeatures, true, this.dimensions, this.legacyCustomOptions);
    }

    public WorldGenSettings withFeaturesToggled() {
        return new WorldGenSettings(this.seed, !this.generateFeatures, this.generateBonusChest, this.dimensions);
    }

    public WorldGenSettings withBonusChestToggled() {
        return new WorldGenSettings(this.seed, this.generateFeatures, !this.generateBonusChest, this.dimensions);
    }

    public static WorldGenSettings create(RegistryAccess registryManager, DedicatedServerProperties.WorldGenProperties worldGenProperties) {
        long l = parseSeed(worldGenProperties.levelSeed()).orElse((new Random()).nextLong());
        Registry<DimensionType> registry = registryManager.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        Registry<Biome> registry2 = registryManager.registryOrThrow(Registry.BIOME_REGISTRY);
        Registry<StructureSet> registry3 = registryManager.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        Registry<LevelStem> registry4 = DimensionType.defaultDimensions(registryManager, l);
        String var8 = worldGenProperties.levelType();
        switch(var8) {
        case "flat":
            Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, worldGenProperties.generatorSettings());
            return new WorldGenSettings(l, worldGenProperties.generateStructures(), false, withOverworld(registry, registry4, new FlatLevelSource(registry3, FlatLevelGeneratorSettings.CODEC.parse(dynamic).resultOrPartial(LOGGER::error).orElseGet(() -> {
                return FlatLevelGeneratorSettings.getDefault(registry2, registry3);
            }))));
        case "debug_all_block_states":
            return new WorldGenSettings(l, worldGenProperties.generateStructures(), false, withOverworld(registry, registry4, new DebugLevelSource(registry3, registry2)));
        case "amplified":
            return new WorldGenSettings(l, worldGenProperties.generateStructures(), false, withOverworld(registry, registry4, makeOverworld(registryManager, l, NoiseGeneratorSettings.AMPLIFIED)));
        case "largebiomes":
            return new WorldGenSettings(l, worldGenProperties.generateStructures(), false, withOverworld(registry, registry4, makeOverworld(registryManager, l, NoiseGeneratorSettings.LARGE_BIOMES)));
        default:
            return new WorldGenSettings(l, worldGenProperties.generateStructures(), false, withOverworld(registry, registry4, makeDefaultOverworld(registryManager, l)));
        }
    }

    public WorldGenSettings withSeed(boolean hardcore, OptionalLong seed) {
        long l = seed.orElse(this.seed);
        Registry<LevelStem> registry;
        if (seed.isPresent()) {
            WritableRegistry<LevelStem> writableRegistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);
            long m = seed.getAsLong();

            for(Entry<ResourceKey<LevelStem>, LevelStem> entry : this.dimensions.entrySet()) {
                ResourceKey<LevelStem> resourceKey = entry.getKey();
                writableRegistry.register(resourceKey, new LevelStem(entry.getValue().typeHolder(), entry.getValue().generator().withSeed(m)), this.dimensions.lifecycle(entry.getValue()));
            }

            registry = writableRegistry;
        } else {
            registry = this.dimensions;
        }

        WorldGenSettings worldGenSettings;
        if (this.isDebug()) {
            worldGenSettings = new WorldGenSettings(l, false, false, registry);
        } else {
            worldGenSettings = new WorldGenSettings(l, this.generateFeatures(), this.generateBonusChest() && !hardcore, registry);
        }

        return worldGenSettings;
    }

    public static OptionalLong parseSeed(String seed) {
        seed = seed.trim();
        if (StringUtils.isEmpty(seed)) {
            return OptionalLong.empty();
        } else {
            try {
                return OptionalLong.of(Long.parseLong(seed));
            } catch (NumberFormatException var2) {
                return OptionalLong.of((long)seed.hashCode());
            }
        }
    }
}
