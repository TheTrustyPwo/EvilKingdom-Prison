package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class Biome {
    public static final Codec<Biome> DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Biome.ClimateSettings.CODEC.forGetter((biome) -> {
            return biome.climateSettings;
        }), Biome.BiomeCategory.CODEC.fieldOf("category").forGetter((biome) -> {
            return biome.biomeCategory;
        }), BiomeSpecialEffects.CODEC.fieldOf("effects").forGetter((biome) -> {
            return biome.specialEffects;
        }), BiomeGenerationSettings.CODEC.forGetter((biome) -> {
            return biome.generationSettings;
        }), MobSpawnSettings.CODEC.forGetter((biome) -> {
            return biome.mobSettings;
        })).apply(instance, Biome::new);
    });
    public static final Codec<Biome> NETWORK_CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Biome.ClimateSettings.CODEC.forGetter((biome) -> {
            return biome.climateSettings;
        }), Biome.BiomeCategory.CODEC.fieldOf("category").forGetter((biome) -> {
            return biome.biomeCategory;
        }), BiomeSpecialEffects.CODEC.fieldOf("effects").forGetter((biome) -> {
            return biome.specialEffects;
        })).apply(instance, (weather, category, effects) -> {
            return new Biome(weather, category, effects, BiomeGenerationSettings.EMPTY, MobSpawnSettings.EMPTY);
        });
    });
    public static final Codec<Holder<Biome>> CODEC = RegistryFileCodec.create(Registry.BIOME_REGISTRY, DIRECT_CODEC);
    public static final Codec<HolderSet<Biome>> LIST_CODEC = RegistryCodecs.homogeneousList(Registry.BIOME_REGISTRY, DIRECT_CODEC);
    private static final PerlinSimplexNoise TEMPERATURE_NOISE = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(1234L)), ImmutableList.of(0));
    static final PerlinSimplexNoise FROZEN_TEMPERATURE_NOISE = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(3456L)), ImmutableList.of(-2, -1, 0));
    /** @deprecated */
    @Deprecated(
        forRemoval = true
    )
    public static final PerlinSimplexNoise BIOME_INFO_NOISE = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(2345L)), ImmutableList.of(0));
    private static final int TEMPERATURE_CACHE_SIZE = 1024;
    private final Biome.ClimateSettings climateSettings;
    private final BiomeGenerationSettings generationSettings;
    private final MobSpawnSettings mobSettings;
    private final Biome.BiomeCategory biomeCategory;
    private final BiomeSpecialEffects specialEffects;
    private final ThreadLocal<Long2FloatLinkedOpenHashMap> temperatureCache = ThreadLocal.withInitial(() -> {
        return Util.make(() -> {
            Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = new Long2FloatLinkedOpenHashMap(1024, 0.25F) {
                protected void rehash(int i) {
                }
            };
            long2FloatLinkedOpenHashMap.defaultReturnValue(Float.NaN);
            return long2FloatLinkedOpenHashMap;
        });
    });

    Biome(Biome.ClimateSettings weather, Biome.BiomeCategory category, BiomeSpecialEffects effects, BiomeGenerationSettings generationSettings, MobSpawnSettings spawnSettings) {
        this.climateSettings = weather;
        this.generationSettings = generationSettings;
        this.mobSettings = spawnSettings;
        this.biomeCategory = category;
        this.specialEffects = effects;
    }

    public int getSkyColor() {
        return this.specialEffects.getSkyColor();
    }

    public MobSpawnSettings getMobSettings() {
        return this.mobSettings;
    }

    public Biome.Precipitation getPrecipitation() {
        return this.climateSettings.precipitation;
    }

    public boolean isHumid() {
        return this.getDownfall() > 0.85F;
    }

    private float getHeightAdjustedTemperature(BlockPos pos) {
        float f = this.climateSettings.temperatureModifier.modifyTemperature(pos, this.getBaseTemperature());
        if (pos.getY() > 80) {
            float g = (float)(TEMPERATURE_NOISE.getValue((double)((float)pos.getX() / 8.0F), (double)((float)pos.getZ() / 8.0F), false) * 8.0D);
            return f - (g + (float)pos.getY() - 80.0F) * 0.05F / 40.0F;
        } else {
            return f;
        }
    }

    /** @deprecated */
    @Deprecated
    public float getTemperature(BlockPos blockPos) {
        long l = blockPos.asLong();
        Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = this.temperatureCache.get();
        float f = long2FloatLinkedOpenHashMap.get(l);
        if (!Float.isNaN(f)) {
            return f;
        } else {
            float g = this.getHeightAdjustedTemperature(blockPos);
            if (long2FloatLinkedOpenHashMap.size() == 1024) {
                long2FloatLinkedOpenHashMap.removeFirstFloat();
            }

            long2FloatLinkedOpenHashMap.put(l, g);
            return g;
        }
    }

    public boolean shouldFreeze(LevelReader world, BlockPos blockPos) {
        return this.shouldFreeze(world, blockPos, true);
    }

    public boolean shouldFreeze(LevelReader world, BlockPos pos, boolean doWaterCheck) {
        if (this.warmEnoughToRain(pos)) {
            return false;
        } else {
            if (pos.getY() >= world.getMinBuildHeight() && pos.getY() < world.getMaxBuildHeight() && world.getBrightness(LightLayer.BLOCK, pos) < 10) {
                BlockState blockState = world.getBlockState(pos);
                FluidState fluidState = world.getFluidState(pos);
                if (fluidState.getType() == Fluids.WATER && blockState.getBlock() instanceof LiquidBlock) {
                    if (!doWaterCheck) {
                        return true;
                    }

                    boolean bl = world.isWaterAt(pos.west()) && world.isWaterAt(pos.east()) && world.isWaterAt(pos.north()) && world.isWaterAt(pos.south());
                    if (!bl) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public boolean coldEnoughToSnow(BlockPos pos) {
        return !this.warmEnoughToRain(pos);
    }

    public boolean warmEnoughToRain(BlockPos pos) {
        return this.getTemperature(pos) >= 0.15F;
    }

    public boolean shouldMeltFrozenOceanIcebergSlightly(BlockPos pos) {
        return this.getTemperature(pos) > 0.1F;
    }

    public boolean shouldSnowGolemBurn(BlockPos pos) {
        return this.getTemperature(pos) > 1.0F;
    }

    public boolean shouldSnow(LevelReader world, BlockPos pos) {
        if (this.warmEnoughToRain(pos)) {
            return false;
        } else {
            if (pos.getY() >= world.getMinBuildHeight() && pos.getY() < world.getMaxBuildHeight() && world.getBrightness(LightLayer.BLOCK, pos) < 10) {
                BlockState blockState = world.getBlockState(pos);
                if (blockState.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(world, pos)) {
                    return true;
                }
            }

            return false;
        }
    }

    public BiomeGenerationSettings getGenerationSettings() {
        return this.generationSettings;
    }

    public int getFogColor() {
        return this.specialEffects.getFogColor();
    }

    public int getGrassColor(double x, double z) {
        int i = this.specialEffects.getGrassColorOverride().orElseGet(this::getGrassColorFromTexture);
        return this.specialEffects.getGrassColorModifier().modifyColor(x, z, i);
    }

    private int getGrassColorFromTexture() {
        double d = (double)Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
        double e = (double)Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
        return GrassColor.get(d, e);
    }

    public int getFoliageColor() {
        return this.specialEffects.getFoliageColorOverride().orElseGet(this::getFoliageColorFromTexture);
    }

    private int getFoliageColorFromTexture() {
        double d = (double)Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
        double e = (double)Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
        return FoliageColor.get(d, e);
    }

    public final float getDownfall() {
        return this.climateSettings.downfall;
    }

    public final float getBaseTemperature() {
        return this.climateSettings.temperature;
    }

    public BiomeSpecialEffects getSpecialEffects() {
        return this.specialEffects;
    }

    public final int getWaterColor() {
        return this.specialEffects.getWaterColor();
    }

    public final int getWaterFogColor() {
        return this.specialEffects.getWaterFogColor();
    }

    public Optional<AmbientParticleSettings> getAmbientParticle() {
        return this.specialEffects.getAmbientParticleSettings();
    }

    public Optional<SoundEvent> getAmbientLoop() {
        return this.specialEffects.getAmbientLoopSoundEvent();
    }

    public Optional<AmbientMoodSettings> getAmbientMood() {
        return this.specialEffects.getAmbientMoodSettings();
    }

    public Optional<AmbientAdditionsSettings> getAmbientAdditions() {
        return this.specialEffects.getAmbientAdditionsSettings();
    }

    public Optional<Music> getBackgroundMusic() {
        return this.specialEffects.getBackgroundMusic();
    }

    Biome.BiomeCategory getBiomeCategory() {
        return this.biomeCategory;
    }

    /** @deprecated */
    @Deprecated
    public static Biome.BiomeCategory getBiomeCategory(Holder<Biome> biomeEntry) {
        return biomeEntry.value().getBiomeCategory();
    }

    public static class BiomeBuilder {
        @Nullable
        private Biome.Precipitation precipitation;
        @Nullable
        private Biome.BiomeCategory biomeCategory;
        @Nullable
        private Float temperature;
        private Biome.TemperatureModifier temperatureModifier = Biome.TemperatureModifier.NONE;
        @Nullable
        private Float downfall;
        @Nullable
        private BiomeSpecialEffects specialEffects;
        @Nullable
        private MobSpawnSettings mobSpawnSettings;
        @Nullable
        private BiomeGenerationSettings generationSettings;

        public static Biome.BiomeBuilder from(Biome biome) {
            return (new Biome.BiomeBuilder()).precipitation(biome.getPrecipitation()).biomeCategory(biome.getBiomeCategory()).temperature(biome.getBaseTemperature()).downfall(biome.getDownfall()).specialEffects(biome.getSpecialEffects()).generationSettings(biome.getGenerationSettings()).mobSpawnSettings(biome.getMobSettings());
        }

        public Biome.BiomeBuilder precipitation(Biome.Precipitation precipitation) {
            this.precipitation = precipitation;
            return this;
        }

        public Biome.BiomeBuilder biomeCategory(Biome.BiomeCategory category) {
            this.biomeCategory = category;
            return this;
        }

        public Biome.BiomeBuilder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Biome.BiomeBuilder downfall(float downfall) {
            this.downfall = downfall;
            return this;
        }

        public Biome.BiomeBuilder specialEffects(BiomeSpecialEffects effects) {
            this.specialEffects = effects;
            return this;
        }

        public Biome.BiomeBuilder mobSpawnSettings(MobSpawnSettings spawnSettings) {
            this.mobSpawnSettings = spawnSettings;
            return this;
        }

        public Biome.BiomeBuilder generationSettings(BiomeGenerationSettings generationSettings) {
            this.generationSettings = generationSettings;
            return this;
        }

        public Biome.BiomeBuilder temperatureAdjustment(Biome.TemperatureModifier temperatureModifier) {
            this.temperatureModifier = temperatureModifier;
            return this;
        }

        public Biome build() {
            if (this.precipitation != null && this.biomeCategory != null && this.temperature != null && this.downfall != null && this.specialEffects != null && this.mobSpawnSettings != null && this.generationSettings != null) {
                return new Biome(new Biome.ClimateSettings(this.precipitation, this.temperature, this.temperatureModifier, this.downfall), this.biomeCategory, this.specialEffects, this.generationSettings, this.mobSpawnSettings);
            } else {
                throw new IllegalStateException("You are missing parameters to build a proper biome\n" + this);
            }
        }

        @Override
        public String toString() {
            return "BiomeBuilder{\nprecipitation=" + this.precipitation + ",\nbiomeCategory=" + this.biomeCategory + ",\ntemperature=" + this.temperature + ",\ntemperatureModifier=" + this.temperatureModifier + ",\ndownfall=" + this.downfall + ",\nspecialEffects=" + this.specialEffects + ",\nmobSpawnSettings=" + this.mobSpawnSettings + ",\ngenerationSettings=" + this.generationSettings + ",\n}";
        }
    }

    public static enum BiomeCategory implements StringRepresentable {
        NONE("none"),
        TAIGA("taiga"),
        EXTREME_HILLS("extreme_hills"),
        JUNGLE("jungle"),
        MESA("mesa"),
        PLAINS("plains"),
        SAVANNA("savanna"),
        ICY("icy"),
        THEEND("the_end"),
        BEACH("beach"),
        FOREST("forest"),
        OCEAN("ocean"),
        DESERT("desert"),
        RIVER("river"),
        SWAMP("swamp"),
        MUSHROOM("mushroom"),
        NETHER("nether"),
        UNDERGROUND("underground"),
        MOUNTAIN("mountain");

        public static final Codec<Biome.BiomeCategory> CODEC = StringRepresentable.fromEnum(Biome.BiomeCategory::values, Biome.BiomeCategory::byName);
        private static final Map<String, Biome.BiomeCategory> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(Biome.BiomeCategory::getName, (category) -> {
            return category;
        }));
        private final String name;

        private BiomeCategory(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static Biome.BiomeCategory byName(String name) {
            return BY_NAME.get(name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    static class ClimateSettings {
        public static final MapCodec<Biome.ClimateSettings> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
            return instance.group(Biome.Precipitation.CODEC.fieldOf("precipitation").forGetter((climateSettings) -> {
                return climateSettings.precipitation;
            }), Codec.FLOAT.fieldOf("temperature").forGetter((climateSettings) -> {
                return climateSettings.temperature;
            }), Biome.TemperatureModifier.CODEC.optionalFieldOf("temperature_modifier", Biome.TemperatureModifier.NONE).forGetter((climateSettings) -> {
                return climateSettings.temperatureModifier;
            }), Codec.FLOAT.fieldOf("downfall").forGetter((climateSettings) -> {
                return climateSettings.downfall;
            })).apply(instance, Biome.ClimateSettings::new);
        });
        final Biome.Precipitation precipitation;
        final float temperature;
        final Biome.TemperatureModifier temperatureModifier;
        final float downfall;

        ClimateSettings(Biome.Precipitation precipitation, float temperature, Biome.TemperatureModifier temperatureModifier, float downfall) {
            this.precipitation = precipitation;
            this.temperature = temperature;
            this.temperatureModifier = temperatureModifier;
            this.downfall = downfall;
        }
    }

    public static enum Precipitation implements StringRepresentable {
        NONE("none"),
        RAIN("rain"),
        SNOW("snow");

        public static final Codec<Biome.Precipitation> CODEC = StringRepresentable.fromEnum(Biome.Precipitation::values, Biome.Precipitation::byName);
        private static final Map<String, Biome.Precipitation> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(Biome.Precipitation::getName, (precipitation) -> {
            return precipitation;
        }));
        private final String name;

        private Precipitation(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static Biome.Precipitation byName(String name) {
            return BY_NAME.get(name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static enum TemperatureModifier implements StringRepresentable {
        NONE("none") {
            @Override
            public float modifyTemperature(BlockPos pos, float temperature) {
                return temperature;
            }
        },
        FROZEN("frozen") {
            @Override
            public float modifyTemperature(BlockPos pos, float temperature) {
                double d = Biome.FROZEN_TEMPERATURE_NOISE.getValue((double)pos.getX() * 0.05D, (double)pos.getZ() * 0.05D, false) * 7.0D;
                double e = Biome.BIOME_INFO_NOISE.getValue((double)pos.getX() * 0.2D, (double)pos.getZ() * 0.2D, false);
                double f = d + e;
                if (f < 0.3D) {
                    double g = Biome.BIOME_INFO_NOISE.getValue((double)pos.getX() * 0.09D, (double)pos.getZ() * 0.09D, false);
                    if (g < 0.8D) {
                        return 0.2F;
                    }
                }

                return temperature;
            }
        };

        private final String name;
        public static final Codec<Biome.TemperatureModifier> CODEC = StringRepresentable.fromEnum(Biome.TemperatureModifier::values, Biome.TemperatureModifier::byName);
        private static final Map<String, Biome.TemperatureModifier> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(Biome.TemperatureModifier::getName, (temperatureModifier) -> {
            return temperatureModifier;
        }));

        public abstract float modifyTemperature(BlockPos pos, float temperature);

        TemperatureModifier(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static Biome.TemperatureModifier byName(String name) {
            return BY_NAME.get(name);
        }
    }
}
