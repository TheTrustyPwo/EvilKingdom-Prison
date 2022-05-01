package net.minecraft.world.level.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class DimensionType {
    public static final int BITS_FOR_Y = BlockPos.PACKED_Y_LENGTH;
    public static final int MIN_HEIGHT = 16;
    public static final int Y_SIZE = (1 << BITS_FOR_Y) - 32;
    public static final int MAX_Y = (Y_SIZE >> 1) - 1;
    public static final int MIN_Y = MAX_Y - Y_SIZE + 1;
    public static final int WAY_ABOVE_MAX_Y = MAX_Y << 4;
    public static final int WAY_BELOW_MIN_Y = MIN_Y << 4;
    public static final ResourceLocation OVERWORLD_EFFECTS = new ResourceLocation("overworld");
    public static final ResourceLocation NETHER_EFFECTS = new ResourceLocation("the_nether");
    public static final ResourceLocation END_EFFECTS = new ResourceLocation("the_end");
    public static final Codec<DimensionType> DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.LONG.optionalFieldOf("fixed_time").xmap((optional) -> {
            return optional.map(OptionalLong::of).orElseGet(OptionalLong::empty);
        }, (optionalLong) -> {
            return optionalLong.isPresent() ? Optional.of(optionalLong.getAsLong()) : Optional.empty();
        }).forGetter((dimensionType) -> {
            return dimensionType.fixedTime;
        }), Codec.BOOL.fieldOf("has_skylight").forGetter(DimensionType::hasSkyLight), Codec.BOOL.fieldOf("has_ceiling").forGetter(DimensionType::hasCeiling), Codec.BOOL.fieldOf("ultrawarm").forGetter(DimensionType::ultraWarm), Codec.BOOL.fieldOf("natural").forGetter(DimensionType::natural), Codec.doubleRange((double)1.0E-5F, 3.0E7D).fieldOf("coordinate_scale").forGetter(DimensionType::coordinateScale), Codec.BOOL.fieldOf("piglin_safe").forGetter(DimensionType::piglinSafe), Codec.BOOL.fieldOf("bed_works").forGetter(DimensionType::bedWorks), Codec.BOOL.fieldOf("respawn_anchor_works").forGetter(DimensionType::respawnAnchorWorks), Codec.BOOL.fieldOf("has_raids").forGetter(DimensionType::hasRaids), Codec.intRange(MIN_Y, MAX_Y).fieldOf("min_y").forGetter(DimensionType::minY), Codec.intRange(16, Y_SIZE).fieldOf("height").forGetter(DimensionType::height), Codec.intRange(0, Y_SIZE).fieldOf("logical_height").forGetter(DimensionType::logicalHeight), TagKey.hashedCodec(Registry.BLOCK_REGISTRY).fieldOf("infiniburn").forGetter((dimensionType) -> {
            return dimensionType.infiniburn;
        }), ResourceLocation.CODEC.fieldOf("effects").orElse(OVERWORLD_EFFECTS).forGetter((dimensionType) -> {
            return dimensionType.effectsLocation;
        }), Codec.FLOAT.fieldOf("ambient_light").forGetter((dimensionType) -> {
            return dimensionType.ambientLight;
        })).apply(instance, DimensionType::new);
    }).comapFlatMap(DimensionType::guardY, Function.identity());
    private static final int MOON_PHASES = 8;
    public static final float[] MOON_BRIGHTNESS_PER_PHASE = new float[]{1.0F, 0.75F, 0.5F, 0.25F, 0.0F, 0.25F, 0.5F, 0.75F};
    public static final ResourceKey<DimensionType> OVERWORLD_LOCATION = ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation("overworld"));
    public static final ResourceKey<DimensionType> NETHER_LOCATION = ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation("the_nether"));
    public static final ResourceKey<DimensionType> END_LOCATION = ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation("the_end"));
    protected static final DimensionType DEFAULT_OVERWORLD = create(OptionalLong.empty(), true, false, false, true, 1.0D, false, false, true, false, true, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, OVERWORLD_EFFECTS, 0.0F);
    protected static final DimensionType DEFAULT_NETHER = create(OptionalLong.of(18000L), false, true, true, false, 8.0D, false, true, false, true, false, 0, 256, 128, BlockTags.INFINIBURN_NETHER, NETHER_EFFECTS, 0.1F);
    protected static final DimensionType DEFAULT_END = create(OptionalLong.of(6000L), false, false, false, false, 1.0D, true, false, false, false, true, 0, 256, 256, BlockTags.INFINIBURN_END, END_EFFECTS, 0.0F);
    public static final ResourceKey<DimensionType> OVERWORLD_CAVES_LOCATION = ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, new ResourceLocation("overworld_caves"));
    protected static final DimensionType DEFAULT_OVERWORLD_CAVES = create(OptionalLong.empty(), true, true, false, true, 1.0D, false, false, true, false, true, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, OVERWORLD_EFFECTS, 0.0F);
    public static final Codec<Holder<DimensionType>> CODEC = RegistryFileCodec.create(Registry.DIMENSION_TYPE_REGISTRY, DIRECT_CODEC);
    private final OptionalLong fixedTime;
    private final boolean hasSkylight;
    private final boolean hasCeiling;
    private final boolean ultraWarm;
    private final boolean natural;
    private final double coordinateScale;
    private final boolean createDragonFight;
    private final boolean piglinSafe;
    private final boolean bedWorks;
    private final boolean respawnAnchorWorks;
    private final boolean hasRaids;
    private final int minY;
    private final int height;
    private final int logicalHeight;
    private final TagKey<Block> infiniburn;
    private final ResourceLocation effectsLocation;
    private final float ambientLight;
    private final transient float[] brightnessRamp;

    private static DataResult<DimensionType> guardY(DimensionType type) {
        if (type.height() < 16) {
            return DataResult.error("height has to be at least 16");
        } else if (type.minY() + type.height() > MAX_Y + 1) {
            return DataResult.error("min_y + height cannot be higher than: " + (MAX_Y + 1));
        } else if (type.logicalHeight() > type.height()) {
            return DataResult.error("logical_height cannot be higher than height");
        } else if (type.height() % 16 != 0) {
            return DataResult.error("height has to be multiple of 16");
        } else {
            return type.minY() % 16 != 0 ? DataResult.error("min_y has to be a multiple of 16") : DataResult.success(type);
        }
    }

    private DimensionType(OptionalLong fixedTime, boolean hasSkylight, boolean hasCeiling, boolean ultrawarm, boolean natural, double coordinateScale, boolean piglinSafe, boolean bedWorks, boolean respawnAnchorWorks, boolean hasRaids, int minimumY, int height, int logicalHeight, TagKey<Block> tagKey, ResourceLocation effects, float ambientLight) {
        this(fixedTime, hasSkylight, hasCeiling, ultrawarm, natural, coordinateScale, false, piglinSafe, bedWorks, respawnAnchorWorks, hasRaids, minimumY, height, logicalHeight, tagKey, effects, ambientLight);
    }

    public static DimensionType create(OptionalLong fixedTime, boolean hasSkylight, boolean hasCeiling, boolean ultrawarm, boolean natural, double coordinateScale, boolean hasEnderDragonFight, boolean piglinSafe, boolean bedWorks, boolean respawnAnchorWorks, boolean hasRaids, int minimumY, int height, int logicalHeight, TagKey<Block> tagKey, ResourceLocation effects, float ambientLight) {
        DimensionType dimensionType = new DimensionType(fixedTime, hasSkylight, hasCeiling, ultrawarm, natural, coordinateScale, hasEnderDragonFight, piglinSafe, bedWorks, respawnAnchorWorks, hasRaids, minimumY, height, logicalHeight, tagKey, effects, ambientLight);
        guardY(dimensionType).error().ifPresent((partialResult) -> {
            throw new IllegalStateException(partialResult.message());
        });
        return dimensionType;
    }

    /** @deprecated */
    @Deprecated
    private DimensionType(OptionalLong fixedTime, boolean hasSkylight, boolean hasCeiling, boolean ultrawarm, boolean natural, double coordinateScale, boolean hasEnderDragonFight, boolean piglinSafe, boolean bedWorks, boolean respawnAnchorWorks, boolean hasRaids, int minimumY, int height, int logicalHeight, TagKey<Block> tagKey, ResourceLocation effects, float ambientLight) {
        this.fixedTime = fixedTime;
        this.hasSkylight = hasSkylight;
        this.hasCeiling = hasCeiling;
        this.ultraWarm = ultrawarm;
        this.natural = natural;
        this.coordinateScale = coordinateScale;
        this.createDragonFight = hasEnderDragonFight;
        this.piglinSafe = piglinSafe;
        this.bedWorks = bedWorks;
        this.respawnAnchorWorks = respawnAnchorWorks;
        this.hasRaids = hasRaids;
        this.minY = minimumY;
        this.height = height;
        this.logicalHeight = logicalHeight;
        this.infiniburn = tagKey;
        this.effectsLocation = effects;
        this.ambientLight = ambientLight;
        this.brightnessRamp = fillBrightnessRamp(ambientLight);
    }

    private static float[] fillBrightnessRamp(float ambientLight) {
        float[] fs = new float[16];

        for(int i = 0; i <= 15; ++i) {
            float f = (float)i / 15.0F;
            float g = f / (4.0F - 3.0F * f);
            fs[i] = Mth.lerp(ambientLight, g, 1.0F);
        }

        return fs;
    }

    /** @deprecated */
    @Deprecated
    public static DataResult<ResourceKey<Level>> parseLegacy(Dynamic<?> nbt) {
        Optional<Number> optional = nbt.asNumber().result();
        if (optional.isPresent()) {
            int i = optional.get().intValue();
            if (i == -1) {
                return DataResult.success(Level.NETHER);
            }

            if (i == 0) {
                return DataResult.success(Level.OVERWORLD);
            }

            if (i == 1) {
                return DataResult.success(Level.END);
            }
        }

        return Level.RESOURCE_KEY_CODEC.parse(nbt);
    }

    public static RegistryAccess.Writable registerBuiltin(RegistryAccess.Writable registryManager) {
        WritableRegistry<DimensionType> writableRegistry = registryManager.ownedWritableRegistryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        writableRegistry.register(OVERWORLD_LOCATION, DEFAULT_OVERWORLD, Lifecycle.stable());
        writableRegistry.register(OVERWORLD_CAVES_LOCATION, DEFAULT_OVERWORLD_CAVES, Lifecycle.stable());
        writableRegistry.register(NETHER_LOCATION, DEFAULT_NETHER, Lifecycle.stable());
        writableRegistry.register(END_LOCATION, DEFAULT_END, Lifecycle.stable());
        return registryManager;
    }

    public static Registry<LevelStem> defaultDimensions(RegistryAccess registryManager, long seed) {
        return defaultDimensions(registryManager, seed, true);
    }

    public static Registry<LevelStem> defaultDimensions(RegistryAccess registryManager, long seed, boolean bl) {
        WritableRegistry<LevelStem> writableRegistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);
        Registry<DimensionType> registry = registryManager.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        Registry<Biome> registry2 = registryManager.registryOrThrow(Registry.BIOME_REGISTRY);
        Registry<StructureSet> registry3 = registryManager.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        Registry<NoiseGeneratorSettings> registry4 = registryManager.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
        Registry<NormalNoise.NoiseParameters> registry5 = registryManager.registryOrThrow(Registry.NOISE_REGISTRY);
        writableRegistry.register(LevelStem.NETHER, new LevelStem(registry.getOrCreateHolder(NETHER_LOCATION), new NoiseBasedChunkGenerator(registry3, registry5, MultiNoiseBiomeSource.Preset.NETHER.biomeSource(registry2, bl), seed, registry4.getOrCreateHolder(NoiseGeneratorSettings.NETHER))), Lifecycle.stable());
        writableRegistry.register(LevelStem.END, new LevelStem(registry.getOrCreateHolder(END_LOCATION), new NoiseBasedChunkGenerator(registry3, registry5, new TheEndBiomeSource(registry2, seed), seed, registry4.getOrCreateHolder(NoiseGeneratorSettings.END))), Lifecycle.stable());
        return writableRegistry;
    }

    public static double getTeleportationScale(DimensionType fromDimension, DimensionType toDimension) {
        double d = fromDimension.coordinateScale();
        double e = toDimension.coordinateScale();
        return d / e;
    }

    public static Path getStorageFolder(ResourceKey<Level> worldRef, Path worldDirectory) {
        if (worldRef == Level.OVERWORLD) {
            return worldDirectory;
        } else if (worldRef == Level.END) {
            return worldDirectory.resolve("DIM1");
        } else {
            return worldRef == Level.NETHER ? worldDirectory.resolve("DIM-1") : worldDirectory.resolve("dimensions").resolve(worldRef.location().getNamespace()).resolve(worldRef.location().getPath());
        }
    }

    public boolean hasSkyLight() {
        return this.hasSkylight;
    }

    public boolean hasCeiling() {
        return this.hasCeiling;
    }

    public boolean ultraWarm() {
        return this.ultraWarm;
    }

    public boolean natural() {
        return this.natural;
    }

    public double coordinateScale() {
        return this.coordinateScale;
    }

    public boolean piglinSafe() {
        return this.piglinSafe;
    }

    public boolean bedWorks() {
        return this.bedWorks;
    }

    public boolean respawnAnchorWorks() {
        return this.respawnAnchorWorks;
    }

    public boolean hasRaids() {
        return this.hasRaids;
    }

    public int minY() {
        return this.minY;
    }

    public int height() {
        return this.height;
    }

    public int logicalHeight() {
        return this.logicalHeight;
    }

    public boolean createDragonFight() {
        return this.createDragonFight;
    }

    public boolean hasFixedTime() {
        return this.fixedTime.isPresent();
    }

    public float timeOfDay(long time) {
        double d = Mth.frac((double)this.fixedTime.orElse(time) / 24000.0D - 0.25D);
        double e = 0.5D - Math.cos(d * Math.PI) / 2.0D;
        return (float)(d * 2.0D + e) / 3.0F;
    }

    public int moonPhase(long time) {
        return (int)(time / 24000L % 8L + 8L) % 8;
    }

    public float brightness(int lightLevel) {
        return this.brightnessRamp[lightLevel];
    }

    public TagKey<Block> infiniburn() {
        return this.infiniburn;
    }

    public ResourceLocation effectsLocation() {
        return this.effectsLocation;
    }
}
