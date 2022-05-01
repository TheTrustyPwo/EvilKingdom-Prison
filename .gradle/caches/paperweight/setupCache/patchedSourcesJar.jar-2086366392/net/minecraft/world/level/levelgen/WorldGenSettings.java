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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryOps;
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

    public static final Codec<WorldGenSettings> CODEC = RecordCodecBuilder.<WorldGenSettings>create((instance) -> { // CraftBukkit - decompile error
        return instance.group(Codec.LONG.fieldOf("seed").stable().forGetter(WorldGenSettings::seed), Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(WorldGenSettings::generateFeatures), Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(WorldGenSettings::generateBonusChest), RegistryCodecs.dataPackAwareCodec(Registry.LEVEL_STEM_REGISTRY, Lifecycle.stable(), LevelStem.CODEC).xmap(LevelStem::sortMap, Function.identity()).fieldOf("dimensions").forGetter(WorldGenSettings::dimensions), Codec.STRING.optionalFieldOf("legacy_custom_options").stable().forGetter((generatorsettings) -> {
            return generatorsettings.legacyCustomOptions;
        })).apply(instance, instance.stable(WorldGenSettings::new));
    }).comapFlatMap(WorldGenSettings::guardExperimental, Function.identity());
    private static final Logger LOGGER = LogUtils.getLogger();
    private final long seed;
    private final boolean generateFeatures;
    private final boolean generateBonusChest;
    private final Registry<LevelStem> dimensions;
    private final Optional<String> legacyCustomOptions;

    private DataResult<WorldGenSettings> guardExperimental() {
        LevelStem worlddimension = (LevelStem) this.dimensions.get(LevelStem.OVERWORLD);

        return worlddimension == null ? DataResult.error("Overworld settings missing") : (this.stable() ? DataResult.success(this, Lifecycle.stable()) : DataResult.success(this));
    }

    private boolean stable() {
        return LevelStem.stable(this.seed, this.dimensions);
    }

    public WorldGenSettings(long seed, boolean generateStructures, boolean bonusChest, Registry<LevelStem> options) {
        this(seed, generateStructures, bonusChest, options, Optional.empty());
        LevelStem worlddimension = (LevelStem) options.get(LevelStem.OVERWORLD);

        if (worlddimension == null) {
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

        return new WorldGenSettings((long) i, true, true, WorldGenSettings.withOverworld(registryManager.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), DimensionType.defaultDimensions(registryManager, (long) i), WorldGenSettings.makeDefaultOverworld(registryManager, (long) i)));
    }

    public static WorldGenSettings makeDefault(RegistryAccess registryManager) {
        long i = (new Random()).nextLong();

        return new WorldGenSettings(i, true, false, WorldGenSettings.withOverworld(registryManager.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), DimensionType.defaultDimensions(registryManager, i), WorldGenSettings.makeDefaultOverworld(registryManager, i)));
    }

    public static NoiseBasedChunkGenerator makeDefaultOverworld(RegistryAccess registryManager, long seed) {
        return WorldGenSettings.makeDefaultOverworld(registryManager, seed, true);
    }

    public static NoiseBasedChunkGenerator makeDefaultOverworld(RegistryAccess registryManager, long seed, boolean flag) {
        return WorldGenSettings.makeOverworld(registryManager, seed, NoiseGeneratorSettings.OVERWORLD, flag);
    }

    public static NoiseBasedChunkGenerator makeOverworld(RegistryAccess registryManager, long seed, ResourceKey<NoiseGeneratorSettings> settings) {
        return WorldGenSettings.makeOverworld(registryManager, seed, settings, true);
    }

    public static NoiseBasedChunkGenerator makeOverworld(RegistryAccess registryManager, long seed, ResourceKey<NoiseGeneratorSettings> settings, boolean flag) {
        Registry<Biome> iregistry = registryManager.registryOrThrow(Registry.BIOME_REGISTRY);
        Registry<StructureSet> iregistry1 = registryManager.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        Registry<NoiseGeneratorSettings> iregistry2 = registryManager.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        Registry<NormalNoise.NoiseParameters> iregistry3 = registryManager.registryOrThrow(Registry.NOISE_REGISTRY);

        return new NoiseBasedChunkGenerator(iregistry1, iregistry3, MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(iregistry, flag), seed, iregistry2.getOrCreateHolder(settings));
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
        LevelStem worlddimension = (LevelStem) options.get(LevelStem.OVERWORLD);
        Holder<DimensionType> holder = worlddimension == null ? dimensionTypeRegistry.getOrCreateHolder(DimensionType.OVERWORLD_LOCATION) : worlddimension.typeHolder();

        return WorldGenSettings.withOverworld(options, holder, overworldGenerator);
    }

    public static Registry<LevelStem> withOverworld(Registry<LevelStem> options, Holder<DimensionType> dimensionType, ChunkGenerator overworldGenerator) {
        WritableRegistry<LevelStem> iregistrywritable = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function) null);

        iregistrywritable.register(LevelStem.OVERWORLD, new LevelStem(dimensionType, overworldGenerator), Lifecycle.stable()); // CraftBukkit - decompile error
        Iterator iterator = options.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<ResourceKey<LevelStem>, LevelStem> entry = (Entry) iterator.next();
            ResourceKey<LevelStem> resourcekey = (ResourceKey) entry.getKey();

            if (resourcekey != LevelStem.OVERWORLD) {
                iregistrywritable.register(resourcekey, entry.getValue(), options.lifecycle(entry.getValue())); // CraftBukkit - decompile error
            }
        }

        return iregistrywritable;
    }

    public Registry<LevelStem> dimensions() {
        return this.dimensions;
    }

    public ChunkGenerator overworld() {
        LevelStem worlddimension = (LevelStem) this.dimensions.get(LevelStem.OVERWORLD);

        if (worlddimension == null) {
            throw new IllegalStateException("Overworld settings missing");
        } else {
            return worlddimension.generator();
        }
    }

    public ImmutableSet<ResourceKey<Level>> levels() {
        return (ImmutableSet) this.dimensions().entrySet().stream().map(Entry::getKey).map(WorldGenSettings::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
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
        long i = WorldGenSettings.parseSeed(worldGenProperties.levelSeed()).orElse((new Random()).nextLong());
        Registry<DimensionType> iregistry = registryManager.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        Registry<Biome> iregistry1 = registryManager.registryOrThrow(Registry.BIOME_REGISTRY);
        Registry<StructureSet> iregistry2 = registryManager.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        Registry<LevelStem> iregistry3 = DimensionType.defaultDimensions(registryManager, i);
        String s = worldGenProperties.levelType();
        byte b0 = -1;

        switch (s.hashCode()) {
            case -1100099890:
                if (s.equals("largebiomes")) {
                    b0 = 3;
                }
                break;
            case 3145593:
                if (s.equals("flat")) {
                    b0 = 0;
                }
                break;
            case 1045526590:
                if (s.equals("debug_all_block_states")) {
                    b0 = 1;
                }
                break;
            case 1271599715:
                if (s.equals("amplified")) {
                    b0 = 2;
                }
        }

        switch (b0) {
            case 0:
                Dynamic<JsonElement> dynamic = new Dynamic(RegistryOps.create(JsonOps.INSTANCE, registryManager), worldGenProperties.generatorSettings()); // CraftBukkit - Incorrect Ops
                boolean flag = worldGenProperties.generateStructures();
                DataResult dataresult = FlatLevelGeneratorSettings.CODEC.parse(dynamic);
                Logger logger = WorldGenSettings.LOGGER;

                Objects.requireNonNull(logger);
                return new WorldGenSettings(i, flag, false, WorldGenSettings.withOverworld(iregistry, iregistry3, new FlatLevelSource(iregistry2, (FlatLevelGeneratorSettings) dataresult.resultOrPartial(s1 -> logger.error(String.valueOf(s1))).orElseGet(() -> { // CraftBukkit - decompile error
                    return FlatLevelGeneratorSettings.getDefault(iregistry1, iregistry2);
                }))));
            case 1:
                return new WorldGenSettings(i, worldGenProperties.generateStructures(), false, WorldGenSettings.withOverworld(iregistry, iregistry3, new DebugLevelSource(iregistry2, iregistry1)));
            case 2:
                return new WorldGenSettings(i, worldGenProperties.generateStructures(), false, WorldGenSettings.withOverworld(iregistry, iregistry3, WorldGenSettings.makeOverworld(registryManager, i, NoiseGeneratorSettings.AMPLIFIED)));
            case 3:
                return new WorldGenSettings(i, worldGenProperties.generateStructures(), false, WorldGenSettings.withOverworld(iregistry, iregistry3, WorldGenSettings.makeOverworld(registryManager, i, NoiseGeneratorSettings.LARGE_BIOMES)));
            default:
                return new WorldGenSettings(i, worldGenProperties.generateStructures(), false, WorldGenSettings.withOverworld(iregistry, iregistry3, WorldGenSettings.makeDefaultOverworld(registryManager, i)));
        }
    }

    public WorldGenSettings withSeed(boolean hardcore, OptionalLong seed) {
        long i = seed.orElse(this.seed);
        Object object;

        if (seed.isPresent()) {
            WritableRegistry<LevelStem> iregistrywritable = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function) null);
            long j = seed.getAsLong();
            Iterator iterator = this.dimensions.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<ResourceKey<LevelStem>, LevelStem> entry = (Entry) iterator.next();
                ResourceKey<LevelStem> resourcekey = (ResourceKey) entry.getKey();

                iregistrywritable.register(resourcekey, new LevelStem(entry.getValue().typeHolder(), entry.getValue().generator().withSeed(j)), this.dimensions.lifecycle(entry.getValue())); // CraftBukkit - decompile error
            }

            object = iregistrywritable;
        } else {
            object = this.dimensions;
        }

        WorldGenSettings generatorsettings;

        if (this.isDebug()) {
            generatorsettings = new WorldGenSettings(i, false, false, (Registry) object);
        } else {
            generatorsettings = new WorldGenSettings(i, this.generateFeatures(), this.generateBonusChest() && !hardcore, (Registry) object);
        }

        return generatorsettings;
    }

    public static OptionalLong parseSeed(String seed) {
        seed = seed.trim();
        if (StringUtils.isEmpty(seed)) {
            return OptionalLong.empty();
        } else {
            try {
                return OptionalLong.of(Long.parseLong(seed));
            } catch (NumberFormatException numberformatexception) {
                return OptionalLong.of((long) seed.hashCode());
            }
        }
    }
}
